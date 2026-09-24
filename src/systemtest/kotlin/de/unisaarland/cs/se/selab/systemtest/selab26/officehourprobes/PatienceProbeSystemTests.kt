package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SLOW = "Slow Bowl"
private const val BLOCKED = "Blocked Bowl"

/**
 * A/B pair (Sep 21) for how the five ticks of patience are counted. Written after the office hour,
 * where the tutors said the counting for the BallKnowledge component test is probably off by one.
 * The reference chose reading B (runs 4-12), later confirmed in forum topic 340.
 *
 * Both groups order in tick 1 and share the only cook. Group 1's Slow Bowl keeps the cook busy
 * until tick 4, so group 2's Blocked Bowl is never served. The two tests differ only in the tick in
 * which group 2 leaves:
 *  - A, [PatienceEndsFiveTicksAfterOrderingSystemTest]: the ordering tick does not count, tick 6;
 *  - B, [PatienceEndsFourTicksAfterOrderingSystemTest]: it counts as the first of the five, tick 5.
 *
 * Group 1 is served in tick 4 and is still eating in tick 5, but not in tick 6. The eating status
 * right after the "Restaurant No Eating" line therefore shows which tick that line is in.
 */
abstract class PatienceScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/patience/restaurants.json"
    override val scenario = "officehourjson/patience/scenario.json"
    override val food = "officehourjson/patience/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 12

    /** the setup both readings agree on, so a failure here is not about the counting */
    protected suspend fun assertSharedSetup() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        skipUntilString(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(SLOW to GROUP_SIZE), 1))
        skipUntilString(FohArrivalTestLogs.ordering(1, 2, 2, mapOf(BLOCKED to GROUP_SIZE), 1))
        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(SLOW to GROUP_SIZE), 1, 3))
    }

    protected companion object {
        const val GROUP_SIZE = 2
    }
}

/** Reading A (rejected by the reference, fails by design): group 2 leaves in tick 6. */
class PatienceEndsFiveTicksAfterOrderingSystemTest : PatienceScenario() {
    override val name = "PatienceEndsFiveTicksAfterOrderingSystemTest"
    override val description = "A group whose food never arrives leaves 5 ticks after its ordering tick"

    override suspend fun run() {
        assertSharedSetup()
        // nothing may be given up before tick 6, so the skip starts there
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(FohServiceTestLogs.noEating(1, GROUP_SIZE, 2, 2))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, GROUP_SIZE))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, GROUP_SIZE, 1, 1))
    }
}

/** Reading B (the reference's): the ordering tick is the first of the five, so group 2 leaves in tick 5. */
class PatienceEndsFourTicksAfterOrderingSystemTest : PatienceScenario() {
    override val name = "PatienceEndsFourTicksAfterOrderingSystemTest"
    override val description = "A group whose food never arrives leaves 4 ticks after its ordering tick"

    override suspend fun run() {
        assertSharedSetup()
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(FohServiceTestLogs.noEating(1, GROUP_SIZE, 2, 2))
        // group 1 is still eating in tick 5, which is what separates this reading from the other one
        assertNextLine(FohServiceTestLogs.eatingStatus(1, GROUP_SIZE, 0))
    }
}
