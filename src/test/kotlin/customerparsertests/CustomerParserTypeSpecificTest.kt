package customerparsertests

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.parsers.CustomerParser
import de.unisaarland.cs.se.selab.restaurant.RestaurantStats
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the fields specific to each customer group type (REGULAR, CASUAL, EVENT) and
 * the extra validation rules [de.unisaarland.cs.se.selab.parsers.CustomerParser] performs for
 * each of them. These tests bypass the JSON schema, so they only target rules enforced by the
 * parser itself.
 */
class CustomerParserTypeSpecificTest {
    private val parser = CustomerParser()

    /**
     * Builds a string with the same `hashCode()` as [value] but that is not equal to it, by
     * shifting the weight of the last two characters (`char[n-2] * 31 + char[n-1]` is invariant
     * under `char[n-2] -= 1, char[n-1] += 31`). The parser dispatches on these string constants
     * via a compiled hashCode+equals switch, and the "same hash bucket, but equals() is false"
     * path is otherwise unreachable through any semantically distinct input string.
     */
    private fun hashCollisionOf(value: String): String {
        val chars = value.toCharArray()
        val last = chars.size - 1
        chars[last - 1] = chars[last - 1] - 1
        chars[last] = chars[last] + 31
        return String(chars)
    }

    private val rice = Ingredient("rice", MeasurementUnit.G, bestBefore = 3, initialPackagingVolume = 1000)
    private val ingredients = listOf(rice)

    private val riceBowl = Recipe(
        id = 1,
        name = "Rice Bowl",
        duration = 10,
        cookType = listOf(CookType.EXEC),
        ingredients = mutableMapOf(rice to 100),
        basicDishFor = RestaurantType.ASIAN
    )
    private val noodleBowl = Recipe(
        id = 2,
        name = "Noodle Bowl",
        duration = 10,
        cookType = listOf(CookType.EXEC),
        ingredients = mutableMapOf(rice to 50),
        basicDishFor = RestaurantType.EUROPEAN
    )
    private val plainSoup = Recipe(
        id = 3,
        name = "Plain Soup",
        duration = 5,
        cookType = listOf(CookType.EXEC),
        ingredients = mutableMapOf(rice to 10),
        basicDishFor = null
    )
    private val africanStew = Recipe(
        id = 4,
        name = "African Stew",
        duration = 15,
        cookType = listOf(CookType.EXEC),
        ingredients = mutableMapOf(rice to 20),
        basicDishFor = RestaurantType.AFRICAN
    )
    private val americanBurger = Recipe(
        id = 5,
        name = "American Burger",
        duration = 12,
        cookType = listOf(CookType.EXEC),
        ingredients = mutableMapOf(rice to 30),
        basicDishFor = RestaurantType.AMERICAN
    )
    private val recipes = listOf(riceBowl, noodleBowl, plainSoup, africanStew, americanBurger)

    private val restaurant1 = RestaurantStats(
        restaurantId = 1,
        restaurantType = RestaurantType.ASIAN,
        openingTickStart = 5,
        openingTickEnd = 15,
        event = true,
        positiveRatings = 0,
        negativeRatings = 0,
        menu = recipes
    )
    private val restaurantStats = listOf(restaurant1)

    // ---- JSON builders ----

    private fun regularGroup(
        id: Int,
        restaurantId: Int = 1,
        visitingAt: Int = 6,
        visitingStart: Int = 1,
        visitingPeriod: Int = 2,
        overrides: JsonObjectBuilder.() -> Unit = {}
    ): JsonObject = buildJsonObject {
        put("id", id)
        put("type", "REGULAR")
        put("size", 3)
        put("visitingTick", visitingAt)
        putJsonArray("foodPreferences") {}
        put("visitingStart", visitingStart)
        put("visitingPeriod", visitingPeriod)
        put("restaurant", restaurantId)
        overrides()
    }

