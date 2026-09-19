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
import kotlin.test.assertNull

/** Unit tests for how [RestaurantParser] builds a restaurant's menu from recipes and basic dishes. */
class RestaurantParserBasicDishMenuTest {
    private val rice = Ingredient("rice", MeasurementUnit.G, 3, 1000)
    private val stock = Stock(listOf(rice))

    private fun recipe(id: Int, name: String, basicDishFor: RestaurantType? = null) =
        Recipe(id, name, 10, listOf(CookType.EXEC), mutableMapOf(rice to 100), basicDishFor)

    private fun restaurant(id: Int, type: RestaurantType, recipeIds: List<Int>) =
        """{"id": $id, "name": "R$id", "type": "${type.name}", "openingTickStart": 1, "openingTickEnd": 24,
            "deliveryDrivers": 0, "event": false, "positiveRatings": 0, "negativeRatings": 0,
            "recipes": $recipeIds,
            "kitchenStaff": {"EXEC": 1, "SOUS": 0, "TOURNANT": 0, "SAUCE": 0, "FISH": 0, "ROAST": 0,
                "VEGETABLE": 0, "PASTRY": 0},
            "waitstaff": 1, "tables": [{"id": 1, "type": "COMMON", "size": 4}]}"""

    private fun restaurants(vararg entries: String): JsonArray =
        Json.parseToJsonElement("[${entries.joinToString(",")}]").jsonArray

    @Test
    fun `adapted recipe with the name of a basic dish keeps basic status on the menu`() {
        val recipes = listOf(
            recipe(1, "Rice Bowl", RestaurantType.ASIAN),
            recipe(2, "Rice Bowl")
        )

        val (stats, _) = RestaurantParser().parseRestaurants(
            restaurants(restaurant(1, RestaurantType.ASIAN, listOf(2))),
            recipes,
            stock
        )

        val menu = stats.single().menu
        assertEquals(listOf(2), menu.map { it.id })
        assertEquals(RestaurantType.ASIAN, menu.single().basicDishFor)
    }

    @Test
    fun `non basic recipe with another name does not become a basic dish`() {
        val recipes = listOf(
            recipe(1, "Rice Bowl", RestaurantType.ASIAN),
            recipe(2, "Fried Rice")
        )

        val (stats, _) = RestaurantParser().parseRestaurants(
            restaurants(restaurant(1, RestaurantType.ASIAN, listOf(2))),
            recipes,
            stock
        )

        val menu = stats.single().menu
        assertEquals(listOf(1, 2), menu.map { it.id })
        assertNull(menu.first { it.id == 2 }.basicDishFor)
        assertEquals(RestaurantType.ASIAN, menu.first { it.id == 1 }.basicDishFor)
    }

    @Test
    fun `basic dish of another restaurant type does not make a same named recipe basic`() {
        val recipes = listOf(
            recipe(1, "Rice Bowl", RestaurantType.ASIAN),
            recipe(2, "Rice Bowl"),
            recipe(3, "Stew", RestaurantType.EUROPEAN)
        )

        val (stats, _) = RestaurantParser().parseRestaurants(
            restaurants(
                restaurant(1, RestaurantType.EUROPEAN, listOf(2)),
                restaurant(2, RestaurantType.ASIAN, listOf(1))
            ),
            recipes,
            stock
        )

        val european = stats.first { it.restaurantId == 1 }.menu
        assertEquals(listOf(2, 3), european.map { it.id })
        assertNull(european.first { it.id == 2 }.basicDishFor)
    }

    @Test
    fun `default basic dish is not added when the restaurant lists it itself`() {
        val recipes = listOf(recipe(1, "Rice Bowl", RestaurantType.ASIAN))

        val (stats, _) = RestaurantParser().parseRestaurants(
            restaurants(restaurant(1, RestaurantType.ASIAN, listOf(1))),
            recipes,
            stock
        )

        assertEquals(listOf(1), stats.single().menu.map { it.id })
    }

    @Test
    fun `default basic dish is not added when an adapted recipe replaces it`() {
        val recipes = listOf(
            recipe(1, "Rice Bowl", RestaurantType.ASIAN),
            recipe(2, "Rice Bowl"),
            recipe(3, "Noodles", RestaurantType.ASIAN)
        )

        val (stats, _) = RestaurantParser().parseRestaurants(
            restaurants(restaurant(1, RestaurantType.ASIAN, listOf(2))),
            recipes,
            stock
        )

        // Rice Bowl is replaced by recipe 2, Noodles is still added as a default basic dish
        assertEquals(listOf(2, 3), stats.single().menu.map { it.id })
    }

    @Test
    fun `menu is sorted by recipe id`() {
        val recipes = listOf(
            recipe(1, "Noodles", RestaurantType.ASIAN),
            recipe(2, "Soup"),
            recipe(3, "Curry")
        )

        val (stats, _) = RestaurantParser().parseRestaurants(
            restaurants(restaurant(1, RestaurantType.ASIAN, listOf(3, 2))),
            recipes,
            stock
        )

        assertEquals(listOf(1, 2, 3), stats.single().menu.map { it.id })
    }
}
