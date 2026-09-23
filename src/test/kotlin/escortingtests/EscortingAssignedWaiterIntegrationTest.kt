package escortingtests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Countertop
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Pantry
import de.unisaarland.cs.se.selab.restaurant.Table
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests escorting of groups with an assigned waiter through the FrontOfHouse.
 */
class EscortingAssignedWaiterIntegrationTest {
    private lateinit var output: StringWriter

    private val water = Ingredient("water", MeasurementUnit.ML, 5, 1000)
    private val soup = Recipe(1, "soup", 10, listOf(CookType.TOURNANT), mutableMapOf(water to 1), null)
    private val menu = listOf(soup)

    @BeforeTest
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 5
        Time.evening = 1
    }

    private fun lines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun escortingLines(): List<String> = lines().filter { it.contains("FOH Escorting (") }

    private fun frontOfHouse(tables: List<Table>, waiters: List<Waiter>): FrontOfHouse {
        val pantry = Pantry(
            mutableListOf(IngredientPackage(water)),
            Supplier(Stock(listOf(water)))
        )
        val countertop = Countertop(
            pantry,
            ArrayDeque(),
            listOf(Cook(CookType.TOURNANT)),
            RestaurantType.EUROPEAN
        )
        return FrontOfHouse(tables, waiters, emptyList(), countertop)
    }

    private fun waiter(id: Int) = Waiter().apply { this.id = id }

    private fun casualGroup(id: Int, size: Int) = CasualGroup(
        id,
        size,
        TableType.COMMON,
        5,
        List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        listOf(RestaurantType.EUROPEAN),
        listOf(1),
        0,
        RatingLikelihood.ALWAYS,
    )

    private fun regularGroup(id: Int, size: Int) = RegularGroup(
        id,
        size,
        TableType.COMMON,
        5,
        List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) },
        1,
        1,
        1,
    )

    private fun finishEating(customerGroup: CustomerGroup) {
        val order = checkNotNull(customerGroup.currentOrder)
        order.dishes.forEach { it.status = DishStatus.EATEN }
    }

    @Test
    fun `Freed Casual Table Is Taken By Next Group`() {
        val frontOfHouse = frontOfHouse(listOf(Table(1, 4, TableType.COMMON)), listOf(waiter(1)))
        val firstCasualGroup = casualGroup(1, 4)

        frontOfHouse.processArrival(firstCasualGroup, menu)
        assertEquals(0, frontOfHouse.getAvailableSeats()[TableType.COMMON])

        finishEating(firstCasualGroup)
        frontOfHouse.processEscorting()

        assertEquals(0, firstCasualGroup.customersRemainingInRestaurant)
        assertEquals(4, frontOfHouse.getAvailableSeats()[TableType.COMMON])

        frontOfHouse.clearActionLoads()
        val secondCasualGroup = casualGroup(2, 4)
        frontOfHouse.processArrival(secondCasualGroup, menu)

        assertTrue(lines().any { it.contains("FOH Seating (R 1): Group 2 seated at table 1") })
    }

    @Test
    fun `Seating Waiter Escorts The Group`() {
        val frontOfHouse = frontOfHouse(
            listOf(Table(1, 2, TableType.COMMON), Table(2, 2, TableType.COMMON)),
            listOf(waiter(1), waiter(2))
        )
        val firstCasualGroup = casualGroup(1, 2)
        val secondCasualGroup = casualGroup(2, 2)

        frontOfHouse.processArrival(firstCasualGroup, menu)
        frontOfHouse.processArrival(secondCasualGroup, menu)
        val seatedBy = lines().filter { it.contains("FOH Seating (R 1)") }
            .associate { line ->
                line.substringAfter("Group ").substringBefore(" seated") to line.substringAfter("waitstaff ").trim('.')
            }

        finishEating(firstCasualGroup)
        finishEating(secondCasualGroup)
        frontOfHouse.processEscorting()

        val escortedBy = escortingLines().associate { line ->
            line.substringAfter("of group ").substringBefore(" from") to
                line.substringAfter("Waitstaff ").substringBefore(" escorts")
        }
        assertEquals(seatedBy, escortedBy)
    }

    @Test
    fun `Regular Group Reserved Table Is Not Released`() {
        val table = Table(1, 4, TableType.COMMON)
        val frontOfHouse = frontOfHouse(listOf(table), listOf(waiter(1)))
        val regularGroup = regularGroup(1, 4)

        assertTrue(frontOfHouse.reserveTables(regularGroup))
        frontOfHouse.processArrival(regularGroup, menu)
        finishEating(regularGroup)
        frontOfHouse.processEscorting()

        assertEquals(0, regularGroup.customersRemainingInRestaurant)
        assertEquals(TableStatus.RESERVED, table.status)
        assertEquals(0, frontOfHouse.getAvailableSeats()[TableType.COMMON])
    }

    @Test
    fun `Waiter Out Of Escort Actions Finishes Second Table Next Tick`() {
        val frontOfHouse = frontOfHouse(
            listOf(Table(1, 6, TableType.COMMON), Table(2, 6, TableType.COMMON)),
            listOf(waiter(1))
        )
        val firstCasualGroup = casualGroup(1, 6)
        val secondCasualGroup = casualGroup(2, 6)
        frontOfHouse.processArrival(firstCasualGroup, menu)
        frontOfHouse.clearActionLoads()
        frontOfHouse.processArrival(secondCasualGroup, menu)
        finishEating(firstCasualGroup)
        finishEating(secondCasualGroup)

        frontOfHouse.processEscorting()

        assertEquals(0, firstCasualGroup.customersRemainingInRestaurant)
        assertEquals(2, secondCasualGroup.customersRemainingInRestaurant)
        assertEquals(6, frontOfHouse.getAvailableSeats()[TableType.COMMON])

        frontOfHouse.processRatings(0, 0)
        frontOfHouse.clearActionLoads()
        frontOfHouse.processEscorting()

        assertEquals(0, secondCasualGroup.customersRemainingInRestaurant)
        assertEquals(12, frontOfHouse.getAvailableSeats()[TableType.COMMON])
        assertEquals(
            listOf(6, Constants.ACTION_LIMIT - 6, 2),
            escortingLines().map { it.substringAfter("escorts ").substringBefore(" customers").toInt() }
        )
    }

    @Test
    fun `Closing Time Empties Restaurant Without Escorting`() {
        val frontOfHouse = frontOfHouse(
            listOf(Table(1, 2, TableType.COMMON), Table(2, 2, TableType.COMMON)),
            listOf(waiter(1))
        )
        val eatingCasualGroup = casualGroup(1, 2)
        val waitingCasualGroup = casualGroup(2, 2)
        frontOfHouse.processArrival(eatingCasualGroup, menu)
        frontOfHouse.processArrival(waitingCasualGroup, menu)
        finishEating(eatingCasualGroup)

        frontOfHouse.startFohClosing()

        assertEquals(0, eatingCasualGroup.customersRemainingInRestaurant)
        assertEquals(0, waitingCasualGroup.customersRemainingInRestaurant)
        assertTrue(escortingLines().isEmpty())
    }

    @Test
    fun `Ending Opening Time Releases Tables`() {
        val table = Table(1, 4, TableType.COMMON)
        val frontOfHouse = frontOfHouse(listOf(table), listOf(waiter(1)))
        val casualGroup = casualGroup(1, 4)
        frontOfHouse.processArrival(casualGroup, menu)

        frontOfHouse.startFohClosing()
        frontOfHouse.endFohOpeningTime()

        assertEquals(TableStatus.FREE, table.status)
        assertEquals(4, frontOfHouse.getAvailableSeats()[TableType.COMMON])
    }
}
