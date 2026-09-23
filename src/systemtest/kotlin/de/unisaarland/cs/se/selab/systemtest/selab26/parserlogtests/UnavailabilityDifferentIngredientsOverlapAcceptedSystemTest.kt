package de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension

private const val RESOURCE_DIR = "ParserControllerLogTests"
private const val RESTAURANTS_FILE = "restaurants.json"

private fun successLine(file: String) = "[INFO] Initialization Info: $file successfully parsed and validated."

/**
 * Overlapping UNAVAILABLE incidents are only a conflict when they concern the same ingredient,
 * so overlapping evenings on two different ingredients must be accepted.
 */
class UnavailabilityDifferentIngredientsOverlapAcceptedSystemTest : ExampleSystemTestExtension() {
    private val scenarioFile = "scenarioOverlappingUnavailabilityDifferentIngredients.json"
    override val food: String = "$RESOURCE_DIR/foodTwoIngredients.json"
    override val restaurants: String = "$RESOURCE_DIR/$RESTAURANTS_FILE"
    override val scenario: String = "$RESOURCE_DIR/$scenarioFile"
    override val logLevel: String = "DEBUG"
    override val maxTicks: Int = 0
    override val name = "Unavailability Different Ingredients Overlap Accepted System Test"
    override val description =
        "Overlapping UNAVAILABLE incidents on different ingredients are not a conflict and are accepted."

    override suspend fun run() {
        assertNextLine(successLine("foodTwoIngredients.json"))
        assertNextLine(successLine(RESTAURANTS_FILE))
        assertNextLine(successLine(scenarioFile))
    }
}
