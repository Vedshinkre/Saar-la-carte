package incidentparsertests

import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.parsers.IncidentParser
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StaffIncidentParserTest {

    private val parser: IncidentParser = IncidentParser()

    // Test fixtures
    private val testIngredient: Ingredient = Ingredient(
        name = "garlic",
        unit = MeasurementUnit.G,
        bestBefore = 50,
        packagingVolume = 1
    )

    private val ingredientsList: List<Ingredient> = listOf(
        testIngredient
    )

    private val testStock: Stock = Stock(
        ingredientList = ingredientsList
    )

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

    private val restaurantsList: List<Restaurant> = listOf(
        fakeRestaurant
    )

    // EXCEPTION tests (Negative case scenarios)

    @Test
    fun staff_Incident_Unknown_Staff_Type_Enum_Fails() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 1)
                put("evening", 2)
                put("type", "STAFF")
                put("restaurant", 10)
                put("staffType", "DIDDY")
                put("number", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, emptyList(), restaurantsList)
        }
    }
}
