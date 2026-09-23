package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DIR = "driverincidentbrowsingjson"

/**
 * F18/F20/F29, for the mandatory "DriveItLikeYouMeanIt" full test ("how changing driver
 * availability influences delivery customer groups' decisions"). A STAFF/DRIVER incident with a
 * negative `number` must lower `RestaurantStats.availableDrivers` for the browsing service, not
 * just for the restaurant's own driver pool. Restaurant 1 starts with 2 drivers; a DRIVER -1
 * incident before evening 2 leaves only 1. Two delivery CASUAL groups then decide at the same
 * early tick (same visitingTick and distance): only the first (ascending id) can get the sole
 * remaining driver, the second must find no restaurant. `StayInYourLane` already covers a driver
 * incident changing which restaurant a group picks; this pins down the *count*, not the choice.
 */
class DriverIncidentReducesBrowsingAvailabilitySystemTest : ExampleSystemTestExtension() {
    override val name = "DriverIncidentReducesBrowsingAvailabilitySystemTest"
    override val description = "A DRIVER -1 incident leaves only one delivery group able to decide on the restaurant"
    override val restaurants = "$DIR/restaurants.json"
    override val food = "$DIR/food.json"
    override val scenario = "$DIR/scenario.json"
    override val logLevel = "DEBUG"

    /** the early decision tick (visitingTick 10 - ceil(5/5) - 3 = 6) in evening 2 */
    override val maxTicks = 24 + 6

    override suspend fun run() {
        // incidents are logged right after the serving of the evening before, ahead of the preparation
        skipUntilString(InitialAndPrepTestLogs.incident(1, "STAFF", 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))

        skipUntilString(TickStatusTestLogs.tickStart(6, 2))
        // group 1 gets the one remaining driver
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))
        // group 2 finds availableDrivers already back to 0 this tick
        assertNextLine(TickStatusTestLogs.restNoDecision(2))
    }
}
