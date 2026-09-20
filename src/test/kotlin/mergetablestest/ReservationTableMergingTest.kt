package mergetablestest

import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import kotlin.collections.emptyList

class ReservationTableMergingTest {

    // helper methods

    @BeforeEach
    fun setupLogger() {
        // Initialize a dummy logger to suppress the error
        Logger.setup(PrintWriter(ByteArrayOutputStream()))
        Logger.setup(LogLevel.DEBUG)
    }
    private fun createDummyCountertop(): Countertop {
        val dummyStock = Stock(emptyList())
        val dummyPantry = Pantry(dummyStock)
        return Countertop(
            pantry = dummyPantry,
            orderQueue = ArrayDeque(),
            cooks = emptyList(),
            restaurantType = RestaurantType.EUROPEAN
        )
    }

    private fun createDummyRegularGroup(groupSize: Int, tableType: TableType): RegularGroup {
        return RegularGroup(
            id = 3,
            size = groupSize,
            tableType = tableType,
            visitingAt = 1,
            foodPreferences = emptyList(),
            visitingStart = 1,
            visitingPeriod = 5,
            restaurantId = 1
        )
    }

    private fun createDummyEventGroup(groupSize: Int, tableType: TableType): EventGroup {
        return EventGroup(
            id = 2,
            size = groupSize,
            tableType = tableType,
            visitingAt = 1,
            foodPreferences = emptyList(),
            restaurantTypes = emptyList(),
            eventEvening = 1,
            eventDishes = emptyMap()
        )
    }
    private fun createDummyCasualGroup(groupSize: Int, tableType: TableType): CasualGroup {
        return CasualGroup(
            id = 1,
            size = groupSize,
            tableType = tableType,
            visitingAt = 1,
            foodPreferences = emptyList(),
            restaurantTypes = emptyList(),
            visitingEvenings = emptyList(),
            deliveryDistance = 0,
            ratingLikelihood = RatingLikelihood.NEVER
        )
    }

    private fun setupFoh(tables: List<Table>): FrontOfHouse {
        return FrontOfHouse(
            tables = tables,
            waiters = emptyList(),
            drivers = emptyList(),
            countertop = createDummyCountertop()
        )
    }

    // exception cases, nop casuals will get reserved tables

    @Test
    fun `reserveTables - CasualGroup Common throws exception`() {
        // casual groups cannot have tables reserved for them
        val foh = setupFoh(emptyList())
        val casualGroup = createDummyCasualGroup(5, TableType.COMMON)

        assertThrows<IllegalArgumentException> {
            foh.reserveTables(casualGroup)
        }
    }

    @Test
    fun `reserveTables - CasualGroup Bar throws exception`() {
        // casual groups cannot have tables reserved for them
        val foh = setupFoh(emptyList())
        val casualGroup = createDummyCasualGroup(5, TableType.BAR)

        assertThrows<IllegalArgumentException> {
            foh.reserveTables(casualGroup)
        }
    }

    @Test
    fun `reserveTables - CasualGroup Seperated throws exception`() {
        // casual groups cannot have tables reserved for them
        val foh = setupFoh(emptyList())
        val casualGroup = createDummyCasualGroup(5, TableType.SEPARATED)

        assertThrows<IllegalArgumentException> {
            foh.reserveTables(casualGroup)
        }
    }

