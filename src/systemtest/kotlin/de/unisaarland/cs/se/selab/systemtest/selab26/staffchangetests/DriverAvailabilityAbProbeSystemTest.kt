package de.unisaarland.cs.se.selab.systemtest.selab26.staffchangetests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A/B probe, not a settled rule: it states the reading we implement so that a failure against the
 * reference implementation tells us the reference reads it the other way.
 *
 * The browsing service "store[s] how many delivery drivers are currently available" and, once a
 * group has decided, "decrements the number of delivery drivers available" (specification page 23,
 * lines 27 and 32-33). Nothing says the count goes back up when a driver has returned, so we only
 * decrement it per decision and rebuild it once per evening from the restaurant's driver count.
 *
 * The restaurant has exactly one driver. Group 1 orders on tick 2 and takes it; the driver has
 * returned by tick 5. Group 2 only decides on tick 16, long after that. Under our reading the
 * count is still 0 and group 2 finds no restaurant. If the reference refreshes the count as the
 * seats are refreshed every tick, group 2 would decide on restaurant 1 instead.
 *
 * This is the behaviour the DriveItLikeYouMeanIt full test exercises.
 */
class DriverAvailabilityAbProbeSystemTest : ExampleSystemTestExtension() {
    override val name = "DriverAvailabilityAbProbeSystemTest"
    override val description = "A/B: a returned driver does not become available to the browsing service again"
    override val food = "staffprobejson/driverFood.json"
    override val restaurants = "staffprobejson/driverRestaurants.json"
    override val scenario = "staffprobejson/driverScenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = ONE_EVENING

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.restDecision(1, 1))
        skipUntilString(DRIVER_RETURNED)

        // the driver is idle again from here on, but the browsing count stays at zero
        skipUntilString(TickStatusTestLogs.tickStart(DECISION_TICK, 1))
        skipUntilString(TickStatusTestLogs.restNoDecision(2))
    }

    private companion object {
        const val ONE_EVENING = 24
        const val DECISION_TICK = 16
        const val DRIVER_RETURNED = "[INFO] Delivery Returned (R 1): Driver 1 has returned."
    }
}
