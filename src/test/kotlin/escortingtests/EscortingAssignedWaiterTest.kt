package escortingtests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.EscortingProcessor
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the EscortingProcessor for groups that are escorted by their assigned waiter.
 */
class EscortingAssignedWaiterTest {
    private lateinit var output: StringWriter

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

    private val water = Ingredient("water", MeasurementUnit.ML, 10, 1)
    private val soup = Recipe(1, "soup", 1, emptyList(), mutableMapOf(water to 1), null)

    private fun order(dishes: Int, status: DishStatus = DishStatus.EATEN): Order =
        Order(List(dishes) { Dish(soup, false, 0, status) })

    private fun foodPreferences(size: Int) = List(size) { FoodPreference(emptyList(), emptyList(), emptyList()) }

    private fun casualGroup(id: Int, size: Int, order: Order? = order(size)) = CasualGroup(
        id,
        size,
        TableType.COMMON,
        5,
        foodPreferences(size),
        listOf(RestaurantType.EUROPEAN),
        listOf(1),
        0,
        RatingLikelihood.ALWAYS,
    ).also { it.currentOrder = order }

    private fun regularGroup(id: Int, size: Int) = RegularGroup(
        id,
        size,
        TableType.COMMON,
        5,
        foodPreferences(size),
        1,
        1,
        1,
    ).also { it.currentOrder = order(size) }

    private fun escortingProcessor(
        inHouse: Map<CustomerGroup, Waiter>,
        tables: MutableMap<CustomerGroup, List<Table>>,
    ): EscortingProcessor {
        var nextId: Id = 1
        return EscortingProcessor(
            tables,
            mutableListOf(),
            inHouse,
            { inHouse.keys.toList() },
            { group -> if (group is RegularGroup) 0 else 2 },
            { _, _ -> emptyList() },
            { nextId++ },
        )
    }

    @Test
    fun `Table Still Eating Is Not Escorted`() {
        val casualGroup = casualGroup(1, 2, order(2, DishStatus.SERVED))
        val inHouse = mapOf<CustomerGroup, Waiter>(casualGroup to Waiter())
        val tables = mutableMapOf<CustomerGroup, List<Table>>(casualGroup to listOf(Table(1, 2, TableType.COMMON)))

        escortingProcessor(inHouse, tables).processEscorting()

        assertEquals(2, casualGroup.customersRemainingInRestaurant)
        assertTrue(escortingLines().isEmpty())
    }

    @Test
    fun `Group Without Order Is Skipped`() {
        val casualGroup = casualGroup(1, 3, order = null)
        val inHouse = mapOf<CustomerGroup, Waiter>(casualGroup to Waiter())
        val tables = mutableMapOf<CustomerGroup, List<Table>>(casualGroup to listOf(Table(1, 3, TableType.COMMON)))

        escortingProcessor(inHouse, tables).processEscorting()

        assertEquals(3, casualGroup.customersRemainingInRestaurant)
        assertTrue(escortingLines().isEmpty())
        assertEquals(
            "[DEBUG] FOH Escorting Status (R 1): 0 waitstaff escorted 0 customers this tick.",
            lines().last { it.contains("FOH Escorting Status") }
        )
    }

    @Test
    fun `Assigned Waiter Escorts Group And Gets Id`() {
        val casualGroup = casualGroup(7, 4)
        val waiter = Waiter().also { it.addToCurrentLoad(4) }
        val inHouse = mapOf<CustomerGroup, Waiter>(casualGroup to waiter)
        val tables = mutableMapOf<CustomerGroup, List<Table>>(casualGroup to listOf(Table(3, 4, TableType.COMMON)))

        escortingProcessor(inHouse, tables).processEscorting()

        assertEquals(
            listOf("[IMPORTANT] FOH Escorting (R 1): Waitstaff 1 escorts 4 customers of group 7 from table 3 outside."),
            escortingLines()
        )
        assertEquals(1, waiter.id)
        assertEquals(0, waiter.currentLoad)
    }

    @Test
    fun `Casual Group Frees Its Table`() {
        val casualGroup = casualGroup(1, 4)
        val table = Table(2, 4, TableType.COMMON).also { it.status = TableStatus.OCCUPIED }
        val inHouse = mapOf<CustomerGroup, Waiter>(casualGroup to Waiter())
        val tables = mutableMapOf<CustomerGroup, List<Table>>(casualGroup to listOf(table))

        escortingProcessor(inHouse, tables).processEscorting()

        assertEquals(0, casualGroup.customersRemainingInRestaurant)
        assertEquals(TableStatus.FREE, table.status)
        assertTrue(tables.isEmpty())
    }

    @Test
    fun `All Tables Of Merged Casual Table Are Freed`() {
        val casualGroup = casualGroup(1, 6)
        val mergedTables = listOf(
            Table(5, 3, TableType.COMMON).also { it.status = TableStatus.OCCUPIED },
            Table(2, 3, TableType.COMMON).also { it.status = TableStatus.OCCUPIED },
        )
        val inHouse = mapOf<CustomerGroup, Waiter>(casualGroup to Waiter())
        val tables = mutableMapOf<CustomerGroup, List<Table>>(casualGroup to mergedTables)

        escortingProcessor(inHouse, tables).processEscorting()

        assertTrue(mergedTables.all { it.status == TableStatus.FREE })
        assertTrue(escortingLines().single().endsWith("from table 2 outside."))
    }

