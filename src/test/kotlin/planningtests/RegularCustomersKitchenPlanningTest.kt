package planningtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.Table
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertTrue

/**
 *  F22 kitchen planning: it must (a) combine the order
 * history of every coming REGULAR into one list for the kitchen
 */
class RegularCustomersKitchenPlanningTest {
    private lateinit var output: StringWriter
    private val potato = Ingredient("potato", MeasurementUnit.X, 100, 100)
    private val carrot = Ingredient("carrot", MeasurementUnit.X, 100, 5)
    private val stew = Recipe(1, "Stew", 10, listOf(CookType.TOURNANT), mutableMapOf(potato to 100), null)
    private val soup = Recipe(2, "Soup", 10, listOf(CookType.TOURNANT), mutableMapOf(carrot to 5), null)

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.tick = 1
        Time.evening = 1
    }

    @Test
    fun `prepareForEvening pools every regular's history and treats new regulars as ordinary capacity`() {
        val withHistory = RegularGroup(1, 2, TableType.COMMON, 1, emptyList(), 1, 1, 1).also {
            it.addOrderToHistory(
                Order(
                    listOf(
                        Dish(stew)
                    )
                )
            )
        }
        val firstTimer = RegularGroup(2, 2, TableType.COMMON, 1, emptyList(), 1, 1, 1)

        val restaurant = Restaurant(
            restaurantStats = RestaurantStats(1, RestaurantType.EUROPEAN, 1, 20, false, 0, 0, listOf(soup)),
            name = "Testaurant",
            staff = RestaurantStaff(mutableListOf(), mutableListOf(), mutableListOf()),
            tables = listOf(Table(1, 2, TableType.COMMON), Table(2, 2, TableType.COMMON)),
            stock = Stock(listOf(potato, carrot))
        )

        restaurant.prepareForEvening(listOf(withHistory, firstTimer))

        // withHistory's past Stew order is pooled in: exactly one 100-unit package of potato
        assertTrue(
            output.toString().contains("Procured 100 X of potato"),
            output.toString()
        ) // firstTimer has no history, so its 2 reserved seats still count as free capacity for the
        // menu estimate (2 seats -> 1 estimated Soup) even though both tables are now reserved
        assertTrue(output.toString().contains("Procured 5 X of carrot"), output.toString())
    }
}
