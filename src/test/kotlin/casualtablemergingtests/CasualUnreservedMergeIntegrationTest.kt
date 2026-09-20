package casualtablemergingtests

import casualtablemergingtests.CasualTableMergingFixtures.casualGroup
import casualtablemergingtests.CasualTableMergingFixtures.frontOfHouse
import casualtablemergingtests.CasualTableMergingFixtures.menu
import casualtablemergingtests.CasualTableMergingFixtures.regularGroup
import casualtablemergingtests.CasualTableMergingFixtures.table
import casualtablemergingtests.CasualTableMergingFixtures.waiter
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.loggers.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** integration tests for merging tables for casual customer arrival during the serving phase */
class CasualUnreservedMergeIntegrationTest {

    private lateinit var output: StringWriter
    private val orderQueue: ArrayDeque<Order> = ArrayDeque()

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
    }

    private fun lines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    @Test
    fun `an ad-hoc merge is seated within the same tick, with FOH Merging Tables logged before FOH Seating`() {
        val small = table(1, 2)
        val large = table(2, 4)
        val foh = frontOfHouse(listOf(small, large), listOf(waiter()), orderQueue)
        val group = casualGroup(id = 1, size = 6)

        foh.processArrival(group, menu)

        val mergingIndex = lines().indexOfFirst { it.contains("FOH Merging Tables") }
        val seatingIndex = lines().indexOfFirst { it.contains("FOH Seating") }
        assertTrue(mergingIndex in 0..<seatingIndex, "expected merging logged before seating, got ${lines()}")
        assertTrue(lines()[mergingIndex].contains("the tables 1,2 were merged into 1."), lines()[mergingIndex])
        assertEquals(TableStatus.OCCUPIED, small.status)
        assertEquals(TableStatus.OCCUPIED, large.status)
    }

    @Test
    fun `a second CASUAL group in the same tick cannot select a table the first just occupied`() {
        val first = table(1, 4)
        val second = table(2, 4)
        val foh = frontOfHouse(listOf(first, second), listOf(waiter(), waiter()), orderQueue)

        foh.processArrival(casualGroup(id = 1, size = 4), menu)
        foh.processArrival(casualGroup(id = 2, size = 4), menu)

        assertEquals(TableStatus.OCCUPIED, first.status)
        assertEquals(TableStatus.OCCUPIED, second.status)
        assertTrue(lines().any { it.contains("FOH Seating (R 1): Group 1 seated at table 1") }, lines().toString())
        assertTrue(lines().any { it.contains("FOH Seating (R 1): Group 2 seated at table 2") }, lines().toString())
    }

    @Test
    fun `a CASUAL group arriving after prep-phase reservations cannot select the reserved table`() {
        val reservedForRegular = table(1, 4)
        val freeForCasual = table(2, 4)
        val foh = frontOfHouse(listOf(reservedForRegular, freeForCasual), listOf(waiter()), orderQueue)
        val reserved = foh.reserveTables(regularGroup(id = 1, size = 4))

        foh.processArrival(casualGroup(id = 2, size = 4), menu)

        assertTrue(reserved)
        assertEquals(TableStatus.RESERVED, reservedForRegular.status, "the reservation must survive the ad-hoc pass")
        assertEquals(TableStatus.OCCUPIED, freeForCasual.status)
        assertTrue(
            lines().any { it.contains("FOH Seating (R 1): Group 2 seated at table 2") },
            lines().toString()
        )
    }
}
