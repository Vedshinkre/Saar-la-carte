package parsertests

import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.parsers.ParserController
import de.unisaarland.cs.se.selab.system.SimulationConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val RESTAURANT_FIXTURES =
    "src/systemtest/kotlin/de/unisaarland/cs/se/selab/systemtest/selab26/restaurantparsertests/"
private const val LEGACY_FOOD_FIXTURES = "src/test/kotlin/restaurantparsertests/"
private const val FIXTURES = "src/test/kotlin/parsertests/fixtures/"

private const val FOOD_VALID = RESTAURANT_FIXTURES + "food.json"
private const val RESTAURANTS_VALID = RESTAURANT_FIXTURES + "restaurants.json"
private const val SCENARIO_VALID = FIXTURES + "scenarioValid.json"

/**
 * Tests all logs of pareser
 */
class ParserControllerLogTest {

    private lateinit var output: StringWriter

    @BeforeEach
    fun setUp() {
        output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
    }

    private fun parse(food: String, restaurants: String, scenario: String): SimulationConfig {
        return ParserController().parseFiles(food, restaurants, scenario)
    }

    private fun loggedLines(): List<String> = output.toString().lines().filter { it.isNotBlank() }

    private fun successLine(file: String) = "[INFO] Initialization Info: $file successfully parsed and validated."

    private fun failLine(file: String) = "[IMPORTANT] Initialization Info: $file is invalid."

    @Test
    fun `all valid files produce three success logs in order`() {
        val result = parse(FOOD_VALID, RESTAURANTS_VALID, SCENARIO_VALID)

        assertEquals(
            listOf(successLine(FOOD_VALID), successLine(RESTAURANTS_VALID), successLine(SCENARIO_VALID)),
            loggedLines()
        )
        assertFalse(result.wasInvalidFile)
    }

    @Test
    fun `invalid food short-circuits before restaurants and scenario are attempted`() {
        val badFood = FIXTURES + "foodEmptyIngredients.json"
        val result = parse(badFood, RESTAURANTS_VALID, SCENARIO_VALID)

        assertEquals(listOf(failLine(badFood)), loggedLines())
        assertTrue(result.wasInvalidFile)
    }

    @Test
    fun `invalid restaurants logs food success then restaurants failure and skips scenario`() {
        val badRestaurants = RESTAURANT_FIXTURES + "restaurantsNoTableExists.json"
        val result = parse(FOOD_VALID, badRestaurants, SCENARIO_VALID)

        assertEquals(listOf(successLine(FOOD_VALID), failLine(badRestaurants)), loggedLines())
        assertTrue(result.wasInvalidFile)
    }

    @Test
    fun `invalid scenario data logs food and restaurant success then scenario failure`() {
        val badScenario = FIXTURES + "scenarioBadCustomerGroupRestaurant.json"
        val result = parse(FOOD_VALID, RESTAURANTS_VALID, badScenario)

        assertEquals(
            listOf(successLine(FOOD_VALID), successLine(RESTAURANTS_VALID), failLine(badScenario)),
            loggedLines()
        )
        assertTrue(result.wasInvalidFile)
    }

    @Test
    fun `distinct non-overlapping incidents pass crossvalidation`() {
        val scenario = FIXTURES + "scenarioValidIncidents.json"
        val result = parse(FOOD_VALID, RESTAURANTS_VALID, scenario)

        assertEquals(
            listOf(successLine(FOOD_VALID), successLine(RESTAURANTS_VALID), successLine(scenario)),
            loggedLines()
        )
        assertFalse(result.wasInvalidFile)
    }

    @Test
    fun `duplicate incident ids are rejected by crossvalidation`() {
        val scenario = FIXTURES + "scenarioDuplicateIncidentId.json"
        val result = parse(FOOD_VALID, RESTAURANTS_VALID, scenario)

        assertEquals(
            listOf(successLine(FOOD_VALID), successLine(RESTAURANTS_VALID), failLine(scenario)),
            loggedLines()
        )
        assertTrue(result.wasInvalidFile)
    }

    @Test
    fun `overlapping unavailability incidents are rejected by crossvalidation`() {
        val scenario = FIXTURES + "scenarioOverlappingUnavailability.json"
        val result = parse(FOOD_VALID, RESTAURANTS_VALID, scenario)

        assertEquals(
            listOf(successLine(FOOD_VALID), successLine(RESTAURANTS_VALID), failLine(scenario)),
            loggedLines()
        )
        assertTrue(result.wasInvalidFile)
    }

    @Test
    fun `at IMPORTANT log level the INFO success lines are suppressed`() {
        Logger.setup(LogLevel.IMPORTANT)
        val result = parse(FOOD_VALID, RESTAURANTS_VALID, SCENARIO_VALID)

        assertEquals(emptyList<String>(), loggedLines())
        assertFalse(result.wasInvalidFile)
    }

    private fun assertRestaurantFixtureFails(fileName: String) {
        val badRestaurants = RESTAURANT_FIXTURES + fileName
        val result = parse(FOOD_VALID, badRestaurants, SCENARIO_VALID)

        assertEquals(listOf(successLine(FOOD_VALID), failLine(badRestaurants)), loggedLines())
        assertTrue(result.wasInvalidFile, "$fileName should have been rejected")
    }

    @Test
    fun `restaurantsDifferentNameSameId is rejected`() {
        assertRestaurantFixtureFails("restaurantsDifferentNameSameId.json")
    }

    @Test
    fun `restaurantsSameNameDifferentId is rejected`() {
        assertRestaurantFixtureFails("restaurantsSameNameDifferentId.json")
    }

