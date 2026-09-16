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

class StaffIncidentParserTest {
    private val parser: IncidentParser = IncidentParser()

    // Test fixtures
    private val testIngredient: Ingredient = Ingredient(
        name = "garlic",
        unit = MeasurementUnit.G,
        bestBefore = 50,
        initialPackagingVolume = 1,
    )

    private val ingredientsList: List<Ingredient> = listOf(
        testIngredient
    )

    private val testStock: Stock = Stock(
        ingredientList = ingredientsList
    )

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

    private val restaurantsList: List<Restaurant> = listOf(
        fakeRestaurant
    )

    // EXCEPTION tests (Negative case scenarios)

    @Test
    fun `staff Incident-Unknown Staff type Enum-Fails`() {
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
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `staff Incident-Missing Restaurant Property-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 2)
                put("evening", 1)
                put("type", "STAFF")
                put("staffType", "DRIVER")
                put("number", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `staff Incident-Unknown Restaurant Id-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 3)
                put("evening", 1)
                put("type", "STAFF")
                put("restaurant", 999)
                put("staffType", "WAITSTAFF")
                put("number", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `staff Incident-Negative Restaurant Id-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 4)
                put("evening", 1)
                put("type", "STAFF")
                put("restaurant", -10)
                put("staffType", "WAITSTAFF")
                put("number", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `staff Incident-Cook Missing Cook Type-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 5)
                put("evening", 1)
                put("type", "STAFF")
                put("restaurant", 10)
                put("staffType", "COOK")
                put("number", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `staff Incident-Cook Invalid Cook Type Enum-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 6)
                put("evening", 1)
                put("type", "STAFF")
                put("restaurant", 10)
                put("staffType", "COOK")
                put("cookType", "Walter_White")
                put("number", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `staff Incident-Missing Number Property-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 7)
                put("evening", 1)
                put("type", "STAFF")
                put("restaurant", 10)
                put("staffType", "WAITSTAFF")
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `staff Incident-Non Integer Number-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 8)
                put("evening", 1)
                put("type", "STAFF")
                put("restaurant", 10)
                put("staffType", "DRIVER")
                put("number", "ONE")
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `staff Incident-Missing Evening-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 9)
                put("type", "STAFF")
                put("restaurant", 10)
                put("staffType", "COOK")
                put("cookType", "SOUS")
                put("number", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `staff Incident-Missing Id-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("evening", 1)
                put("type", "STAFF")
                put("restaurant", 10)
                put("staffType", "WAITSTAFF")
                put("number", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `staff Incident-Missing Type Property-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 11)
                put("evening", 1)
                put("restaurant", 10)
                put("staffType", "WAITSTAFF")
                put("number", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `staff Incident-Missing StaffType Property-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 12)
                put("evening", 1)
                put("type", "STAFF")
                put("restaurant", 10)
                put("number", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }

    @Test
    fun `staff Incident-Unknown Incident Type Enum-Fails`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 18)
                put("evening", 1)
                put("type", "DEPRESSION")
                put("restaurant", 10)
                put("staffType", "WAITSTAFF")
                put("number", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredientsList, testStock, recipes, restaurantsList)
        }
    }
}