    private fun casualGroup(
        id: Int,
        visitingAt: Int = 5,
        deliveryDistance: Int = 0,
        ratingLikelihood: String = "SOME",
        restaurantTypes: List<String> = listOf("ASIAN"),
        overrides: JsonObjectBuilder.() -> Unit = {}
    ): JsonObject = buildJsonObject {
        put("id", id)
        put("type", "CASUAL")
        put("size", 2)
        put("visitingTick", visitingAt)
        putJsonArray("foodPreferences") {}
        putJsonArray("restaurantTypes") { restaurantTypes.forEach { add(it) } }
        putJsonArray("visitingEvenings") { add(1) }
        put("ratingLikelihood", ratingLikelihood)
        if (deliveryDistance > 0) put("deliveryDistance", deliveryDistance)
        overrides()
    }

    private fun eventGroup(
        id: Int,
        restaurantTypes: List<String> = listOf("ASIAN"),
        favoriteDishes: Map<String, String> = mapOf("ASIAN" to "Rice Bowl"),
        overrides: JsonObjectBuilder.() -> Unit = {}
    ): JsonObject = buildJsonObject {
        put("id", id)
        put("type", "EVENT")
        put("size", 6)
        put("visitingTick", 3)
        putJsonArray("foodPreferences") {}
        putJsonArray("restaurantTypes") { restaurantTypes.forEach { add(it) } }
        put("eventEvening", 4)
        putJsonObject("favoriteDishes") { favoriteDishes.forEach { (type, dish) -> put(type, dish) } }
        overrides()
    }

    private fun parse(vararg groups: JsonObject) =
        parser.parseCustomers(JsonArray(groups.toList()), recipes, ingredients, restaurantStats)

    // ---- REGULAR ----

    @Test
    fun `hashCollisionOf produces a distinct string with the same hashCode`() {
        for (value in listOf("REGULAR", "CASUAL", "EVENT", "COMMON", "BAR", "SEPARATED", "SOME", "NEVER", "ALWAYS")) {
            val collision = hashCollisionOf(value)
            assertEquals(value.hashCode(), collision.hashCode())
            assertTrue(value != collision)
        }
    }

