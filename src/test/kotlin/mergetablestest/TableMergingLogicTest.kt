package mergetablestest

import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TableMergingLogicTest {

    // --- Helper Methods for Dummy Data ---

    private fun createDummyCountertop(): Countertop {
        val dummyStock = Stock(emptyList())
        val dummyPantry = Pantry(dummyStock)
        return Countertop(
            pantry = dummyPantry,
            orderQueue = ArrayDeque(),
            cooks = emptyList()
        )
    }

    private fun createDummyCasualGroup(groupSize: Int): CasualGroup {
        return CasualGroup(
            id = 1,
            size = groupSize,
            tableType = TableType.COMMON,
            visitingAt = 1,
            foodPreferences = emptyList(),
            restaurantTypes = emptyList(),
            visitingEvenings = emptyList(),
            deliveryDistance = 0,
            ratingLikelihood = RatingLikelihood.NEVER
        )
    }

    private fun createDummyEventGroup(groupSize: Int): EventGroup {
        return EventGroup(
            id = 2,
            size = groupSize,
            tableType = TableType.COMMON,
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

    // --- FOH Table Merging Tests ---

    @Test
    fun `reserveTables-No Table For Casuals-Fails`() {
        val foh = setupFoh(emptyList())
        val casualGroup = createDummyCasualGroup(5)

        assertThrows<IllegalArgumentException> {
            foh.reserveTables(casualGroup)
        }
    }

    @Test
    fun `reserveTables-Merge Exact Capacity Match-Succeeds`() {
        val table1 = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2))

        val eventGroup = createDummyEventGroup(groupSize = 6)
        val success = foh.reserveTables(eventGroup)

        assertTrue(success)
        assertEquals(TableStatus.RESERVED, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
    }

    @Test
    fun `reserveTables-Insufficient Total Capacity-Returns False`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2))

        val eventGroup = createDummyEventGroup(groupSize = 6)
        val success = foh.reserveTables(eventGroup)

        assertFalse(success)
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.FREE, table2.status)
    }

    @Test
    fun `reserveTables-Select Appropriate Tables-Succeeds`() {
        val table1 = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val table2 = Table(id = 2, size = 4, tableType = TableType.COMMON)
        val table3 = Table(id = 3, size = 4, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(table1, table2, table3))

        val eventGroup = createDummyEventGroup(groupSize = 7)
        val success = foh.reserveTables(eventGroup)

        assertTrue(success)
        assertEquals(TableStatus.FREE, table1.status)
        assertEquals(TableStatus.RESERVED, table2.status)
        assertEquals(TableStatus.RESERVED, table3.status)
    }
}
