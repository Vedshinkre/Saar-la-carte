package casualtablemergingtests

import casualtablemergingtests.CasualTableMergingFixtures.casualGroup
import casualtablemergingtests.CasualTableMergingFixtures.menu
import casualtablemergingtests.CasualTableMergingFixtures.processor
import casualtablemergingtests.CasualTableMergingFixtures.starving
import casualtablemergingtests.CasualTableMergingFixtures.table
import casualtablemergingtests.CasualTableMergingFixtures.waiter
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Table
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** unit tests for F15 to merge tables for unreserved casuals */
class CasualUnreservedMergeTest {

    private lateinit var output: StringWriter
    private val customerToTable: MutableMap<CustomerGroup, List<Table>> = mutableMapOf()
    private val turnedAway: MutableList<CustomerGroup> = mutableListOf()

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
    }

    private fun arrive(
        tables: List<Table>,
        group: CustomerGroup,
        waiters: List<Waiter> = listOf(waiter())
    ): Boolean = processor(tables, waiters, customerToTable, turnedAway).processArrival(group, menu)

    // ---- steps 2-3: single table ----

    @Test
    fun `a perfect single-table fit occupies exactly that table`() {
        val exact = table(1, 4)

        arrive(listOf(exact), casualGroup(id = 1, size = 4))

        assertEquals(TableStatus.OCCUPIED, exact.status)
    }

    @Test
    fun `a single table that only satisfies the three-quarters rule is used`() {
        val relaxedFitTable = table(1, 4)

        arrive(listOf(relaxedFitTable), casualGroup(id = 1, size = 3))

        assertEquals(TableStatus.OCCUPIED, relaxedFitTable.status)
    }

    @Test
    fun `a table that fits but fails the three-quarters rule is unreachable for CASUAL (no relaxation)`() {
        // table 8 / group 5: fits (5 <= 8) but fails the 3/4 rule (8*0.75=6 > 5), so it falls
        // through step 2/3 to merging - and since this is the only free table, merging with
        // itself fails the same 3/4 check. A REGULAR/EVENT reservation would rescue this via
        // step 5 (relaxed single table); assignTables has no such step, so it simply fails
        val onlyTable = table(1, 8)
        val group = casualGroup(id = 1, size = 5)

        val removeFromQueue = arrive(listOf(onlyTable), group)

        assertTrue(removeFromQueue)
        assertEquals(TableStatus.FREE, onlyTable.status)
        assertTrue(turnedAway.contains(group))
        assertFalse(customerToTable.containsKey(group))
    }

    // ---- Step 4: merging ----

    @Test
    fun `merging trims the smallest tables down to the minimal set that still satisfies three-quarters`() {
        val small = table(1, 2)
        val medium = table(2, 3)
        val large = table(3, 4)

        arrive(listOf(small, medium, large), casualGroup(id = 1, size = 6))

        assertEquals(TableStatus.FREE, small.status, "the smallest table should have been trimmed off")
        assertEquals(TableStatus.OCCUPIED, medium.status)
        assertEquals(TableStatus.OCCUPIED, large.status)
    }

    @Test
    fun `a trim tie between two same-size tables drops the lower id first`() {
        val lowerId = table(1, 2)
        val higherId = table(2, 2)
        val large = table(3, 4)

        arrive(listOf(lowerId, higherId, large), casualGroup(id = 1, size = 5))

        assertEquals(TableStatus.FREE, lowerId.status, "the lower-id table of the tied pair must be dropped")
        assertEquals(TableStatus.OCCUPIED, higherId.status)
        assertEquals(TableStatus.OCCUPIED, large.status)
    }

    @Test
    fun `merging succeeds exactly at the three-quarters boundary after trimming as far as possible`() {
        // three size-2 tables for a group of 3: merged size 4, 4*0.75=3 <= 3 succeeds, and the
        // third table can be trimmed off without dropping below the group size
        val first = table(1, 2)
        val second = table(2, 2)
        val third = table(3, 2)

        arrive(listOf(first, second, third), casualGroup(id = 1, size = 3))

        assertEquals(TableStatus.OCCUPIED, first.status)
        assertEquals(TableStatus.OCCUPIED, second.status)
        assertEquals(TableStatus.FREE, third.status, "the untrimmable-but-unnecessary third table stays free")
    }

    @Test
    fun `merging fails the three-quarters rule even after trimming as far as possible`() {
        // three size-3 tables for a group of 4: merged size 6, but no table can be trimmed off
        // without dropping the sum below 4, and 6*0.75=4.5 > 4 fails the 3/4 rule regardless
        val tables = listOf(table(1, 3), table(2, 3), table(3, 3))

        val removeFromQueue = arrive(tables, casualGroup(id = 1, size = 4))

        assertTrue(removeFromQueue)
        assertTrue(tables.all { it.status == TableStatus.FREE })
    }

    @Test
    fun `insufficient total free capacity across all free tables returns no merge candidate`() {
        val tables = listOf(table(1, 2), table(2, 2))

        val removeFromQueue = arrive(tables, casualGroup(id = 1, size = 10))

        assertTrue(removeFromQueue)
        assertTrue(tables.all { it.status == TableStatus.FREE })
    }

    // ---- filtering ----

    @Test
    fun `BAR tables are never merged for a CASUAL group`() {
        val tables = listOf(table(1, 2, TableType.BAR), table(2, 2, TableType.BAR))

        val removeFromQueue = arrive(tables, casualGroup(id = 1, size = 4, tableType = TableType.BAR))

        assertTrue(removeFromQueue)
        assertTrue(tables.all { it.status == TableStatus.FREE })
    }

    @Test
    fun `a same-size free table of the wrong type is never a candidate`() {
        val wrongType = table(1, 4, TableType.BAR)

        val removeFromQueue = arrive(listOf(wrongType), casualGroup(id = 1, size = 4, tableType = TableType.COMMON))

        assertTrue(removeFromQueue)
        assertEquals(TableStatus.FREE, wrongType.status)
    }

    @Test
    fun `a right-sized table that is not FREE is never a candidate`() {
        val alreadyReserved = table(1, 4).also { it.status = TableStatus.RESERVED }
        val group = casualGroup(id = 1, size = 4)

        val removeFromQueue = arrive(listOf(alreadyReserved), group)

        assertTrue(removeFromQueue)
        assertEquals(TableStatus.RESERVED, alreadyReserved.status)
        assertFalse(customerToTable.containsKey(group))
    }

    // ---- outcome ----

    @Test
    fun `a successful ad-hoc assignment sets OCCUPIED, never RESERVED`() {
        val exact = table(1, 4)
        val group = casualGroup(id = 1, size = 4)

        arrive(listOf(exact), group)

        assertEquals(TableStatus.OCCUPIED, exact.status)
        assertEquals(listOf(exact), customerToTable[group])
    }

    @Test
    fun `an order that fails afterward frees the ad-hoc table again and turns the group away`() {
        val exact = table(1, 4)
        val group = casualGroup(id = 1, size = 4, preferences = List(4) { starving() })

        val removeFromQueue = arrive(listOf(exact), group)

        assertTrue(removeFromQueue)
        assertEquals(TableStatus.FREE, exact.status)
        assertFalse(customerToTable.containsKey(group))
        assertTrue(turnedAway.contains(group))
        assertEquals(ExperienceType.NEGATIVE, group.experience)
    }

    @Test
    fun `a successful ad-hoc merge logs FOH Merging Tables with the sorted old ids and the lowest merged id`() {
        val small = table(2, 2)
        val large = table(1, 4)

        arrive(listOf(small, large), casualGroup(id = 7, size = 6))

        val log = output.toString()
        assertTrue(log.contains("FOH Merging Tables (R 1): For group 7 the tables 1,2 were merged into 1."), log)
    }

    @Test
    fun `fed() customers actually order, proving OCCUPIED status is not just an artefact of a failed order`() {
        val exact = table(1, 2)
        val group = casualGroup(id = 1, size = 2)

        arrive(listOf(exact), group)

        assertTrue(group.customersRemainingInRestaurant > 0)
    }

    // ---- F15/F16 boundary: failing before or because of assignTables ----

    @Test
    fun `no free waiter on the visiting tick keeps the group queued and leaves the table untouched`() {
        val fitting = table(1, 4)
        val group = casualGroup(id = 1, size = 4, visitingAt = 1)
        Time.tick = 1

        val removeFromQueue = arrive(listOf(fitting), group, emptyList())

        assertFalse(removeFromQueue)
        assertEquals(TableStatus.FREE, fitting.status)
        assertFalse(customerToTable.containsKey(group))
        assertEquals(ExperienceType.NEUTRAL, group.experience)
    }

    @Test
    fun `no free waiter after the visiting tick turns the CASUAL group away without touching the table`() {
        val fitting = table(1, 4)
        val group = casualGroup(id = 1, size = 4, visitingAt = 1)
        Time.tick = 2 // the group's one visiting tick has already passed

        val removeFromQueue = arrive(listOf(fitting), group, emptyList())

        assertTrue(removeFromQueue)
        assertEquals(TableStatus.FREE, fitting.status)
        assertTrue(turnedAway.contains(group))
        assertEquals(ExperienceType.NEGATIVE, group.experience)
    }

    @Test
    fun `logFohNoSeatingNoWaitstaff fires with the group's id when no waiter is free`() {
        val group = casualGroup(id = 9, size = 4, visitingAt = 1)

        arrive(emptyList(), group, emptyList())

        assertTrue(output.toString().contains("FOH No Seating (R 1): No free waitstaff available for group 9."))
    }

    @Test
    fun `a free waiter with no table candidate turns the group away and never mutates a table`() {
        val tooSmall = table(1, 2)
        val freeWaiter = waiter()
        val group = casualGroup(id = 3, size = 4)

        val removeFromQueue = arrive(listOf(tooSmall), group, listOf(freeWaiter))

        assertTrue(removeFromQueue)
        assertEquals(TableStatus.FREE, tooSmall.status)
        assertTrue(turnedAway.contains(group))
        assertEquals(ExperienceType.NEGATIVE, group.experience)
        // seating (and its SEAT tick load) never happens since assignTables rejected the group
        assertEquals(0, freeWaiter.getTickLoad(ActionType.SEAT))
    }

    @Test
    fun `logFohNoSeating fires with the assigned waiter's id when assignTables finds no candidate`() {
        val tooSmall = table(1, 2)
        val group = casualGroup(id = 3, size = 4)

        arrive(listOf(tooSmall), group, listOf(waiter()))

        val log = output.toString()
        assertTrue(
            log.contains("FOH No Seating (R 1): Assigned waitstaff 1 but no table available, group 3 is sent away."),
            log
        )
    }
}
