package reservationtests

import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the "canceling reservations" half of F14: every table still reserved gets
 * discarded back to FREE at [FrontOfHouse.endFohOpeningTime], per the spec's "reservation
 * from this evening are discarded" rule - regardless of whether the group ever showed up,
 * and regardless of how many groups or how many tables were involved.
 */
class ReservationCancellationTest {

    @BeforeEach
    fun setup() {
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
    }

    private fun frontOfHouse(tables: List<Table>): FrontOfHouse {
        val stock = Stock(emptyList<Ingredient>())
        val countertop = Countertop(
            pantry = Pantry(stock),
            orderQueue = ArrayDeque(),
            cooks = emptyList(),
            restaurantType = RestaurantType.EUROPEAN
        )
        return FrontOfHouse(tables = tables, waiters = emptyList(), drivers = emptyList(), countertop = countertop)
    }

    private fun regularGroup(id: Int, size: Int): RegularGroup = RegularGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 5,
        foodPreferences = emptyList(),
        visitingStart = 1,
        visitingPeriod = 1,
        restaurantId = 1
    )

    private fun eventGroup(id: Int, size: Int): EventGroup = EventGroup(
        id = id,
        size = size,
        tableType = TableType.COMMON,
        visitingAt = 5,
        foodPreferences = emptyList(),
        restaurantTypes = listOf(RestaurantType.ASIAN),
        eventEvening = 4,
        eventDishes = emptyMap()
    )

    @Test
    fun `a table reserved this evening is freed back to FREE once the opening time ends`() {
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val foh = frontOfHouse(listOf(table))
        foh.reserveTables(regularGroup(id = 1, size = 4))

        foh.endFohOpeningTime()

        assertEquals(TableStatus.FREE, table.status)
    }

    @Test
    fun `a discarded reservation genuinely frees the table up for a different group afterward`() {
        val table = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val foh = frontOfHouse(listOf(table))
        foh.reserveTables(regularGroup(id = 1, size = 4))
        foh.endFohOpeningTime()

        val reservedAgain = foh.reserveTables(regularGroup(id = 2, size = 4))

        assertTrue(reservedAgain)
        assertEquals(TableStatus.RESERVED, table.status)
    }

    @Test
    fun `every table from a merged reservation is freed together at the end of the opening time`() {
        val tableOne = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val tableTwo = Table(id = 2, size = 2, tableType = TableType.COMMON)
        val foh = frontOfHouse(listOf(tableOne, tableTwo))
        foh.reserveTables(eventGroup(id = 1, size = 4))

        foh.endFohOpeningTime()

        assertEquals(TableStatus.FREE, tableOne.status)
        assertEquals(TableStatus.FREE, tableTwo.status)
    }

    @Test
    fun `reservations for multiple groups are all discarded together, not just the first`() {
        val regularsTable = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val eventsTable = Table(id = 2, size = 4, tableType = TableType.COMMON)
        val foh = frontOfHouse(listOf(regularsTable, eventsTable))
        foh.reserveTables(regularGroup(id = 1, size = 4))
        foh.reserveTables(eventGroup(id = 2, size = 4))

        foh.endFohOpeningTime()

        assertEquals(TableStatus.FREE, regularsTable.status)
        assertEquals(TableStatus.FREE, eventsTable.status)
    }
}
