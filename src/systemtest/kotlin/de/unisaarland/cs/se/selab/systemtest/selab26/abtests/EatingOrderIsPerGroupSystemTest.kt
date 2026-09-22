package de.unisaarland.cs.se.selab.systemtest.selab26.abtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs

/**
 * Reading A of [EatingOrderScenario], confirmed wrong: per-group processing, so group 1's "Finished
 * Eating" line comes before group 2's "No Eating" line. Kept as a negative control.
 */
class EatingOrderIsPerGroupSystemTest : EatingOrderScenario() {
    override val name = "EatingOrderIsPerGroupSystemTest"
    override val description = "The lower-id group's finished-eating log precedes the higher-id group's leaving log"

    override suspend fun run() {
        assertSharedSetup()
        skipUntilString(FohServiceTestLogs.finishedEating(1, 2, 1, 1))
        skipUntilString(FohServiceTestLogs.noEating(1, 2, 2, 2))
    }
}
