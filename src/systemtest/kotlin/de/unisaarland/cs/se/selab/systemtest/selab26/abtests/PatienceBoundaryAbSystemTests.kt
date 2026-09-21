package de.unisaarland.cs.se.selab.systemtest.selab26.abtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/** A/B probe for the counting base of the 5 tick patience window */
abstract class BasePatienceAbSystemTest : ExampleSystemTestExtension() {
    override val restaurants = "abtests/basepatience/restaurants.json"
    override val scenario = "abtests/basepatience/scenario.json"
    override val food = "abtests/basepatience/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    /** asserts that group 2 walks out in [tick] and not before */
    protected suspend fun assertLeavesInTick(tick: Int) {
        skipUntilString(TickStatusTestLogs.tickStart(tick, 1))
        skipUntilString(FohServiceTestLogs.noEating(restId = 1, customers = 2, groupId = 2, tableId = 2))
        // the walk-out must not have happened before the tick under test, so the very next tick
        // start that follows it is the one after it
        skipUntilString(TickStatusTestLogs.tickStart(tick + 1, 1))
    }
}

/** reading A: the order tick does not count, the group waits through tick 5 and leaves in tick 6. */
class PatienceLeavesFiveTicksAfterOrderSystemTest : BasePatienceAbSystemTest() {
    override val name = "PatienceLeavesFiveTicksAfterOrderSystemTest"
    override val description = "Reading A: an unserved group leaves 5 ticks after the tick it ordered in"

    override suspend fun run() {
        // nothing may happen in tick 5 yet
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        assertLeavesInTick(LEAVING_TICK)
    }

    private companion object {
        const val LEAVING_TICK = 6
    }
}

/** reading B: the order tick is the first of the 5, so the group is gone one tick earlier. */
class PatienceLeavesFourTicksAfterOrderSystemTest : BasePatienceAbSystemTest() {
    override val name = "PatienceLeavesFourTicksAfterOrderSystemTest"
    override val description = "Reading B: an unserved group leaves 4 ticks after the tick it ordered in"

    override suspend fun run() {
        assertLeavesInTick(LEAVING_TICK)
    }

    private companion object {
        const val LEAVING_TICK = 5
    }
}
