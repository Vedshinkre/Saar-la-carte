package de.unisaarland.cs.se.selab.systemtest.selab26.abtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A/B probe for how the "Eating" step (specification p.20f) orders its two kinds of logs when they
 * both apply to different groups in the same tick: a group finishing eating, and a different group
 * giving up on an unserved dish.
 *
 * The spec presents them as two back-to-back paragraphs: "Customers who weren't served for too long
 * now leave the restaurant. This is logged in ascending order of [group type and id]... Customers
 * start eating immediately after being served. Once they have finished eating, this will be logged.
 * First, it is logged for every group..." That phrasing could mean two separate full passes over all
 * groups (leaving-pass, then finished-eating-pass), or it could just be introducing each kind of log
 * in turn while the implementation still handles one group fully before moving to the next.
 *
 * The scenario (fixture shared with [de.unisaarland.cs.se.selab.systemtest.selab26.generaltests
 * .WaitingForFoodSystemTest], which asserts the same tick 7 under whichever reading we implement):
 * group 1 (lower id) orders in tick 2 and is served in time, so it finishes eating in tick 7. Groups 2
 * and 3 (higher ids) order in tick 3 for the same single ROAST cook, which is still busy with group
 * 1's order, so they never get served and give up four ticks after ordering, also in tick 7.
 *
 * - [EatingOrderIsPerGroupSystemTest], reading A: the implementation loops over groups in ascending
 *   id and, for each group, checks both "did they just finish eating" and "are they giving up" before
 *   moving to the next group. Since group 1 has the lowest id, its "Finished Eating" line comes out
 *   before group 2 and 3's "No Eating" lines. This is what we implement.
 * - [EatingOrderIsTwoPassesSystemTest], reading B: the leaving-pass and the finished-eating-pass are
 *   each a separate sweep over every group, so every "No Eating" line of the tick precedes every
 *   "Finished Eating" line of the tick, regardless of which group has the lower id.
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
