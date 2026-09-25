package de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val PROBE_DIR = "recipeprobejson"
private const val PROBE_UNIT = "g"
private const val TWO_EVENINGS = 48

private fun isolation(file: String) = "$PROBE_DIR/$file"

/**
 * Written on Sep 21, when every planning test with a REGULAR group failed on the reference while
 * the ones without customers passed. Two causes were possible: what an order takes out of the
 * pantry, or how a REGULAR reservation changes the estimate. This test isolates the first one.
 * It passed on the reference from run 2 onward, so the difference had to be in the estimate. The
 * follow-up tests are in ReservedSeatsHypothesisSystemTests.kt.
 */
abstract class PlanningIsolationSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
}

/**
 * What an order leaves in the pantry, with no REGULAR group and no order history involved.
 *
 * A CASUAL group of 2 visits on evening 1 only. CASUAL groups reserve nothing, so all 32 seats
 * count on both evenings: ceil(32 / 10) = 4 meals of the 10 g dish. Evening 1 buys 40 g and the
 * group's 2 meals use 20 g. Evening 2 needs the same 40 g, finds 20 g, and buys 20 g.
 */
class CasualConsumptionCarryOverSystemTest : PlanningIsolationSystemTest() {
    override val name = "CasualConsumptionCarryOverSystemTest"
    override val description = "What a CASUAL order leaves in the pantry for the next evening"
    override val food = isolation("casualFood.json")
    override val restaurants = isolation("casualRestaurants.json")
    override val scenario = isolation("casualScenario.json")
    override val maxTicks = TWO_EVENINGS

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, FIRST_EVENING, PROBE_UNIT, SAGO))

        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SECOND_EVENING, PROBE_UNIT, SAGO))
    }

    private companion object {
        const val SAGO = "sago"

        /** 4 meals estimated for the 32 seats, none of them reserved */
        const val FIRST_EVENING = 40

        /** the same 40 required, minus the 20 the group's two meals took */
        const val SECOND_EVENING = 20
    }
}
