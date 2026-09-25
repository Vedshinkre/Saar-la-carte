package restaurantparsertests

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.parsers.RestaurantParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * [RestaurantParser] is called directly, so the JSON schema does not run first. The rules that only the parser
 * enforces (at least one cook across all cook types) and the values it has to reject on its own are checked here.
 */
class RestaurantParserRejectionTest {
    private val rice = Ingredient("rice", MeasurementUnit.G, bestBefore = 5, initialPackagingVolume = 20)
    private val riceBowl = Recipe(
        id = 1,
        name = "Rice Bowl",
        duration = 10,
        cookType = listOf(CookType.EXEC),
        ingredients = mutableMapOf(rice to 1),
        basicDishFor = RestaurantType.ASIAN
    )

    private fun restaurants(
        type: String = "ASIAN",
        tableType: String = "COMMON",
        execCooks: Int = 1
    ): JsonArray = Json.parseToJsonElement(
        """[{"id": 1, "name": "Bistro", "type": "$type", "openingTickStart": 1, "openingTickEnd": 24,
            "deliveryDrivers": 1, "event": false, "positiveRatings": 0, "negativeRatings": 0, "recipes": [1],
            "kitchenStaff": {"EXEC": $execCooks, "SOUS": 0, "TOURNANT": 0, "SAUCE": 0, "FISH": 0, "ROAST": 0,
                             "VEGETABLE": 0, "PASTRY": 0},
            "waitstaff": 1, "tables": [{"id": 1, "type": "$tableType", "size": 4}]}]"""
    ).jsonArray

    private fun parse(json: JsonArray) =
        RestaurantParser().parseRestaurants(json, listOf(riceBowl), Stock(listOf(rice)))

    private fun rejectionOf(json: JsonArray): String =
        assertFailsWith<IllegalArgumentException> { parse(json) }.message.orEmpty()

    @Test
    fun `the restaurant used by the rejection tests is valid`() {
        val (stats, restaurants) = parse(restaurants())

        assertEquals(listOf(RestaurantType.ASIAN), stats.map { it.restaurantType })
        assertEquals(1, restaurants.size)
    }

    @Test
    fun `a restaurant without any cook is rejected`() {
        assertEquals("Restaurant must employ at least one cook", rejectionOf(restaurants(execCooks = 0)))
    }

    @Test
    fun `a restaurant type that does not exist is rejected`() {
        assertEquals("Invalid type: MARTIAN", rejectionOf(restaurants(type = "MARTIAN")))
    }

    @Test
    fun `a table type that does not exist is rejected`() {
        assertEquals("Invalid table type: ROOFTOP", rejectionOf(restaurants(tableType = "ROOFTOP")))
    }
}
