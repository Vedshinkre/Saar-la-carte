package customerparsertests

import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.parsers.ParserController
import de.unisaarland.cs.se.selab.system.SimulationConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
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

// Food preference that uses all three properties with valid names from fixtures/food.json.
private const val FULL_PREFERENCE =
    """{"size": 2, "excludedIngredients": ["onion"], "preferredIngredients": ["rice"],
        "favoriteDishes": ["Rice Bowl"]}"""

/**
 * Integration tests for customer parsing through [ParserController]: JSON schema validation,
 * [de.unisaarland.cs.se.selab.parsers.CustomerParser] and the initialization logs working together.
 * Covers the shared customer group fields and food preferences.
 */
class CustomerParserIntegrationTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var output: StringWriter

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
    }

    // ---- Scenario builders ----

    private fun casual(id: Int, size: Int = 4, visitingTick: Int = 5, preferences: String = "", extra: String = "") =
        """{"id": $id, "type": "CASUAL", "size": $size, "visitingTick": $visitingTick,
            "restaurantTypes": ["ASIAN"], "visitingEvenings": [1], "ratingLikelihood": "SOME",
            "foodPreferences": [$preferences]$extra}"""

    private fun regular(id: Int) =
        """{"id": $id, "type": "REGULAR", "size": 3, "visitingTick": 4, "foodPreferences": [],
            "visitingStart": 1, "visitingPeriod": 2, "restaurant": 1}"""

    private fun event(id: Int) =
        """{"id": $id, "type": "EVENT", "size": 6, "visitingTick": 3, "foodPreferences": [],
            "restaurantTypes": ["ASIAN"], "eventEvening": 4, "favoriteDishes": {"ASIAN": "Rice Bowl"}}"""

    private fun preference(size: Int, properties: String) = """{"size": $size$properties}"""

    private fun writeScenario(vararg groups: String): String =
        writeRawScenario("""{"customerGroups": [${groups.joinToString(",")}], "incidents": []}""")

    private fun writeRawScenario(content: String): String {
        val file = tempDir.resolve("scenario.json").toFile()
        file.writeText(content)
        return file.path
    }

    // ---- Helpers ----

    private fun loggedLines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun successLine(file: String) = "[INFO] Initialization Info: $file successfully parsed and validated."

    private fun failLine(file: String) = "[IMPORTANT] Initialization Info: $file is invalid."

    private fun assertScenarioValid(vararg groups: String): SimulationConfig {
        val scenario = writeScenario(*groups)
        val config = ParserController().parseFiles(FOOD, RESTAURANTS, scenario)

        assertEquals(listOf(successLine(FOOD), successLine(RESTAURANTS), successLine(scenario)), loggedLines())
        assertFalse(config.wasInvalidFile)
        return config
    }

    private fun assertScenarioInvalid(vararg groups: String) = assertScenarioFileInvalid(writeScenario(*groups))

    private fun assertScenarioFileInvalid(scenario: String) {
        val config = ParserController().parseFiles(FOOD, RESTAURANTS, scenario)

        assertEquals(listOf(successLine(FOOD), successLine(RESTAURANTS), failLine(scenario)), loggedLines())
        assertTrue(config.wasInvalidFile)
        assertTrue(config.customers.isEmpty())
    }

    // ---- Valid scenarios ----

    @Test
    fun `mixed scenario ends up in the simulation config`() {
        val config = assertScenarioValid(
            regular(1),
            casual(2, preferences = FULL_PREFERENCE, extra = """, "tableType": "BAR""""),
            event(3)
        )

        val customers = config.customers
        assertEquals(listOf(1, 2, 3), customers.map { it.id })
        assertIs<RegularGroup>(customers[0])
        assertIs<CasualGroup>(customers[1])
        assertIs<EventGroup>(customers[2])
        assertEquals(TableType.COMMON, customers[0].tableType)
        assertEquals(TableType.BAR, customers[1].tableType)
    }

    @Test
    fun `parsed food preferences reference the ingredients from the food file`() {
        val config = assertScenarioValid(casual(1, size = 4, preferences = FULL_PREFERENCE))

        val preferences = config.customers.single().foodPreferences
        assertEquals(4, preferences.size)
        assertEquals(listOf("onion"), preferences[0].excludedIngredients.map { it.name })
        assertTrue(preferences[0].excludedIngredients.single() in config.ingredients)
        assertTrue(preferences[3].excludedIngredients.isEmpty())
    }

    @Test
    fun `empty customer group list is valid`() {
        val config = assertScenarioValid()

        assertTrue(config.customers.isEmpty())
    }

    @Test
    fun `visitingTick 1 and 21 are valid`() {
        val config = assertScenarioValid(casual(1, visitingTick = 1), casual(2, visitingTick = 21))

        assertEquals(listOf(1, 21), config.customers.map { it.visitingAt })
    }

    @Test
    fun `preference sizes summing exactly to the group size are valid`() {
        assertScenarioValid(casual(1, size = 2, preferences = FULL_PREFERENCE))
    }

    @Test
    fun `preference with only excludedIngredients is valid`() {
        assertScenarioValid(casual(1, preferences = preference(1, """, "excludedIngredients": ["onion"]""")))
    }

    @Test
    fun `preference with only preferredIngredients is valid`() {
        assertScenarioValid(casual(1, preferences = preference(1, """, "preferredIngredients": ["rice"]""")))
    }

    @Test
    fun `preference with only favoriteDishes is valid`() {
        assertScenarioValid(casual(1, preferences = preference(1, """, "favoriteDishes": ["Garlic Soup"]""")))
    }

    // ---- Invalid shared fields ----

    @Test
    fun `duplicate group id across types is invalid`() {
        assertScenarioInvalid(regular(7), casual(8), event(7))
    }

    @Test
    fun `visitingTick 0 is invalid`() {
        assertScenarioInvalid(casual(1, visitingTick = 0))
    }

    @Test
    fun `visitingTick 22 is invalid`() {
        assertScenarioInvalid(casual(1, visitingTick = 22))
    }

    @Test
    fun `negative group id is invalid`() {
        assertScenarioInvalid(casual(-1))
    }

    @Test
    fun `unknown group type is invalid`() {
        assertScenarioInvalid("""{"id": 1, "type": "VIP", "size": 2, "visitingTick": 5, "foodPreferences": []}""")
    }

    @Test
    fun `unknown table type is invalid`() {
        assertScenarioInvalid(casual(1, extra = """, "tableType": "ROOFTOP""""))
    }

    @Test
    fun `missing visitingTick is invalid`() {
        assertScenarioInvalid(
            """{"id": 1, "type": "REGULAR", "size": 3, "foodPreferences": [],
                "visitingStart": 1, "visitingPeriod": 2, "restaurant": 1}"""
        )
    }

    @Test
    fun `missing foodPreferences is invalid`() {
        assertScenarioInvalid(
            """{"id": 1, "type": "REGULAR", "size": 3, "visitingTick": 4,
                "visitingStart": 1, "visitingPeriod": 2, "restaurant": 1}"""
        )
    }

    // ---- Invalid food preferences ----

    @Test
    fun `preference sizes exceeding the group size are invalid`() {
        assertScenarioInvalid(casual(1, size = 1, preferences = FULL_PREFERENCE))
    }

    @Test
    fun `preference of size 0 is invalid`() {
        assertScenarioInvalid(casual(1, preferences = preference(0, """, "preferredIngredients": ["rice"]""")))
    }

    @Test
    fun `preference without any of the three properties is invalid`() {
        assertScenarioInvalid(casual(1, preferences = preference(1, "")))
    }

    @Test
    fun `preference with an unknown ingredient is invalid`() {
        val properties = """, "excludedIngredients": ["saffron"], "preferredIngredients": ["rice"],
            "favoriteDishes": ["Rice Bowl"]"""
        assertScenarioInvalid(casual(1, preferences = preference(1, properties)))
    }

    @Test
    fun `preference with an unknown favorite dish is invalid`() {
        val properties = """, "excludedIngredients": ["onion"], "preferredIngredients": ["rice"],
            "favoriteDishes": ["Pizza"]"""
        assertScenarioInvalid(casual(1, preferences = preference(1, properties)))
    }

    @Test
    fun `ingredient both excluded and preferred is invalid`() {
        val properties = """, "excludedIngredients": ["onion", "rice"], "preferredIngredients": ["rice"],
            "favoriteDishes": ["Rice Bowl"]"""
        assertScenarioInvalid(casual(1, preferences = preference(1, properties)))
    }

    @Test
    fun `favoring every dish is invalid`() {
        val properties = """, "excludedIngredients": ["onion"], "preferredIngredients": ["rice"],
            "favoriteDishes": ["Rice Bowl", "Garlic Soup", "Onion Salad"]"""
        assertScenarioInvalid(casual(1, preferences = preference(1, properties)))
    }

    @Test
    fun `duplicate entry in a preference list is invalid`() {
        val properties = """, "excludedIngredients": ["onion", "onion"], "preferredIngredients": ["rice"],
            "favoriteDishes": ["Rice Bowl"]"""
        assertScenarioInvalid(casual(1, preferences = preference(1, properties)))
    }

    @Test
    fun `excluding every ingredient is invalid`() {
        val properties = """, "excludedIngredients": ["rice", "garlic", "onion"], "favoriteDishes": ["Rice Bowl"]"""
        assertScenarioInvalid(casual(1, preferences = preference(1, properties)))
    }

    @Test
    fun `preferring every ingredient is invalid`() {
        val properties = """, "preferredIngredients": ["onion", "rice", "garlic"], "favoriteDishes": ["Rice Bowl"]"""
        assertScenarioInvalid(casual(1, preferences = preference(1, properties)))
    }

    @Test
    fun `ingredient name with different capitalisation is invalid`() {
        assertScenarioInvalid(casual(1, preferences = preference(1, """, "excludedIngredients": ["Onion"]""")))
    }

    @Test
    fun `explicitly empty preference list is invalid`() {
        val properties = """, "excludedIngredients": [], "preferredIngredients": ["rice"]"""
        assertScenarioInvalid(casual(1, preferences = preference(1, properties)))
    }

    @Test
    fun `unknown property in a preference is invalid`() {
        val properties = """, "preferredIngredients": ["rice"], "dislikedDishes": ["Garlic Soup"]"""
        assertScenarioInvalid(casual(1, preferences = preference(1, properties)))
    }

    @Test
    fun `preference without size is invalid`() {
        assertScenarioInvalid(casual(1, preferences = """{"preferredIngredients": ["rice"]}"""))
    }

    @Test
    fun `negative preference size is invalid`() {
        assertScenarioInvalid(casual(1, preferences = preference(-1, """, "preferredIngredients": ["rice"]""")))
    }

    @Test
    fun `invalid preference in a later group is invalid`() {
        val badPreference = preference(1, """, "favoriteDishes": ["Pizza"]""")
        assertScenarioInvalid(casual(1, preferences = FULL_PREFERENCE), casual(2, preferences = badPreference))
    }

    // ---- More invalid shared fields ----

    @Test
    fun `lowercase table type is invalid`() {
        assertScenarioInvalid(casual(1, extra = """, "tableType": "bar""""))
    }

    @Test
    fun `group id given as a string is invalid`() {
        assertScenarioInvalid(
            """{"id": "1", "type": "CASUAL", "size": 4, "visitingTick": 5, "restaurantTypes": ["ASIAN"],
                "visitingEvenings": [1], "ratingLikelihood": "SOME", "foodPreferences": []}"""
        )
    }

    @Test
    fun `non-integer visitingTick is invalid`() {
        assertScenarioInvalid(
            """{"id": 1, "type": "CASUAL", "size": 4, "visitingTick": 5.5, "restaurantTypes": ["ASIAN"],
                "visitingEvenings": [1], "ratingLikelihood": "SOME", "foodPreferences": []}"""
        )
    }

    @Test
    fun `missing size is invalid`() {
        assertScenarioInvalid(
            """{"id": 1, "type": "CASUAL", "visitingTick": 5, "restaurantTypes": ["ASIAN"],
                "visitingEvenings": [1], "ratingLikelihood": "SOME", "foodPreferences": []}"""
        )
    }

    @Test
    fun `missing type is invalid`() {
        assertScenarioInvalid("""{"id": 1, "size": 4, "visitingTick": 5, "foodPreferences": []}""")
    }

    @Test
    fun `scenario without customerGroups is invalid`() {
        assertScenarioFileInvalid(writeRawScenario("""{"incidents": []}"""))
    }

    // ---- More valid scenarios ----

    @Test
    fun `favorite dish that is not on any restaurant menu is valid`() {
        // "Onion Salad" exists in the food file but no restaurant lists it.
        val onlyOnionSalad = preference(1, """, "favoriteDishes": ["Onion Salad"]""")
        val config = assertScenarioValid(casual(1, preferences = onlyOnionSalad))

        assertEquals(listOf("Onion Salad"), config.customers.single().foodPreferences.first().favouriteDishes)
    }

    @Test
    fun `favoring every dish on the menu is valid when other recipes exist`() {
        val properties = """, "favoriteDishes": ["Rice Bowl", "Garlic Soup"]"""
        assertScenarioValid(casual(1, preferences = preference(1, properties)))
    }

    @Test
    fun `preferences on REGULAR and EVENT groups are parsed`() {
        val regularWithPreference =
            """{"id": 1, "type": "REGULAR", "size": 3, "visitingTick": 4, "foodPreferences": [$FULL_PREFERENCE],
                "visitingStart": 1, "visitingPeriod": 2, "restaurant": 1}"""
        val eventWithPreference =
            """{"id": 2, "type": "EVENT", "size": 6, "visitingTick": 3, "foodPreferences": [$FULL_PREFERENCE],
                "restaurantTypes": ["ASIAN"], "eventEvening": 4, "favoriteDishes": {"ASIAN": "Rice Bowl"}}"""
        val config = assertScenarioValid(regularWithPreference, eventWithPreference)

        assertEquals(listOf(3, 6), config.customers.map { it.foodPreferences.size })
        for (group in config.customers) {
            assertTrue(group.foodPreferences.take(2).all { it.favouriteDishes.isNotEmpty() })
        }
    }

    @Test
    fun `group of size 1 with a preference of size 1 is valid`() {
        val onlyGarlic = preference(1, """, "preferredIngredients": ["garlic"]""")
        assertScenarioValid(casual(1, size = 1, preferences = onlyGarlic))
    }

    @Test
    fun `multiple preferences in one group are valid`() {
        val second = preference(1, """, "excludedIngredients": ["garlic"]""")
        val config = assertScenarioValid(casual(1, size = 4, preferences = "$FULL_PREFERENCE, $second"))

        assertEquals(
            listOf(listOf("onion"), listOf("onion"), listOf("garlic"), emptyList()),
            config.customers.single().foodPreferences.map { pref -> pref.excludedIngredients.map { it.name } }
        )
    }
}
