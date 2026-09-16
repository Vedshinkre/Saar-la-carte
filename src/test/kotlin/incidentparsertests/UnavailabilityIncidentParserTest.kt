
package incidentparsertests

import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.parsers.IncidentParser
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UnavailabilityIncidentParserTest {

    private val parser: IncidentParser = IncidentParser()

    private val testIngredient: Ingredient = Ingredient(
        name = "garlic",
        unit = MeasurementUnit.G,
        bestBefore = 50,
        initialPackagingVolume = 0
    )
    private val ingredientsList: List<Ingredient> = listOf(testIngredient)
    private val testStock: Stock = Stock(ingredientList = ingredientsList)
    private val recipes: List<Recipe> = emptyList()
    private val fakeStaff: RestaurantStaff = RestaurantStaff(
        cooks = mutableListOf(),
        waiters = mutableListOf(),
        drivers = mutableListOf()
    )
    private val fakeStats: RestaurantStats = RestaurantStats(
        positiveRatings = 0,
        negativeRatings = 0,
        openingTickStart = 1,
        openingTickEnd = 24,
        menu = emptyList(),
        event = false,
        restaurantType = RestaurantType.EUROPEAN,
        restaurantId = 10
    )
    private val fakeRestaurant: Restaurant = Restaurant(
        restaurantStats = fakeStats,
        name = "Get Cooked",
        staff = fakeStaff,
        tables = emptyList(),
        stock = testStock
    )
    private val restaurantsList: List<Restaurant> = listOf(fakeRestaurant)

    @Test
    fun `unavailable Incident-Missing Ingredient Property-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 1)
                put("evening", 1)
                put("type", "UNAVAILABLE")
                put("duration", 2)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `unavailable Incident-Unknown Ingredient Name-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 2)
                put("evening", 1)
                put("type", "UNAVAILABLE")
                put("ingredient", "LOVE")
                put("duration", 2)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `unavailable Incident-Missing Duration Property-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 3)
                put("evening", 1)
                put("type", "UNAVAILABLE")
                put("ingredient", "garlic")
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }
}
