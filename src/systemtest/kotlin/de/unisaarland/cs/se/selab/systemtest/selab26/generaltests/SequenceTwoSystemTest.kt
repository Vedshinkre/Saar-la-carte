package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * System Test for Seq 2
 **/
class SequenceTwoSystemTest : ExampleSystemTestExtension() {
    override val name = "SequenceTwoSystemTest"
    override val description = "SEQ 2"
    override val food = "SequenceTwo/food.json"
    override val restaurants = "SequenceTwo/restaurants.json"
    override val scenario = "SequenceTwo/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 1

    override suspend fun run() {
        assertNextLine(InitialAndPrepTestLogs.initSuccess("food.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("restaurants.json"))
        assertNextLine(InitialAndPrepTestLogs.initSuccess("scenario.json"))
        assertNextLine(InitialAndPrepTestLogs.SIM_START)
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))

        //  once Restaurant.prepareForEvening is implemented (blockers #1/#2): assert table
        // reservation for rc3 (size 10 -> tables 5+6 merged, per the diagram) and pantry
        // procurement logs here, before "Serving: Serving of evening 1 starts.".

        assertNextLine(TickStatusTestLogs.servingStart(1))
        assertNextLine(TickStatusTestLogs.tickStart(1, 1))

        // cc1 (EUROPEAN/AFRICAN, dine-in, size 8) should be routed to restaurant 1. FAILS TODAY:
        // actual output is "Restaurant No Decision" for group 1, per blocker #3.
        assertNextLine(TickStatusTestLogs.restDecision(1, 1))

        // cc5 (AMERICAN only) genuinely has no matching restaurant here (restaurant 1 is
        // EUROPEAN) -- this line is correct today and should keep holding once cc1's routing
        // is fixed, since it isn't a routing bug, just a real type mismatch.
        assertNextLine(TickStatusTestLogs.restNoDecision(5))

        assertNextLine(TickStatusTestLogs.restStart(1))

        // TODO once FrontOfHouse.processArrivalSeatingOrdering can actually seat cc1 (waiter w1,
        // table t2 per the diagram) and rc3 is present via its reserved table: assert seating and
        // ordering log lines here instead of the current all-zero FOH/Kitchen/serving status lines.

        assertNextLine(TickStatusTestLogs.restEnd(1))
    }
}
