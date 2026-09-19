package customerparsertests

import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.parsers.FoodPreferenceParser
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val GARLIC = "garlic"
private const val ONION = "onion"
private const val RICE = "rice"
private const val RICE_BOWL = "Rice Bowl"
private const val GARLIC_SOUP = "Garlic Soup"

/**
 * Unit tests for [FoodPreferenceParser]: validation of a single food preference, the check of
 * preference sizes against the group size, and the expansion into one preference per customer.
 *
 * Invalid cases start from a preference that uses all three properties and break exactly one
 * rule, so a test cannot pass because a different rule happened to reject the input.
 */
class FoodPreferenceParserTest {
    private val parser = FoodPreferenceParser()

    private val garlic = Ingredient(GARLIC, MeasurementUnit.G, bestBefore = 1, initialPackagingVolume = 50)
    private val onion = Ingredient(ONION, MeasurementUnit.X, bestBefore = 1, initialPackagingVolume = 10)
    private val rice = Ingredient(RICE, MeasurementUnit.G, bestBefore = 3, initialPackagingVolume = 1000)
    private val ingredients = listOf(garlic, onion, rice)

    private val recipes = listOf(
        Recipe(1, RICE_BOWL, 10, listOf(CookType.EXEC), mutableMapOf(rice to 100), RestaurantType.ASIAN),
        Recipe(2, GARLIC_SOUP, 20, listOf(CookType.EXEC), mutableMapOf(garlic to 10, onion to 1), null)
    )

    /** Builds one food preference object; passing `null` omits that property entirely. */
    private fun preference(
        size: Int = 1,
        excluded: List<String>? = listOf(ONION),
        preferred: List<String>? = listOf(RICE),
        favorites: List<String>? = listOf(RICE_BOWL)
    ): JsonObject = buildJsonObject {
        put("size", size)
        excluded?.let { put("excludedIngredients", JsonArray(it.map(::JsonPrimitive))) }
        preferred?.let { put("preferredIngredients", JsonArray(it.map(::JsonPrimitive))) }
        favorites?.let { put("favoriteDishes", JsonArray(it.map(::JsonPrimitive))) }
    }

    private fun parse(groupSize: Int, vararg preferences: JsonObject): List<FoodPreference> =
        parser.parseFoodPreferences(JsonArray(preferences.toList()), recipes, ingredients, groupSize)

    private fun FoodPreference.isEmpty(): Boolean =
        excludedIngredients.isEmpty() && preferredIngredients.isEmpty() && favouriteDishes.isEmpty()

    // ---- Expansion into one preference per customer ----

    @Test
    fun `no preferences yields one empty preference per customer`() {
        val result = parse(groupSize = 3)

        assertEquals(3, result.size)
        assertTrue(result.all { it.isEmpty() })
    }

    @Test
    fun `preference with size n appears n times`() {
        val result = parse(3, preference(size = 3))

        assertEquals(3, result.size)
        assertTrue(result.none { it.isEmpty() })
    }

    @Test
    fun `sizes summing exactly to the group size add no empty preferences`() {
        val result = parse(3, preference(size = 1), preference(size = 2, excluded = listOf(GARLIC)))

        assertEquals(3, result.size)
        assertTrue(result.none { it.isEmpty() })
    }

    @Test
    fun `customers not covered by any preference get empty preferences at the end`() {
        val result = parse(5, preference(size = 2))

        assertEquals(5, result.size)
        assertTrue(result.take(2).none { it.isEmpty() })
        assertTrue(result.drop(2).all { it.isEmpty() })
    }

    @Test
    fun `expanded preferences keep the order of the json array`() {
        val result = parse(
            5,
            preference(size = 1, excluded = listOf(GARLIC)),
            preference(size = 2, excluded = listOf(ONION))
        )

        assertEquals(
            listOf(listOf(GARLIC), listOf(ONION), listOf(ONION), emptyList(), emptyList()),
            result.map { pref -> pref.excludedIngredients.map { it.name } }
        )
    }

    // ---- Contents of a parsed preference ----

    @Test
    fun `ingredient names resolve to the parsed ingredient objects`() {
        val result = parse(1, preference(excluded = listOf(ONION, GARLIC), preferred = listOf(RICE))).single()

        assertEquals(2, result.excludedIngredients.size)
        assertSame(onion, result.excludedIngredients[0])
        assertSame(garlic, result.excludedIngredients[1])
        assertSame(rice, result.preferredIngredients.single())
    }

