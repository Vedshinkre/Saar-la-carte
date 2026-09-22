package de.unisaarland.cs.se.selab.systemtest.selab26.abtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A/B probe for how the "Eating" step (spec p.20f) orders its two log kinds when they land in the same
 * tick for different groups: one group finishing eating, another giving up on an unserved dish.
 *
 * Fixture shared with [de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.WaitingForFoodSystemTest]:
 * group 1 (lower id) orders tick 2, served in time, finishes eating tick 7. Groups 2 and 3 (higher ids)
 * order tick 3 for the same cook, never served, give up in tick 7 too.
 *
 * - [EatingOrderIsPerGroupSystemTest], reading A: per-group loop, so group 1's "Finished Eating" line
 *   comes before group 2/3's "No Eating" lines.
 * - [EatingOrderIsTwoPassesSystemTest], reading B: two full sweeps, so every "No Eating" line precedes
 *   every "Finished Eating" line regardless of id.
 */
abstract class EatingOrderScenario : ExampleSystemTestExtension() {
    override val restaurants = "waitingforfoodjson/restaurants.json"
    override val food = "waitingforfoodjson/food.json"
    override val scenario = "waitingforfoodjson/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    /** both readings agree the tick is 7 and which groups are involved, only the order differs */
    protected suspend fun assertSharedSetup() {
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
    }
}
