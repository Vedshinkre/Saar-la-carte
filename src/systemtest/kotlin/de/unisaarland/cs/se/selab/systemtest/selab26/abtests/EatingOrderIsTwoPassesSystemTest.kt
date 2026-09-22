package de.unisaarland.cs.se.selab.systemtest.selab26.abtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs

/**
 * Reading B of [EatingOrderScenario], confirmed correct: two full sweeps over all groups, so every
 * "No Eating" line of the tick precedes every "Finished Eating" line, regardless of group id.
 */
class EatingOrderIsTwoPassesSystemTest : EatingOrderScenario() {
    override val name = "EatingOrderIsTwoPassesSystemTest"
    override val description = "Every leaving log of the tick precedes every finished-eating log of the tick"

    override suspend fun run() {
        assertSharedSetup()
        skipUntilString(FohServiceTestLogs.noEating(1, 2, 2, 2))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 2, 1, 1))
    }
}
