package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val SLOW = "Slow Bowl"
private const val BLOCKED = "Blocked Bowl"

/**
 * A/B probe for the way the five ticks of patience are counted, the rule the tutors pointed at for
 * the BallKnowledge component test ("the way the 5 ticks are being counted [...] should probably be
 * inclusive of the first tick, off by one").
 *
 * Both groups order in tick 1 on the only TOURNANT cook. Group 1's Slow Bowl occupies the cook from
 * tick 1 to tick 4, so group 2's Blocked Bowl only starts in tick 5 and is never served. The two
 * tests below differ only in the tick in which group 2 gives up, so exactly one of them can pass:
 *
 *  - [PatienceEndsFiveTicksAfterOrderingSystemTest]: the ordering tick does not count, so the group
 *    leaves once Time.tick - orderedAt reaches 5, which is tick 6. This is what we implement.
 *  - [PatienceEndsFourTicksAfterOrderingSystemTest]: the ordering tick is the first of the five, so
 *    the group leaves in tick 5.
 *
 * Group 1 was served in tick 4 and eats two full ticks, so it is still eating in tick 5 and has
 * finished in tick 6. The eating status of the tick therefore pins down which tick the "Restaurant
 * No Eating" line belongs to.
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

/** Reading A: the group leaves in tick 6, five ticks after the tick it ordered in. */
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

/** Reading B: the ordering tick counts as the first of the five, so the group leaves in tick 5. */
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