    @Test
    fun `Regular Group Keeps Reserved Table`() {
        val regularGroup = regularGroup(1, 2)
        val table = Table(3, 2, TableType.COMMON).also { it.status = TableStatus.RESERVED }
        val inHouse = mapOf<CustomerGroup, Waiter>(regularGroup to Waiter())
        val tables = mutableMapOf<CustomerGroup, List<Table>>(regularGroup to listOf(table))

        escortingProcessor(inHouse, tables).processEscorting()

        assertEquals(0, regularGroup.customersRemainingInRestaurant)
        assertEquals(TableStatus.RESERVED, table.status)
        assertEquals(1, tables.size)
    }

    @Test
    fun `Group Larger Than Action Limit Leaves Over Two Ticks`() {
        val size = Constants.ACTION_LIMIT + 2
        val casualGroup = casualGroup(1, size)
        val waiter = Waiter().also { it.addToCurrentLoad(size) }
        val inHouse = mapOf<CustomerGroup, Waiter>(casualGroup to waiter)
        val tables = mutableMapOf<CustomerGroup, List<Table>>(casualGroup to listOf(Table(1, size, TableType.COMMON)))
        val escortingProcessor = escortingProcessor(inHouse, tables)

        escortingProcessor.processEscorting()
        assertEquals(2, casualGroup.customersRemainingInRestaurant)
        assertEquals(Constants.ACTION_LIMIT, waiter.getTickLoad(ActionType.ESCORT))

        waiter.resetActionLoads()
        escortingProcessor.processEscorting()

        assertEquals(0, casualGroup.customersRemainingInRestaurant)
        assertEquals(
            listOf(10, 2),
            escortingLines().map { it.substringAfter("escorts ").substringBefore(" customers").toInt() }
        )
    }

    @Test
    fun `Escorting Counters Report This Tick Only`() {
        val casualGroup = casualGroup(1, 4)
        val inHouse = mapOf<CustomerGroup, Waiter>(casualGroup to Waiter())
        val tables = mutableMapOf<CustomerGroup, List<Table>>(casualGroup to listOf(Table(1, 4, TableType.COMMON)))
        val escortingProcessor = escortingProcessor(inHouse, tables)
        escortingProcessor.customerEscortingNumber = 99

        escortingProcessor.processEscorting()

        assertEquals(4, escortingProcessor.customerEscortingNumber)
        assertEquals(1, escortingProcessor.waitstaffNumber)
    }

    @Test
    fun `Two Groups Escorted In Same Tick`() {
        val firstCasualGroup = casualGroup(1, 2)
        val secondCasualGroup = casualGroup(2, 3)
        val inHouse = linkedMapOf<CustomerGroup, Waiter>(firstCasualGroup to Waiter(), secondCasualGroup to Waiter())
        val tables = mutableMapOf<CustomerGroup, List<Table>>(
            firstCasualGroup to listOf(Table(1, 2, TableType.COMMON)),
            secondCasualGroup to listOf(Table(2, 3, TableType.COMMON)),
        )

        escortingProcessor(inHouse, tables).processEscorting()

        assertEquals(2, escortingLines().size)
        assertEquals(
            "[DEBUG] FOH Escorting Status (R 1): 2 waitstaff escorted 5 customers this tick.",
            lines().last { it.contains("FOH Escorting Status") }
        )
    }

    @Test
    fun `Each Group Escorted By Own Waiter From Own Table`() {
        val regularGroup = regularGroup(1, 2)
        val casualGroup = casualGroup(4, 3)
        val regularGroupWaiter = Waiter()
        val casualGroupWaiter = Waiter()
        val inHouse = linkedMapOf(
            regularGroup to regularGroupWaiter,
            casualGroup to casualGroupWaiter
        )
        val tables = mutableMapOf(
            regularGroup to listOf(Table(1, 2, TableType.COMMON)),
            casualGroup to listOf(Table(7, 3, TableType.COMMON)),
        )

        escortingProcessor(inHouse, tables).processEscorting()

        assertEquals(
            listOf(
                "[IMPORTANT] FOH Escorting (R 1): Waitstaff 1 escorts 2 customers of group 1 from table 1 outside.",
                "[IMPORTANT] FOH Escorting (R 1): Waitstaff 2 escorts 3 customers of group 4 from table 7 outside.",
            ),
            escortingLines()
        )
    }

    @Test
    fun `Waiter Out Of Escort Actions Escorts Nobody`() {
        val casualGroup = casualGroup(1, 3)
        val waiter = Waiter().also { it.addToTickLoad(ActionType.ESCORT, Constants.ACTION_LIMIT) }
        val inHouse = mapOf<CustomerGroup, Waiter>(casualGroup to waiter)
        val tables = mutableMapOf<CustomerGroup, List<Table>>(casualGroup to listOf(Table(1, 3, TableType.COMMON)))

        escortingProcessor(inHouse, tables).processEscorting()

        assertEquals(3, casualGroup.customersRemainingInRestaurant)
        assertTrue(escortingLines().isEmpty())
        assertEquals(
            "[DEBUG] FOH Escorting Status (R 1): 0 waitstaff escorted 0 customers this tick.",
            lines().last { it.contains("FOH Escorting Status") }
        )
    }

    @Test
    fun `Group With Aborted Meals Is Escorted`() {
        val casualGroup = casualGroup(1, 2, order(2, DishStatus.ABORTED))
        val inHouse = mapOf<CustomerGroup, Waiter>(casualGroup to Waiter())
        val tables = mutableMapOf<CustomerGroup, List<Table>>(casualGroup to listOf(Table(1, 2, TableType.COMMON)))

        escortingProcessor(inHouse, tables).processEscorting()

        assertEquals(0, casualGroup.customersRemainingInRestaurant)
        assertEquals(1, escortingLines().size)
    }
}
