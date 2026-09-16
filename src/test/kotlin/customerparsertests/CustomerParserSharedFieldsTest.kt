package customerparsertests

import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.parsers.CustomerParser
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for the fields every customer group type shares (id, type, size, tableType,
 * visitingTick) and for the uniqueness of group ids. Rules that only the JSON schema enforces
 * are covered in [CustomerParserIntegrationTest], since these tests bypass the schema.
 */
class CustomerParserSharedFieldsTest {
    private val parser = CustomerParser()

    private val rice = Ingredient("rice", MeasurementUnit.G, bestBefore = 3, initialPackagingVolume = 1000)
    private val garlic = Ingredient("garlic", MeasurementUnit.G, bestBefore = 1, initialPackagingVolume = 50)
    private val ingredients = listOf(rice, garlic)

    private val riceBowl = Recipe(
        id = 1,
        name = "Rice Bowl",
        duration = 10,
        cookType = listOf(CookType.EXEC),
        ingredients = mutableMapOf(rice to 100),
        basicDishFor = RestaurantType.ASIAN
    )
    private val garlicSoup = Recipe(
        id = 2,
        name = "Garlic Soup",
        duration = 20,
        cookType = listOf(CookType.EXEC),
        ingredients = mutableMapOf(garlic to 10),
        basicDishFor = null
    )
    private val recipes = listOf(riceBowl, garlicSoup)

    private val restaurantStats = listOf(
        RestaurantStats(
            restaurantId = 1,
            restaurantType = RestaurantType.ASIAN,
            openingTickStart = 1,
            openingTickEnd = 24,
            event = true,
            positiveRatings = 0,
            negativeRatings = 0,
            menu = recipes
        )
    )

    // ---- JSON builders ----

    private fun casualGroup(id: Int, size: Int = 2, overrides: JsonObjectBuilder.() -> Unit = {}): JsonObject =
        buildJsonObject {
            put("id", id)
            put("type", "CASUAL")
            put("size", size)
            put("visitingTick", 5)
            putJsonArray("foodPreferences") {}
            putJsonArray("restaurantTypes") { add("ASIAN") }
            putJsonArray("visitingEvenings") { add(1) }
            put("ratingLikelihood", "SOME")
            overrides()
        }

    private fun regularGroup(id: Int): JsonObject = buildJsonObject {
        put("id", id)
        put("type", "REGULAR")
        put("size", 3)
        put("visitingTick", 4)
        putJsonArray("foodPreferences") {}
        put("visitingStart", 1)
        put("visitingPeriod", 2)
        put("restaurant", 1)
    }

    private fun eventGroup(id: Int): JsonObject = buildJsonObject {
        put("id", id)
        put("type", "EVENT")
        put("size", 6)
        put("visitingTick", 3)
        putJsonArray("foodPreferences") {}
        putJsonArray("restaurantTypes") { add("ASIAN") }
        put("eventEvening", 4)
        putJsonObject("favoriteDishes") { put("ASIAN", "Rice Bowl") }
    }

    // Uses all three preference properties on purpose, so tests only fail for the rule they target.
    private fun JsonObjectBuilder.preferencesOfSize(size: Int) {
        putJsonArray("foodPreferences") {
            addJsonObject {
                put("size", size)
                putJsonArray("excludedIngredients") { add("garlic") }
                putJsonArray("preferredIngredients") { add("rice") }
                putJsonArray("favoriteDishes") { add("Rice Bowl") }
            }
        }
    }

    private fun parse(vararg groups: JsonObject) =
        parser.parseCustomers(JsonArray(groups.toList()), recipes, ingredients, restaurantStats)

    // ---- Valid input ----

    @Test
    fun `shared fields are copied onto the parsed group`() {
        val group = parse(casualGroup(id = 7, size = 4)).single()

        assertEquals(7, group.id)
        assertEquals(4, group.size)
        assertEquals(5, group.visitingAt)
    }

    @Test
    fun `customersRemainingInRestaurant starts at the group size`() {
        val group = parse(casualGroup(id = 1, size = 9)).single()

        assertEquals(9, group.customersRemainingInRestaurant)
    }

    @Test
    fun `omitted tableType defaults to COMMON`() {
        val group = parse(casualGroup(id = 1)).single()

        assertEquals(TableType.COMMON, group.tableType)
    }

    @Test
    fun `explicit COMMON tableType is parsed`() {
        val group = parse(casualGroup(id = 1) { put("tableType", "COMMON") }).single()

        assertEquals(TableType.COMMON, group.tableType)
    }

    @Test
    fun `explicit BAR tableType is parsed`() {
        val group = parse(casualGroup(id = 1) { put("tableType", "BAR") }).single()

        assertEquals(TableType.BAR, group.tableType)
    }

    @Test
    fun `explicit SEPARATED tableType is parsed`() {
        val group = parse(casualGroup(id = 1) { put("tableType", "SEPARATED") }).single()

        assertEquals(TableType.SEPARATED, group.tableType)
    }

    @Test
    fun `lowest and highest visitingTick are accepted`() {
        val groups = parse(
            casualGroup(id = 1) { put("visitingTick", 1) },
            casualGroup(id = 2) { put("visitingTick", 21) }
        )

        assertEquals(listOf(1, 21), groups.map { it.visitingAt })
    }

    @Test
    fun `id 0 is accepted`() {
        val group = parse(casualGroup(id = 0)).single()

        assertEquals(0, group.id)
    }

    @Test
    fun `empty customer group array yields an empty list`() {
        val groups = parser.parseCustomers(buildJsonArray {}, recipes, ingredients, restaurantStats)

        assertTrue(groups.isEmpty())
    }

    @Test
    fun `each type string creates the matching subclass`() {
        val groups = parse(regularGroup(id = 1), casualGroup(id = 2), eventGroup(id = 3))

        assertIs<RegularGroup>(groups[0])
        assertIs<CasualGroup>(groups[1])
        assertIs<EventGroup>(groups[2])
    }

    @Test
    fun `groups keep the order of the input file`() {
        val groups = parse(casualGroup(id = 30), regularGroup(id = 10), eventGroup(id = 20))

        assertEquals(listOf(30, 10, 20), groups.map { it.id })
    }

    @Test
    fun `distinct ids across all types are accepted`() {
        val groups = parse(regularGroup(id = 1), casualGroup(id = 2), eventGroup(id = 3), casualGroup(id = 4))

        assertEquals(4, groups.size)
    }

    @Test
    fun `food preference list is expanded to one entry per customer`() {
        val group = parse(casualGroup(id = 1, size = 3) { preferencesOfSize(1) }).single()

        assertEquals(3, group.foodPreferences.size)
    }

    // ---- Invalid input ----

    @Test
    fun `duplicate id within the same type is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 5), casualGroup(id = 5))
        }
    }

    @Test
    fun `duplicate id across different types is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(regularGroup(id = 5), casualGroup(id = 6), eventGroup(id = 5))
        }
    }

    @Test
    fun `duplicate id that is not adjacent in the file is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 1), casualGroup(id = 2), casualGroup(id = 3), casualGroup(id = 1))
        }
    }

    @Test
    fun `unknown group type is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 1) { put("type", "VIP") })
        }
    }

    @Test
    fun `food preference sizes exceeding the group size are rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 1, size = 2) { preferencesOfSize(3) })
        }
    }
}