    @Test
    fun `group type colliding with REGULAR's hashCode is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(regularGroup(id = 1) { put("type", hashCollisionOf("REGULAR")) })
        }
    }

    @Test
    fun `group type colliding with CASUAL's hashCode is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 1) { put("type", hashCollisionOf("CASUAL")) })
        }
    }

    @Test
    fun `group type colliding with EVENT's hashCode is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(eventGroup(id = 1) { put("type", hashCollisionOf("EVENT")) })
        }
    }

    @Test
    fun `tableType colliding with COMMON's hashCode is rejected to the COMMON default`() {
        val group = parse(regularGroup(id = 1) { put("tableType", hashCollisionOf("COMMON")) }).single()

        assertEquals(TableType.COMMON, group.tableType)
    }

    @Test
    fun `tableType colliding with BAR's hashCode falls back to COMMON`() {
        val group = parse(regularGroup(id = 1) { put("tableType", hashCollisionOf("BAR")) }).single()

        assertEquals(TableType.COMMON, group.tableType)
    }

    @Test
    fun `tableType colliding with SEPARATED's hashCode falls back to COMMON`() {
        val group = parse(regularGroup(id = 1) { put("tableType", hashCollisionOf("SEPARATED")) }).single()

        assertEquals(TableType.COMMON, group.tableType)
    }

    @Test
    fun `ratingLikelihood colliding with SOME's hashCode is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 1, ratingLikelihood = hashCollisionOf("SOME")))
        }
    }

    @Test
    fun `ratingLikelihood colliding with NEVER's hashCode is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 1, ratingLikelihood = hashCollisionOf("NEVER")))
        }
    }

    @Test
    fun `ratingLikelihood colliding with ALWAYS's hashCode is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 1, ratingLikelihood = hashCollisionOf("ALWAYS")))
        }
    }

    @Test
    fun `lowercase group type is rejected`() {
        // The JSON schema normally rejects this before the parser ever sees it; this test
        // bypasses the schema to exercise the parser's own type-dispatch default branch.
        assertThrows<IllegalArgumentException> {
            parse(regularGroup(id = 1) { put("type", "regular") })
        }
    }

    @Test
    fun `empty group type is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(regularGroup(id = 1) { put("type", "") })
        }
    }

    @Test
    fun `explicit JSON null tableType defaults to COMMON at the parser level`() {
        val group = parse(regularGroup(id = 1) { put("tableType", JsonNull) }).single()

        assertTrue(group is RegularGroup)
        assertEquals(TableType.COMMON, group.tableType)
    }

    @Test
    fun `unrecognized tableType string defaults to COMMON at the parser level`() {
        // The JSON schema normally rejects this before the parser ever sees it; this test
        // bypasses the schema to exercise the parser's own default branch directly.
        val group = parse(regularGroup(id = 1) { put("tableType", "ROOFTOP") }).single()

        assertTrue(group is RegularGroup)
        assertEquals(TableType.COMMON, group.tableType)
    }

    @Test
    fun `REGULAR fields are parsed onto the group`() {
        val group = parse(regularGroup(id = 1, restaurantId = 1, visitingStart = 2, visitingPeriod = 3)).single()

        assertTrue(group is RegularGroup)
        assertEquals(1, group.restaurantId)
        assertEquals(2, group.visitingStart)
        assertEquals(3, group.visitingPeriod)
    }

    @Test
    fun `REGULAR visitingAt equal to the opening tick start is accepted`() {
        val group = parse(regularGroup(id = 1, visitingAt = restaurant1.openingTickStart)).single()

        assertEquals(restaurant1.openingTickStart, group.visitingAt)
    }

    @Test
    fun `REGULAR visitingAt at the buffered closing tick is accepted`() {
        val lastValidTick = restaurant1.openingTickEnd - Constants.REGULAR_VISITING_TICK_BUFFER
        val group = parse(regularGroup(id = 1, visitingAt = lastValidTick)).single()

        assertEquals(lastValidTick, group.visitingAt)
    }

    @Test
    fun `REGULAR visitingAt before opening is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(regularGroup(id = 1, visitingAt = restaurant1.openingTickStart - 1))
        }
    }

    @Test
    fun `REGULAR visitingAt inside the closing buffer is rejected`() {
        val bufferedTick = restaurant1.openingTickEnd - Constants.REGULAR_VISITING_TICK_BUFFER + 1
        assertThrows<IllegalArgumentException> {
            parse(regularGroup(id = 1, visitingAt = bufferedTick))
        }
    }

    @Test
    fun `REGULAR group is validated against the opening hours of its own restaurant`() {
        val lateRestaurant = RestaurantStats(
            restaurantId = 2,
            restaurantType = RestaurantType.ASIAN,
            openingTickStart = 10,
            openingTickEnd = 20,
            event = false,
            positiveRatings = 0,
            negativeRatings = 0,
            menu = recipes
        )
        val stats = listOf(restaurant1, lateRestaurant)
        val accepted = regularGroup(id = 1, restaurantId = 2, visitingAt = 10)
        val rejected = regularGroup(id = 2, restaurantId = 2, visitingAt = 6)

        val group = parser.parseCustomers(JsonArray(listOf(accepted)), recipes, ingredients, stats).single()

        assertEquals(2, (group as RegularGroup).restaurantId)
        assertThrows<IllegalArgumentException> {
            parser.parseCustomers(JsonArray(listOf(rejected)), recipes, ingredients, stats)
        }
    }

    @Test
    fun `REGULAR unknown restaurant id is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(regularGroup(id = 1, restaurantId = 99))
        }
    }

    // ---- CASUAL ----

    @Test
    fun `CASUAL fields are parsed onto the group`() {
        val group = parse(
            casualGroup(id = 1, restaurantTypes = listOf("ASIAN", "EUROPEAN"), ratingLikelihood = "ALWAYS") {
                putJsonArray("visitingEvenings") {
                    add(2)
                    add(4)
                }
            }
        ).single()

        assertTrue(group is CasualGroup)
        assertEquals(listOf(RestaurantType.ASIAN, RestaurantType.EUROPEAN), group.restaurantTypes)
        assertEquals(listOf(2, 4), group.visitingEvenings)
        assertEquals(RatingLikelihood.ALWAYS, group.ratingLikelihood)
    }

    @Test
    fun `CASUAL ratingLikelihood NEVER is parsed`() {
        val group = parse(casualGroup(id = 1, ratingLikelihood = "NEVER")).single() as CasualGroup

        assertEquals(RatingLikelihood.NEVER, group.ratingLikelihood)
    }

    @Test
    fun `CASUAL ratingLikelihood SOME is parsed`() {
        val group = parse(casualGroup(id = 1, ratingLikelihood = "SOME")).single() as CasualGroup

        assertEquals(RatingLikelihood.SOME, group.ratingLikelihood)
    }

    @Test
    fun `CASUAL unknown ratingLikelihood is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 1, ratingLikelihood = "MAYBE"))
        }
    }

    @Test
    fun `CASUAL lowercase ratingLikelihood is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 1, ratingLikelihood = "some"))
        }
    }

    @Test
    fun `CASUAL empty ratingLikelihood is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 1, ratingLikelihood = ""))
        }
    }

    @Test
    fun `CASUAL restaurantType AFRICAN is parsed`() {
        val group = parse(casualGroup(id = 1, restaurantTypes = listOf("AFRICAN"))).single() as CasualGroup

        assertEquals(listOf(RestaurantType.AFRICAN), group.restaurantTypes)
    }

    @Test
    fun `CASUAL restaurantType AMERICAN is parsed`() {
        val group = parse(casualGroup(id = 1, restaurantTypes = listOf("AMERICAN"))).single() as CasualGroup

        assertEquals(listOf(RestaurantType.AMERICAN), group.restaurantTypes)
    }

    @Test
    fun `CASUAL unknown restaurantType is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 1, restaurantTypes = listOf("MEXICAN")))
        }
    }

    @Test
    fun `CASUAL omitted deliveryDistance defaults to 0 and no delivery is wanted`() {
        val group = parse(casualGroup(id = 1)).single() as CasualGroup

        assertEquals(0, group.deliveryDistance)
        assertFalse(group.wantsDelivery)
    }

    @Test
    fun `CASUAL positive deliveryDistance is parsed and wants delivery`() {
        val group = parse(casualGroup(id = 1, visitingAt = 20, deliveryDistance = 25)).single() as CasualGroup

        assertEquals(25, group.deliveryDistance)
        assertTrue(group.wantsDelivery)
    }

    @Test
    fun `CASUAL deliveryDistance that leaves exactly one tick of lead time is accepted`() {
        // ceil(25 / 5) + 3 = 8, so visitingAt 9 leaves lead time of 1 tick.
        val group = parse(casualGroup(id = 1, visitingAt = 9, deliveryDistance = 25)).single() as CasualGroup

        assertEquals(25, group.deliveryDistance)
    }

    @Test
    fun `CASUAL deliveryDistance that leaves no lead time is rejected`() {
        // ceil(25 / 5) + 3 = 8, so visitingAt 8 leaves 0 lead time and must be rejected.
        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 1, visitingAt = 8, deliveryDistance = 25))
        }
    }

    @Test
    fun `CASUAL deliveryDistance that is not a multiple of the driver speed still rounds up`() {
        // ceil(21 / 5) = 5, + 3 = 8, so visitingAt 9 is the smallest accepted tick.
        val accepted = parse(casualGroup(id = 1, visitingAt = 9, deliveryDistance = 21)).single() as CasualGroup
        assertEquals(21, accepted.deliveryDistance)

        assertThrows<IllegalArgumentException> {
            parse(casualGroup(id = 2, visitingAt = 8, deliveryDistance = 21))
        }
    }

    // ---- EVENT ----

    @Test
    fun `EVENT fields are parsed onto the group`() {
        val group = parse(eventGroup(id = 1)).single()

        assertTrue(group is EventGroup)
        assertEquals(listOf(RestaurantType.ASIAN), group.restaurantTypes)
        assertEquals(4, group.eventEvening)
        assertEquals(mapOf(RestaurantType.ASIAN to "Rice Bowl"), group.eventDishes)
    }

    @Test
    fun `EVENT with multiple restaurantTypes each with a matching favorite dish is accepted`() {
        val group = parse(
            eventGroup(
                id = 1,
                restaurantTypes = listOf("ASIAN", "EUROPEAN"),
                favoriteDishes = mapOf("ASIAN" to "Rice Bowl", "EUROPEAN" to "Noodle Bowl")
            )
        ).single() as EventGroup

        assertEquals(listOf(RestaurantType.ASIAN, RestaurantType.EUROPEAN), group.restaurantTypes)
        assertEquals("Rice Bowl", group.eventDishes[RestaurantType.ASIAN])
        assertEquals("Noodle Bowl", group.eventDishes[RestaurantType.EUROPEAN])
    }

    @Test
    fun `EVENT favoriteDishes may contain extra restaurant types beyond restaurantTypes`() {
        val group = parse(
            eventGroup(
                id = 1,
                restaurantTypes = listOf("ASIAN"),
                favoriteDishes = mapOf("ASIAN" to "Rice Bowl", "EUROPEAN" to "Noodle Bowl")
            )
        ).single() as EventGroup

        assertEquals(listOf(RestaurantType.ASIAN), group.restaurantTypes)
    }

    @Test
    fun `EVENT restaurantType missing from favoriteDishes is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(
                eventGroup(
                    id = 1,
                    restaurantTypes = listOf("ASIAN", "EUROPEAN"),
                    favoriteDishes = mapOf("ASIAN" to "Rice Bowl")
                )
            )
        }
    }

    @Test
    fun `EVENT favorite dish that is not a recipe name is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(eventGroup(id = 1, favoriteDishes = mapOf("ASIAN" to "Unknown Dish")))
        }
    }

    @Test
    fun `EVENT favorite dish whose recipe has a different basicDishFor is rejected`() {
        // "Noodle Bowl" is a basic dish for EUROPEAN, not ASIAN.
        assertThrows<IllegalArgumentException> {
            parse(
                eventGroup(
                    id = 1,
                    restaurantTypes = listOf("ASIAN"),
                    favoriteDishes = mapOf("ASIAN" to "Noodle Bowl")
                )
            )
        }
    }

    @Test
    fun `EVENT favorite dish that is not a basic dish for any restaurant type is rejected`() {
        // "Plain Soup" has no basicDishFor at all.
        assertThrows<IllegalArgumentException> {
            parse(eventGroup(id = 1, favoriteDishes = mapOf("ASIAN" to "Plain Soup")))
        }
    }

    @Test
    fun `EVENT restaurantType AFRICAN with matching favorite dish is parsed`() {
        val group = parse(
            eventGroup(id = 1, restaurantTypes = listOf("AFRICAN"), favoriteDishes = mapOf("AFRICAN" to "African Stew"))
        ).single() as EventGroup

        assertEquals(listOf(RestaurantType.AFRICAN), group.restaurantTypes)
        assertEquals("African Stew", group.eventDishes[RestaurantType.AFRICAN])
    }

    @Test
    fun `EVENT restaurantType AMERICAN with matching favorite dish is parsed`() {
        val group = parse(
            eventGroup(
                id = 1,
                restaurantTypes = listOf("AMERICAN"),
                favoriteDishes = mapOf("AMERICAN" to "American Burger")
            )
        ).single() as EventGroup

        assertEquals(listOf(RestaurantType.AMERICAN), group.restaurantTypes)
        assertEquals("American Burger", group.eventDishes[RestaurantType.AMERICAN])
    }

    @Test
    fun `EVENT unknown restaurantType is rejected`() {
        assertThrows<IllegalArgumentException> {
            parse(
                eventGroup(
                    id = 1,
                    restaurantTypes = listOf("MEXICAN"),
                    favoriteDishes = mapOf("MEXICAN" to "Rice Bowl")
                )
            )
        }
    }
}