    @Test
    fun `favorite dishes are kept as names in json order`() {
        val result = parse(1, preference(favorites = listOf(GARLIC_SOUP))).single()

        assertEquals(listOf(GARLIC_SOUP), result.favouriteDishes)
    }

    @Test
    fun `excluding all ingredients but one is accepted`() {
        val result = parse(1, preference(excluded = listOf(GARLIC, ONION), preferred = listOf(RICE)))

        assertEquals(1, result.size)
    }

    @Test
    fun `favoring all dishes but one is accepted`() {
        val result = parse(1, preference(favorites = listOf(GARLIC_SOUP)))

        assertEquals(1, result.size)
    }

    // ---- Only some of the three properties (spec: each preference uses 1-3 of them) ----

    @Test
    fun `preference with only excludedIngredients is accepted`() {
        val result = parse(1, preference(preferred = null, favorites = null)).single()

        assertSame(onion, result.excludedIngredients.single())
        assertTrue(result.preferredIngredients.isEmpty())
        assertTrue(result.favouriteDishes.isEmpty())
    }

    @Test
    fun `preference with only preferredIngredients is accepted`() {
        val result = parse(1, preference(excluded = null, favorites = null)).single()

        assertSame(rice, result.preferredIngredients.single())
    }

    @Test
    fun `preference with only favoriteDishes is accepted`() {
        val result = parse(1, preference(excluded = null, preferred = null)).single()

        assertEquals(listOf(RICE_BOWL), result.favouriteDishes)
    }

    @Test
    fun `preference with two of the three properties is accepted`() {
        val result = parse(1, preference(favorites = null))

        assertEquals(1, result.size)
    }

    // ---- Invalid input ----

    @Test
    fun `sizes summing to more than the group size are rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(2, preference(size = 2), preference(size = 1, excluded = listOf(GARLIC)))
        }
    }

    @Test
    fun `unknown excluded ingredient is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(1, preference(excluded = listOf("saffron")))
        }
    }

    @Test
    fun `unknown preferred ingredient is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(1, preference(preferred = listOf("saffron")))
        }
    }

    @Test
    fun `unknown favorite dish is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(1, preference(favorites = listOf("Pizza")))
        }
    }

    @Test
    fun `favorite dish matched by ingredient name instead of dish name is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(1, preference(favorites = listOf(RICE)))
        }
    }

    @Test
    fun `ingredient both excluded and preferred is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(1, preference(excluded = listOf(ONION, RICE), preferred = listOf(RICE)))
        }
    }

    @Test
    fun `favoring every dish is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(1, preference(favorites = listOf(GARLIC_SOUP, RICE_BOWL)))
        }
    }

    // Excluding (or preferring) every ingredient leaves no room for the other ingredient list
    // without an intersection, so these two cases cannot use all three properties.
    @Test
    fun `excluding every ingredient is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(1, preference(excluded = listOf(GARLIC, ONION, RICE), preferred = null))
        }
    }

    @Test
    fun `preferring every ingredient is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(1, preference(excluded = null, preferred = listOf(RICE, GARLIC, ONION)))
        }
    }

    @Test
    fun `excluding every ingredient in a different order than the food file is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(1, preference(excluded = listOf(RICE, ONION, GARLIC), preferred = null))
        }
    }

    @Test
    fun `ingredient names are matched case-sensitively`() {
        assertThrows<IllegalArgumentException> {
            parse(1, preference(excluded = listOf("Onion")))
        }
    }

    @Test
    fun `favorite dish names are matched case-sensitively`() {
        assertThrows<IllegalArgumentException> {
            parse(1, preference(favorites = listOf("rice bowl")))
        }
    }

    @Test
    fun `invalid preference after a valid one is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(2, preference(), preference(preferred = listOf("saffron")))
        }
    }

    @Test
    fun `sizes are summed across preferences even if each fits on its own`() {
        assertThrows<IllegalArgumentException> {
            parse(3, preference(size = 2), preference(size = 2, excluded = listOf(GARLIC)))
        }
    }

    @Test
    fun `favoring every dish name is rejected even if a name is used by several recipes`() {
        // Dish names are only unique per restaurant, so two recipes may share a name.
        val customRiceBowl = Recipe(3, RICE_BOWL, 12, listOf(CookType.SOUS), mutableMapOf(rice to 150), null)
        val recipesWithSharedName = recipes + customRiceBowl
        val preferences = JsonArray(listOf(preference(favorites = listOf(RICE_BOWL, GARLIC_SOUP))))

        assertThrows<IllegalArgumentException> {
            parser.parseFoodPreferences(preferences, recipesWithSharedName, ingredients, 1)
        }
    }
}
