package restaurantparsertests

import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.parsers.ParserController
import de.unisaarland.cs.se.selab.system.SimulationConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val FIXTURES = "src/test/kotlin/customerparsertests/fixtures/"
private const val FOOD = FIXTURES + "food.json"
private const val RESTAURANTS = FIXTURES + "restaurants.json"

private const val REGULAR_AT_TICK_4 = """{"id": 1, "type": "REGULAR", "size": 3, "visitingTick": 4,
    "foodPreferences": [], "visitingStart": 1, "visitingPeriod": 2, "restaurant": 1}"""

/**
 * Integration test for the dependency between the restaurant parser and the customer parser: customer groups are
 * validated against the restaurants and recipes that were parsed from the other two files. The customer fields that
 * do not depend on those files are covered by `CustomerParserIntegrationTest`.
 */
class RestaurantAndCustomerParserIntegrationTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var output: StringWriter

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
    }

    private fun write(name: String, content: String): String {
        val file = tempDir.resolve(name).toFile()
        file.writeText(content)
        return file.path
    }

    private fun scenario(vararg groups: String) =
        write("scenario.json", """{"incidents": [], "customerGroups": [${groups.joinToString(",")}]}""")

    /** The shared restaurant fixture, opening from tick [start] to tick [end] instead of 1 to 24. */
    private fun restaurantsOpen(start: Int, end: Int): String = write(
        "restaurants.json",
        File(RESTAURANTS).readText()
            .replace("\"openingTickStart\": 1", "\"openingTickStart\": $start")
            .replace("\"openingTickEnd\": 24", "\"openingTickEnd\": $end")
    )

    private fun parse(restaurants: String, scenario: String): SimulationConfig =
        ParserController().parseFiles(FOOD, restaurants, scenario)

    private fun loggedLines() = output.toString().lines().filter { it.isNotBlank() }

    private fun success(file: String) = "[INFO] Initialization Info: $file successfully parsed and validated."

    private fun failure(file: String) = "[IMPORTANT] Initialization Info: $file is invalid."

    private fun assertScenarioRejected(config: SimulationConfig, restaurants: String, scenario: String) {
        assertEquals(listOf(success(FOOD), success(restaurants), failure(scenario)), loggedLines())
        assertTrue(config.wasInvalidFile)
        assertTrue(config.customers.isEmpty())
    }

    @Test
    fun `customer groups are resolved against the parsed restaurants and recipes`() {
        val scenario = scenario(
            REGULAR_AT_TICK_4,
            """{"id": 2, "type": "CASUAL", "size": 2, "visitingTick": 5, "restaurantTypes": ["ASIAN"],
                "visitingEvenings": [1], "ratingLikelihood": "SOME",
                "foodPreferences": [{"size": 1, "favoriteDishes": ["Rice Bowl"]}]}""",
            """{"id": 3, "type": "EVENT", "size": 6, "visitingTick": 3, "foodPreferences": [],
                "restaurantTypes": ["ASIAN"], "eventEvening": 4, "favoriteDishes": {"ASIAN": "Rice Bowl"}}"""
        )

        val config = parse(RESTAURANTS, scenario)

        assertEquals(listOf(success(FOOD), success(RESTAURANTS), success(scenario)), loggedLines())
        assertFalse(config.wasInvalidFile)
        val stats = config.restaurantStats.single()
        assertEquals(RestaurantType.ASIAN, stats.menu.first { it.id == 1 }.basicDishFor)
        val (regular, casual, event) = config.customers
        assertIs<RegularGroup>(regular)
        assertEquals(stats.restaurantId, regular.restaurantId)
        assertIs<CasualGroup>(casual)
        assertEquals(listOf("Rice Bowl"), casual.foodPreferences.first().favouriteDishes)
        assertIs<EventGroup>(event)
        assertEquals(mapOf(RestaurantType.ASIAN to "Rice Bowl"), event.eventDishes)
    }

    @Test
    fun `regular group of a restaurant that is not in the restaurants file is rejected`() {
        val scenario = scenario(REGULAR_AT_TICK_4.replace("\"restaurant\": 1", "\"restaurant\": 7"))

        val config = parse(RESTAURANTS, scenario)

        assertScenarioRejected(config, RESTAURANTS, scenario)
    }

    @Test
    fun `regular group visiting inside the opening hours of the parsed restaurant is accepted`() {
        val restaurants = restaurantsOpen(start = 4, end = 10)
        val scenario = scenario(REGULAR_AT_TICK_4)

        val config = parse(restaurants, scenario)

        assertEquals(listOf(success(FOOD), success(restaurants), success(scenario)), loggedLines())
        assertEquals(4, config.customers.single().visitingAt)
    }

    @Test
    fun `regular group visiting before the parsed restaurant opens is rejected`() {
        val restaurants = restaurantsOpen(start = 5, end = 20)
        val scenario = scenario(REGULAR_AT_TICK_4)

        val config = parse(restaurants, scenario)

        assertScenarioRejected(config, restaurants, scenario)
    }

    @Test
    fun `regular group visiting too close to the closing tick of the parsed restaurant is rejected`() {
        val restaurants = restaurantsOpen(start = 1, end = 6)
        val scenario = scenario(REGULAR_AT_TICK_4)

        val config = parse(restaurants, scenario)

        assertScenarioRejected(config, restaurants, scenario)
    }

    @Test
    fun `event favorite dish that is not the basic dish of its restaurant type is rejected`() {
        // "Garlic Soup" is a recipe of the food file, but not the ASIAN basic dish
        val scenario = scenario(
            """{"id": 3, "type": "EVENT", "size": 6, "visitingTick": 3, "foodPreferences": [],
                "restaurantTypes": ["ASIAN"], "eventEvening": 4, "favoriteDishes": {"ASIAN": "Garlic Soup"}}"""
        )

        val config = parse(RESTAURANTS, scenario)

        assertScenarioRejected(config, RESTAURANTS, scenario)
    }

    @Test
    fun `customer groups are not parsed when the restaurants file is invalid`() {
        val restaurants = write("restaurants.json", """{"restaurants": "none"}""")
        val scenario = scenario(REGULAR_AT_TICK_4)

        val config = parse(restaurants, scenario)

        assertEquals(listOf(success(FOOD), failure(restaurants)), loggedLines())
        assertTrue(config.wasInvalidFile)
        assertTrue(config.customers.isEmpty())
    }
}