    @Test
    fun `restaurantsNotUniqueTables is rejected`() {
        assertRestaurantFixtureFails("restaurantsNotUniqueTables.json")
    }

    @Test
    fun `restaurantsNoCookExists is rejected`() {
        assertRestaurantFixtureFails("restaurantsNoCookExists.json")
    }

    @Test
    fun `restaurantsNoWaiterExists is rejected`() {
        assertRestaurantFixtureFails("restaurantsNoWaiterExists.json")
    }

    @Test
    fun `restaurantsNoTableExists is rejected`() {
        assertRestaurantFixtureFails("restaurantsNoTableExists.json")
    }

    @Test
    fun `restaurantsNoRecipieExists is rejected`() {
        assertRestaurantFixtureFails("restaurantsNoRecipieExists.json")
    }

    @Test
    fun `restaurantsNoMultipleHeadCooksieEXEC is rejected`() {
        assertRestaurantFixtureFails("restaurantsNoMultipleHeadCooksieEXEC.json")
    }

    @Test
    fun `restaurantsNoInvalidTableSize is rejected`() {
        assertRestaurantFixtureFails("restaurantsNoInvalidTableSize.json")
    }

    @Test
    fun `restaurantsNoInvalidTableSize1 is rejected`() {
        assertRestaurantFixtureFails("restaurantsNoInvalidTableSize1.json")
    }

    @Test
    fun `restaurantsOpeningTickEndLessThanOpeningTickStart is rejected`() {
        assertRestaurantFixtureFails("restaurantsOpeningTickEndLessThanOpeningTickStart.json")
    }

    @Test
    fun `restaurantsTickNotIn1to24 is rejected`() {
        assertRestaurantFixtureFails("restaurantsTickNotIn1to24.json")
    }

    @Test
    fun `restaurantsPositiveRatingsG0 is rejected`() {
        assertRestaurantFixtureFails("restaurantsPositiveRatingsG0.json")
    }

    @Test
    fun `restaurantsPositiveRatingsNeg0 is rejected`() {
        assertRestaurantFixtureFails("restaurantsPositiveRatingsNeg0.json")
    }

    @Test
    fun `restaurantsRecipiesNotExist is rejected`() {
        assertRestaurantFixtureFails("restaurantsRecipiesNotExist.json")
    }

    @Test
    fun `restaurantsNoDupliacteDishName is rejected`() {
        assertRestaurantFixtureFails("restaurantsNoDupliacteDishName.json")
    }

    @Test
    fun `restaurantsNoBasicDishOfItsType is rejected`() {
        assertRestaurantFixtureFails("restaurantsNoBasicDishOfItsType.json")
    }

    // ---- Invalid food fixtures, one per FoodParser validation branch ----

    private fun assertFoodFixtureFails(basePath: String, fileName: String) {
        val badFood = basePath + fileName
        val result = parse(badFood, RESTAURANTS_VALID, SCENARIO_VALID)

        assertEquals(listOf(failLine(badFood)), loggedLines())
        assertTrue(result.wasInvalidFile, "$fileName should have been rejected")
    }

    @Test
    fun `empty ingredients list is rejected`() {
        assertFoodFixtureFails(FIXTURES, "foodEmptyIngredients.json")
    }

    @Test
    fun `duplicate ingredient name is rejected`() {
        assertFoodFixtureFails(FIXTURES, "foodDuplicateIngredient.json")
    }

    @Test
    fun `non-positive packaging volume is rejected`() {
        assertFoodFixtureFails(FIXTURES, "foodInvalidPackagingVolume.json")
    }

    @Test
    fun `recipe duration below the minimum is rejected`() {
        assertFoodFixtureFails(FIXTURES, "foodRecipeDurationTooShort.json")
    }

    @Test
    fun `recipe referencing an unknown ingredient is rejected`() {
        assertFoodFixtureFails(FIXTURES, "foodRecipeMissingIngredientRef.json")
    }

    @Test
    fun `duplicate recipe id is rejected`() {
        assertFoodFixtureFails(FIXTURES, "foodDuplicateRecipeId.json")
    }

    @Test
    fun `legacy foodNoDuplicateDishName fixture is rejected`() {
        assertFoodFixtureFails(LEGACY_FOOD_FIXTURES, "foodNoDuplicateDishName.json")
    }

    @Test
    fun `legacy foodRecipiesNoBasicDishOfItsType fixture is rejected`() {
        assertFoodFixtureFails(LEGACY_FOOD_FIXTURES, "foodRecipiesNoBasicDishOfItsType.json")
    }

    @Test
    fun `legacy foodRecipiesNotExist fixture is rejected`() {
        assertFoodFixtureFails(LEGACY_FOOD_FIXTURES, "foodRecipiesNotExist.json")
    }

    @Test
    fun `customer group referencing an unknown restaurant is rejected`() {
        val badScenario = FIXTURES + "scenarioBadCustomerGroupRestaurant.json"
        val result = parse(FOOD_VALID, RESTAURANTS_VALID, badScenario)

        assertEquals(
            listOf(successLine(FOOD_VALID), successLine(RESTAURANTS_VALID), failLine(badScenario)),
            loggedLines()
        )
        assertTrue(result.wasInvalidFile)
    }

    @Test
    fun `staff incident referencing an unknown restaurant is rejected`() {
        val badScenario = FIXTURES + "scenarioBadIncidentRestaurant.json"
        val result = parse(FOOD_VALID, RESTAURANTS_VALID, badScenario)

        assertEquals(
            listOf(successLine(FOOD_VALID), successLine(RESTAURANTS_VALID), failLine(badScenario)),
            loggedLines()
        )
        assertTrue(result.wasInvalidFile)
    }
}