    @Test
    fun `reserveTables - Casual Group With Available COMMON Tables fails`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1))
        // even if the table type matches , we still do not reserve for casuals
        val casualGroup = createDummyCasualGroup(groupSize = 4, TableType.COMMON)

        assertThrows<IllegalArgumentException> {
            foh.reserveTables(casualGroup)
        }
    }

    @Test
    fun `reserveTables - Casual Group With Available BAR Tables fails`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1))
        // even if the table type matches , we still do not reserve for casuals
        val casualGroup = createDummyCasualGroup(groupSize = 4, TableType.BAR)

        assertThrows<IllegalArgumentException> {
            foh.reserveTables(casualGroup)
        }
    }

    @Test
    fun `reserveTables - Casual Group With Available SEPERATED Tables fails`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1))
        // even if the table type matches , we still do not reserve for casuals
        val casualGroup = createDummyCasualGroup(groupSize = 4, TableType.SEPARATED)

        assertThrows<IllegalArgumentException> {
            foh.reserveTables(casualGroup)
        }
    }

    //  CORE MERGING

    @Test
    fun `reserveTables-Event Groups-Common- exact capacity match succeeds`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyEventGroup(groupSize = 4, TableType.COMMON)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables-Regular Groups-Common- exact capacity match succeeds`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyRegularGroup(groupSize = 4, TableType.COMMON)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables-Event Groups-BAR- exact capacity match succeeds`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyEventGroup(groupSize = 4, TableType.BAR)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables-Regular Groups-BAR- exact capacity match succeeds`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyRegularGroup(groupSize = 4, TableType.BAR)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables-Event Groups-SEPERATED- exact capacity match succeeds`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyEventGroup(groupSize = 4, TableType.SEPARATED)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables-Regular Groups-SEPERATED- exact capacity match succeeds`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyRegularGroup(groupSize = 4, TableType.SEPARATED)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

// ------------------------------------------------------------------------------------
    @Test
    fun `reserveTables single table three quarters rule succeeds- Event-COMMON`() {
        // Table size 4. 3/4 of 4 is 3. Group size 3.
        val table1 = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyEventGroup(groupSize = 3, tableType = TableType.COMMON)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables single table three quarters rule succeeds- Regular-COMMON`() {
        // Table size 4. 3/4 of 4 is 3. Group size 3.
        val table1 = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyRegularGroup(groupSize = 3, tableType = TableType.COMMON)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables single table three quarters rule succeeds- Event-SEPERATED`() {
        // Table size 4. 3/4 of 4 is 3. Group size 3.
        val table1 = Table(id = 1, size = 4, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyEventGroup(groupSize = 3, tableType = TableType.SEPARATED)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables single table three quarters rule succeeds- Regular-SEPERATED`() {
        // Table size 4. 3/4 of 4 is 3. Group size 3.
        val table1 = Table(id = 1, size = 4, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyRegularGroup(groupSize = 3, tableType = TableType.SEPARATED)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables single table three quarters rule succeeds- Event-BAR`() {
        // Table size 4. 3/4 of 4 is 3. Group size 3.
        val table1 = Table(id = 1, size = 4, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyEventGroup(groupSize = 3, tableType = TableType.BAR)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables single table three quarters rule succeeds- Regular-BAR`() {
        // Table size 4. 3/4 of 4 is 3. Group size 3.
        val table1 = Table(id = 1, size = 4, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyRegularGroup(groupSize = 3, tableType = TableType.BAR)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    // ------------------------------------------------------------------------------------
    @Test
    fun `reserveTables merge exact capacity match succeeds- EVENT-COMMON`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyEventGroup(groupSize = 4, tableType = TableType.COMMON)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }

    @Test
    fun `reserveTables merge exact capacity match succeeds- Regular-COMMON`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyRegularGroup(groupSize = 4, tableType = TableType.COMMON)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }
    fun `reserveTables merge exact capacity match succeeds- EVENT-SEPERATED`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 2, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyEventGroup(groupSize = 4, tableType = TableType.SEPARATED)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }

    @Test
    fun `reserveTables merge exact capacity match succeeds- Regular-SEPERATED`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 2, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyRegularGroup(groupSize = 4, tableType = TableType.SEPARATED)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }

    @Test
    fun `reserveTables merge exact capacity match fails - EVENT-BAR`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.BAR)
        val table2 = Table(id = 2, size = 2, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyEventGroup(groupSize = 4, tableType = TableType.BAR)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }

    @Test
    fun `reserveTables merge exact capacity match fails - Regular-BAR`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.BAR)
        val table2 = Table(id = 2, size = 2, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyRegularGroup(groupSize = 4, tableType = TableType.BAR)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }

    // ------------------------------------------------------------------------------------
    @Test
    fun `reserveTables merge tables three quarters rule succeeds- Event-COMMON`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyEventGroup(groupSize = 3, TableType.COMMON) // 3/4 of 4 is 3

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }

    @Test
    fun `reserveTables merge tables three quarters rule succeeds- Regular-COMMON`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyRegularGroup(groupSize = 3, TableType.COMMON) // 3/4 of 4 is 3

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }

    @Test
    fun `reserveTables merge tables three quarters rule succeeds- Event-SEPARATED`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 2, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyEventGroup(groupSize = 3, TableType.SEPARATED) // 3/4 of 4 is 3

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }

    @Test
    fun `reserveTables merge tables three quarters rule succeeds- Regular-SEPARATED`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 2, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyRegularGroup(groupSize = 3, TableType.SEPARATED) // 3/4 of 4 is 3

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }

    @Test
    fun `reserveTables merge tables three quarters rule fails - EVENT-BAR`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.BAR)
        val table2 = Table(id = 2, size = 2, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyEventGroup(groupSize = 3, tableType = TableType.BAR)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }

    @Test
    fun `reserveTables merge tables three quarters rule fails - Regular-BAR`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.BAR)
        val table2 = Table(id = 2, size = 2, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyRegularGroup(groupSize = 3, tableType = TableType.BAR)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }

    // ------------------------------------------------------------------------------------
    @Test
    fun `reserveTables insufficient total capacity returns false- Event-COMMON`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyEventGroup(groupSize = 6, TableType.COMMON)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }

    @Test
    fun `reserveTables insufficient total capacity returns false- Regular-COMMON`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyRegularGroup(groupSize = 6, TableType.COMMON)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }

    @Test
    fun `reserveTables insufficient total capacity returns false- Event-SEPARATED`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 2, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyEventGroup(groupSize = 6, TableType.SEPARATED)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }

    @Test
    fun `reserveTables insufficient total capacity returns false- Regular-SEPARATED`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 2, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyRegularGroup(groupSize = 6, TableType.SEPARATED)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }

    @Test
    fun `reserveTables insufficient total capacity returns false- Event-Bar`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 2, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyEventGroup(groupSize = 6, TableType.COMMON)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }

    @Test
    fun `reserveTables insufficient total capacity returns false- Regular-BAR`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 2, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyRegularGroup(groupSize = 6, TableType.COMMON)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }

    // =========================================================================
    //  TRIMMING, BOUNDARIES & TIE-BREAKS

    @Test
    fun `reserveTables merge drops smallest table when capacity exceeds requirement- Event-COMMON`() {
        // Merge tables {2, 4, 4} for group 7. Drops the smallest (size 2).
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 4, tableType = TableType.COMMON)
        val table3 = Table(id = 3, size = 4, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2, table3))
        val eventGroup = createDummyEventGroup(groupSize = 7, TableType.COMMON) // 8 * 0.75 = 6

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
        assertEquals(TableStatus.RESERVED, table3.status)
    }

    @Test
    fun `reserveTables merge drops smallest table when capacity exceeds requirement- Regular-COMMON`() {
        // Merge tables {2, 4, 4} for group 7. Drops the smallest (size 2).
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 4, tableType = TableType.COMMON)
        val table3 = Table(id = 3, size = 4, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2, table3))
        val eventGroup = createDummyRegularGroup(groupSize = 7, TableType.COMMON) // 8 * 0.75 = 6

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
        assertEquals(TableStatus.RESERVED, table3.status)
    }

    @Test
    fun `reserveTables merge drops smallest table when capacity exceeds requirement- Event-SEPARATED`() {
        // Merge tables {2, 4, 4} for group 7. Drops the smallest (size 2).
        val table1 = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 4, tableType = TableType.SEPARATED)
        val table3 = Table(id = 3, size = 4, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2, table3))
        val eventGroup = createDummyEventGroup(groupSize = 7, TableType.SEPARATED) // 8 * 0.75 = 6

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
        assertEquals(TableStatus.RESERVED, table3.status)
    }

    @Test
    fun `reserveTables merge drops smallest table when capacity exceeds requirement- Regular-SEPARATED`() {
        // Merge tables {2, 4, 4} for group 7. Drops the smallest (size 2).
        val table1 = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 4, tableType = TableType.SEPARATED)
        val table3 = Table(id = 3, size = 4, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2, table3))
        val eventGroup = createDummyRegularGroup(groupSize = 7, TableType.SEPARATED) // 8 * 0.75 = 6

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
        assertEquals(TableStatus.RESERVED, table3.status)
    }
}
