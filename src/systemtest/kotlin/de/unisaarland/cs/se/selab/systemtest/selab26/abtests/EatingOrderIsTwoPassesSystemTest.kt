package de.unisaarland.cs.se.selab.systemtest.selab26.abtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs

/**
 * Reading B of [EatingOrderScenario]: every group's "No Eating" line of the tick comes before every
 * "Finished Eating" line of the tick, because the leaving-pass and the finished-eating-pass are each a
 * separate sweep over all groups, regardless of which group has the lower id.
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
