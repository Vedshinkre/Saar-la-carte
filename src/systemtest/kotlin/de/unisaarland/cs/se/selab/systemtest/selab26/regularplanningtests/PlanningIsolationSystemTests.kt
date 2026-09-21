package de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val PROBE_DIR = "recipeprobejson"
private const val PROBE_UNIT = "g"
private const val TWO_EVENINGS = 48

private fun isolation(file: String) = "$PROBE_DIR/$file"

/**
 * Every planning test of ours that asserts a procurement on an evening where a REGULAR group is
 * involved fails against the reference, while the ones without customers, and the one with an
 * EVENT group, pass. These two tests split that difference into its two halves so that the next
 * reference run says which half is wrong.
 */
abstract class PlanningIsolationSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
}

/**
 * What an order leaves in the pantry, without any REGULAR order history in the picture.
 *
 * A CASUAL group of 2 visits on evening 1 only. CASUAL groups reserve nothing, so both tables count
 * as other seats on both evenings and the estimate is ceil(32 / 10) = 4 meals of the 10 g dish.
 * Evening 1 buys 40 and the group's 2 meals take 20 of it. Evening 2 needs the same 40 and finds 20
 * in the pantry, so it buys 20. CASUAL groups keep no order history, so this is purely the estimate
 * and what the order consumed.
 *
 * A failure here means we consume or carry over ingredients differently from the reference, which
 * would explain the planning failures without the order history being at fault at all.
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
