package loggertests

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.loggers.KitchenLogger
import de.unisaarland.cs.se.selab.loggers.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals

class KitchenLoggerTest {
    private lateinit var output: StringWriter
    private val restaurantId: Id = 1
    private val cookId: Id = 2
    private val mealNumber: Int = 3
    private val dishName: String = "chicken rice"
    private val cookDurationTick: Tick = 4
    private val allOrderId: List<Id> = listOf(5)
    private val allOrderIds: List<Id> = listOf(7, 6)
    private val numberOfCooks: Int = 8
    private val finishedMealNumber: Int = 9
    private val servableMeals: Int = 10
    private val cookType: CookType = CookType.EXEC

    @BeforeEach
    fun setup() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = restaurantId
    }

    private val loggedLine: String get() = output.toString().lines().single { it.isNotBlank() }
    private fun List<Id>.toSortedString() = this.sorted().joinToString(separator = ",")

    @Test
    fun `IMPORTANT - Kitchen Dish Assignment(cookId, cookType, mealNumber, dishName, baseId, allOrderId) - id`() {
        val baseOrderId: Id = allOrderId.single()
        KitchenLogger.logKitchenDishAssignment(
            cookId,
            cookType.name,
            mealNumber,
            dishName,
            baseOrderId,
            allOrderId
        )
        assertEquals(
            "[IMPORTANT] Kitchen Dish Assignment (R $restaurantId): Cook $cookId " +
                "of type ${cookType.name} starts cooking $mealNumber meals of dish $dishName " +
                "based on order $baseOrderId for orders ${allOrderId.toSortedString()}.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - Kitchen Dish Assignment(cookId, cookType, mealNumber, dishName, baseId, allOrderIds) - ids`() {
        val baseOrderId: Id = allOrderIds.min()
        KitchenLogger.logKitchenDishAssignment(
            cookId,
            cookType.name,
            mealNumber,
            dishName,
            baseOrderId,
            allOrderIds
        )
        assertEquals(
            "[IMPORTANT] Kitchen Dish Assignment (R $restaurantId): Cook $cookId " +
                "of type ${cookType.name} starts cooking $mealNumber meals of dish $dishName " +
                "based on order $baseOrderId for orders ${allOrderIds.toSortedString()}.",
            loggedLine
        )
    }

    @Test
    fun `IMPORTANT - Kitchen Meal Cooked(cookId, mealNumber, dishName, cookDurationTick)`() {
        KitchenLogger.logKitchenMealCooked(cookId, mealNumber, dishName, cookDurationTick)
        assertEquals(
            "[IMPORTANT] Kitchen Meal Cooked (R $restaurantId): Cook $cookId " +
                "finished cooking $mealNumber meals of dish $dishName $cookDurationTick ticks after ordering.",
            loggedLine
        )
    }

    @Test
    fun `DEBUG - Kitchen Status(numberOfCooks, mealNumber, finishedMealNumber, servableMeals)`() {
        KitchenLogger.logKitchenStatus(numberOfCooks, mealNumber, finishedMealNumber, servableMeals)
        assertEquals(
            "[DEBUG] Kitchen Status (R $restaurantId): $numberOfCooks cooks were " +
                "active cooking $mealNumber and finishing $finishedMealNumber " +
                "meals. $servableMeals meals can be served by the waitstaff.",
            loggedLine
        )
    }
}
