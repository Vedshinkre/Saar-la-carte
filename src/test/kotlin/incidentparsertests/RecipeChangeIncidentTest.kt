package incidentparsertests

import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.Incident
import de.unisaarland.cs.se.selab.incidents.RecipeChangeIncident
import de.unisaarland.cs.se.selab.parsers.IncidentParser
import de.unisaarland.cs.se.selab.restaurant.Restaurant
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import de.unisaarland.cs.se.selab.restaurant.Table
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * Tests incident parser for recipe change incidents.
 */
class RecipeChangeIncidentTest {
    private val parser: IncidentParser = IncidentParser()
    private val potato: Ingredient = Ingredient("Potato", MeasurementUnit.G, 1, 10)
    private val ingredients: List<Ingredient> = listOf(potato)
    private val stock: Stock = Stock(ingredients)
    private val recipes: List<Recipe> = listOf(
        Recipe(
            1,
            "Potato Soup",
            10,
            listOf(CookType.SOUS),
            mutableMapOf(potato to 1),
            RestaurantType.EUROPEAN
        )
    )
    private val restaurantStats: RestaurantStats = RestaurantStats(
        1,
        RestaurantType.EUROPEAN,
        1,
        24,
        false,
        0,
        0,
        recipes
    )
    private val staff: RestaurantStaff = RestaurantStaff(
        mutableListOf(Cook(CookType.SOUS)),
        mutableListOf(Waiter()),
        mutableListOf(Driver())
    )
    private val tables: List<Table> = listOf(Table(1, 5, TableType.COMMON))
    private val restaurants: List<Restaurant> = listOf(
        Restaurant(
            restaurantStats,
            "Restaurant",
            staff,
            tables,
            stock
        )
    )

    @Test
    fun `Recipe Change Incident - Success`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 1)
                put("type", "RECIPE")
                put("evening", 1)
                put("ingredient", "Potato")
                put("adaptation", 1)
            }
        }
        val incidents: List<Incident> = parser.parseIncidentFile(jsonArray, ingredients, stock, recipes, restaurants)
        assertEquals(1, incidents.size)
        assert(incidents.first() is RecipeChangeIncident)
    }

    @Test
    fun `Recipe Change Incident Non-Existent Ingredient`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 1)
                put("type", "RECIPE")
                put("evening", 1)
                put("ingredient", "chicken")
                put("adaptation", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredients, stock, recipes, restaurants)
        }
    }

    @Test
    fun `Recipe Change Incident Non-Existent Ingredient - Capitalization`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 1)
                put("type", "RECIPE")
                put("evening", 1)
                put("ingredient", "potato")
                put("adaptation", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredients, stock, recipes, restaurants)
        }
    }

    @Test
    fun `Recipe Change Incident Non-Existent Ingredient - Empty String`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 1)
                put("type", "RECIPE")
                put("evening", 1)
                put("ingredient", "")
                put("adaptation", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredients, stock, recipes, restaurants)
        }
    }

    @Test
    fun `Packaging Change Incident Non-Unique Ids`() {
        val jsonArray = buildJsonArray {
            addJsonObject {
                put("id", 1)
                put("type", "RECIPE")
                put("evening", 1)
                put("ingredient", "Potato")
                put("adaptation", 1)
            }
            addJsonObject {
                put("id", 1)
                put("type", "RECIPE")
                put("evening", 1)
                put("ingredient", "Potato")
                put("adaptation", 1)
            }
        }
        assertThrows<IllegalArgumentException> {
            parser.parseIncidentFile(jsonArray, ingredients, stock, recipes, restaurants)
        }
    }
}
