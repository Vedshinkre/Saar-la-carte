package mergetablestest

import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.LogLevel
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
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import kotlin.collections.emptyList

/** Reservation Table Merging 2 Test F 15
 * ReservationTableMerging2Test focuses on edge-case reservation rules and tie-breaking.
 * It tests tie-break that drops lower-ID tables when table sizes match, verifies table filtering
 * that skips non-free or type-mismatched tables, checks rules where oversized single or merged tables are
 * allocated when the three-quarters threshold cannot be met, and  rejects when total capacity is insufficient .
 */
class ReservationTableMerging2Test {

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

    private fun setupFoh(tables: List<Table>): FrontOfHouse {
        return FrontOfHouse(
            tables = tables,
            waiters = emptyList(),
            drivers = emptyList(),
            countertop = createDummyCountertop()
        )
    }

    // ------------------------------------------------------------------------------------
    @Test
    fun `reserveTables merge trim tie-break drops lower ID when sizes are equal- EVENT Common`() {
        // Group size 6. Tables: T1(2), T2(2), T3(5).
        // Accumulates to {T1, T2, T3} total 9.
        // Trims T1 (lowest ID among the size 2 tables).
        // Remaining {T2, T3} total 7, which fits group size 6.
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val table3 = Table(id = 3, size = 5, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2, table3))

        val eventGroup = createDummyEventGroup(groupSize = 6, tableType = TableType.COMMON)

        assertTrue(foh.reserveTables(eventGroup))

        // Table 1 (lower ID) is dropped and remains FREE
        assertEquals(TableStatus.FREE, table1.status)
        // Tables 2 and 3 are kept and RESERVED
        assertEquals(TableStatus.RESERVED, table2.status)
        assertEquals(TableStatus.RESERVED, table3.status)
    }

    @Test
    fun `reserveTables merge trim tie-break drops lower ID when sizes are equal- REGULAR Common`() {
        // Group size 6. Tables: T1(2), T2(2), T3(5).
        // Accumulates to {T1, T2, T3} total 9.
        // Trims T1 (lowest ID among the size 2 tables).
        // Remaining {T2, T3} total 7, which fits group size 6.
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val table3 = Table(id = 3, size = 5, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2, table3))

        val eventGroup = createDummyRegularGroup(groupSize = 6, tableType = TableType.COMMON)

        assertTrue(foh.reserveTables(eventGroup))

        // Table 1 (lower ID) is dropped and remains FREE
        assertEquals(TableStatus.FREE, table1.status)
        // Tables 2 and 3 are kept and RESERVED
        assertEquals(TableStatus.RESERVED, table2.status)
        assertEquals(TableStatus.RESERVED, table3.status)
    }

    @Test
    fun `reserveTables merge trim tie-break drops lower ID when sizes are equal- EVENT SEPARATED`() {
        // Group size 6. Tables: T1(2), T2(2), T3(5).
        // Accumulates to {T1, T2, T3} total 9.
        // Trims T1 (lowest ID among the size 2 tables).
        // Remaining {T2, T3} total 7, which fits group size 6.
        val table1 = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 2, tableType = TableType.SEPARATED)
        val table3 = Table(id = 3, size = 5, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2, table3))

        val eventGroup = createDummyEventGroup(groupSize = 6, tableType = TableType.SEPARATED)

        assertTrue(foh.reserveTables(eventGroup))

        // Table 1 (lower ID) is dropped and remains FREE
        assertEquals(TableStatus.FREE, table1.status)
        // Tables 2 and 3 are kept and RESERVED
        assertEquals(TableStatus.RESERVED, table2.status)
        assertEquals(TableStatus.RESERVED, table3.status)
    }

    @Test
    fun `reserveTables merge trim tie-break drops lower ID when sizes are equal- REGULAR SEPARATED`() {
        // Group size 6. Tables: T1(2), T2(2), T3(5).
        // Accumulates to {T1, T2, T3} total 9.
        // Trims T1 (lowest ID among the size 2 tables).
        // Remaining {T2, T3} total 7, which fits group size 6.
        val table1 = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 2, tableType = TableType.SEPARATED)
        val table3 = Table(id = 3, size = 5, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2, table3))

        val eventGroup = createDummyRegularGroup(groupSize = 6, tableType = TableType.SEPARATED)

        assertTrue(foh.reserveTables(eventGroup))

        // Table 1 (lower ID) is dropped and remains FREE
        assertEquals(TableStatus.FREE, table1.status)
        // Tables 2 and 3 are kept and RESERVED
        assertEquals(TableStatus.RESERVED, table2.status)
        assertEquals(TableStatus.RESERVED, table3.status)
    }

    @Test
    fun `reserveTables ignores tables of wrong type-Event`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyEventGroup(groupSize = 4, tableType = TableType.COMMON)

        assertFalse(foh.reserveTables(eventGroup))
    }

    @Test
    fun `reserveTables ignores tables of wrong type-Regular`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.BAR)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyRegularGroup(groupSize = 4, tableType = TableType.COMMON)

        assertFalse(foh.reserveTables(eventGroup))
    }

    @Test
    fun `reserveTables only considers FREE tables- EVENT`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.COMMON)
        table1.status = TableStatus.OCCUPIED
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyEventGroup(groupSize = 4, tableType = TableType.COMMON)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.OCCUPIED, table1.status)
    }

    @Test
    fun `reserveTables only considers FREE tables- Regular`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.COMMON)
        table1.status = TableStatus.OCCUPIED
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyRegularGroup(groupSize = 4, tableType = TableType.COMMON)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.OCCUPIED, table1.status)
    }

