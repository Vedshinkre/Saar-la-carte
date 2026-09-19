package restaurantparsertests

import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.parsers.ParserController
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

private const val FIXTURES = "src/test/kotlin/customerparsertests/fixtures/"

/**
 * Integration test: the restaurant parser builds the menu from the existing customer parser fixtures and the
 * customer parser resolves groups against those restaurants.
 */
class RestaurantAndCustomerParserIntegrationTest {
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        Logger.setup(PrintWriter(StringWriter()))
        Logger.setup(LogLevel.DEBUG)
    }

    @Test
    fun `restaurant menu and customer groups are parsed together`() {
        val scenario = tempDir.resolve("scenario.json").toFile()
        scenario.writeText(
            """{"incidents": [], "customerGroups": [
                {"id": 1, "type": "REGULAR", "size": 3, "visitingTick": 4, "foodPreferences": [],
                 "visitingStart": 1, "visitingPeriod": 2, "restaurant": 1},
                {"id": 2, "type": "CASUAL", "size": 2, "visitingTick": 5, "restaurantTypes": ["ASIAN"],
                 "visitingEvenings": [1], "ratingLikelihood": "SOME",
                 "foodPreferences": [{"size": 1, "favoriteDishes": ["Rice Bowl"]}]}
            ]}"""
        )

        val config = ParserController().parseFiles(FIXTURES + "food.json", FIXTURES + "restaurants.json", scenario.path)

        assertFalse(config.wasInvalidFile)

        val stats = config.restaurantStats.single()
        assertEquals(listOf(1, 2), stats.menu.map { it.id })
        assertEquals(RestaurantType.ASIAN, stats.menu.first { it.id == 1 }.basicDishFor)

        val (regular, casual) = config.customers
        assertIs<RegularGroup>(regular)
        assertIs<CasualGroup>(casual)
        assertEquals(listOf("Rice Bowl"), casual.foodPreferences.first().favouriteDishes)
    }
}