// =========================================================================

    @Test
    fun `reserveTables step 5 succeeds without three-quarters check- event`() {
        // Table size 8, Group size 5.
        // 3/4 of 8 is 6. Group size 5 fails the strict check, but step 5 allows it.
        val table1 = Table(id = 1, size = 8, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyEventGroup(groupSize = 5, tableType = TableType.COMMON)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables step 5 succeeds without three-quarters check- regular`() {
        // Table size 8, Group size 5.
        // 3/4 of 8 is 6. Group size 5 fails the strict check, but step 5 allows it.
        val table1 = Table(id = 1, size = 8, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyRegularGroup(groupSize = 5, tableType = TableType.COMMON)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables step 5 succeeds without three-quarters check- event SEPERATED`() {
        // Table size 8, Group size 5.
        // 3/4 of 8 is 6. Group size 5 fails the strict check, but step 5 allows it.
        val table1 = Table(id = 1, size = 8, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyEventGroup(groupSize = 5, tableType = TableType.SEPARATED)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }

    @Test
    fun `reserveTables step 5 succeeds without three-quarters check- regular SEPARATED`() {
        // Table size 8, Group size 5.
        // 3/4 of 8 is 6. Group size 5 fails the strict check, but step 5 allows it.
        val table1 = Table(id = 1, size = 8, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1))
        val eventGroup = createDummyRegularGroup(groupSize = 5, tableType = TableType.SEPARATED)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
    }
// =========================================================================

    @Test
    fun `reserveTables step 6 merges without three-quarters check- event`() {
        // Tables: 4, 4. Group size: 5. Total capacity: 8.
        // 3/4 of 8 is 6. Group size 5 fails the strict merge check, but step 6 allows it.
        val table1 = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 4, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyEventGroup(groupSize = 5, tableType = TableType.COMMON)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }

    @Test
    fun `reserveTables step 6 merges without three-quarters check- regular`() {
        // Tables: 4, 4. Group size: 5. Total capacity: 8.
        // 3/4 of 8 is 6. Group size 5 fails the strict merge check, but step 6 allows it.
        val table1 = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 4, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyRegularGroup(groupSize = 5, tableType = TableType.COMMON)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }

    @Test
    fun `reserveTables step 6 merges without three-quarters check- event SEPARATED`() {
        // Tables: 4, 4. Group size: 5. Total capacity: 8.
        // 3/4 of 8 is 6. Group size 5 fails the strict merge check, but step 6 allows it.
        val table1 = Table(id = 1, size = 4, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 4, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyEventGroup(groupSize = 5, tableType = TableType.SEPARATED)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }

    @Test
    fun `reserveTables step 6 merges without three-quarters check- regular SEPARATED`() {
        // Tables: 4, 4. Group size: 5. Total capacity: 8.
        // 3/4 of 8 is 6. Group size 5 fails the strict merge check, but step 6 allows it.
        val table1 = Table(id = 1, size = 4, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 4, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyRegularGroup(groupSize = 5, tableType = TableType.SEPARATED)

        assertTrue(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }

// =========================================================================

    @Test
    fun `reserveTables SEPARATED tables reject if total capacity is insufficient - Event`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 2, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyEventGroup(groupSize = 6, tableType = TableType.SEPARATED)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }

    @Test
    fun `reserveTables SEPARATED tables reject if total capacity is insufficient - Regular`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val table2 = Table(id = 2, size = 2, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(table1, table2))
        val eventGroup = createDummyRegularGroup(groupSize = 6, tableType = TableType.SEPARATED)

        assertFalse(foh.reserveTables(eventGroup))
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }
}
