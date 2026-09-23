package de.unisaarland.cs.se.selab.systemtest.selab26.abtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val MEAL_A = "mealA"

/**
 * A/B probe for whether a driver that is holding part of an order still counts as an available
 * driver for the browsing service ("whether a delivery driver is currently free in case of a
 * delivery").
 *
 * The restaurant has one waiter and one driver. Group 1 (7 people, in house) and group 2 (8 people,
 * delivery) both order in tick 5 and are cooked in one batch that finishes in tick 7. Serving the
 * in-house table first leaves the waiter 3 of their 10 SERVING actions, so order 2 is handed over
 * as 3 meals in tick 7 and the remaining 5 in tick 8.
 *
 * Group 3 is a second delivery whose order tick is 8 (visitingTick 12, distance 5), so it browses
 * exactly while the driver stands outside holding 3 of the 8 meals of order 2. Whether it is
 * offered the restaurant is the whole question.
 */
abstract class PartialDriverAvailabilityScenario : ExampleSystemTestExtension() {
    override val restaurants = "abtests/partialdriverbusy/restaurants.json"
    override val scenario = "abtests/partialdriverbusy/scenario.json"
    override val food = "abtests/partialdriverbusy/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 24

    /** the decision is logged right after the tick line, before the restaurant simulates */
    protected suspend fun skipToDecisionOfTickEight() {
        skipUntilString(TickStatusTestLogs.tickStart(DECISION_TICK, 1))
    }

    protected companion object {
        const val DECISION_TICK = 8
    }
}

/**
 * Guards the two readings below: they only say something about driver availability if the
 * hand-over really is split across ticks 7 and 8, leaving the driver partly loaded while group 3
 * browses. If this probe fails, the pair below is void whatever it reports.
 */
class PartialDriverHandoverIsSplitSystemTest : PartialDriverAvailabilityScenario() {
    override val name = "PartialDriverHandoverIsSplitSystemTest"
    override val description = "the delivery order is handed to the driver as 3 meals in tick 7 and 5 in tick 8"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(
            FohServiceTestLogs.serving(restId = 1, waitstaffId = 1, dishes = mapOf(MEAL_A to 7), tableId = 1, ticks = 2)
        )
        assertNextLine(
            FohServiceTestLogs.deliveryHandover(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf(MEAL_A to 3),
                driverId = 1,
                orderId = 2
            )
        )
        skipUntilString(TickStatusTestLogs.tickStart(DECISION_TICK, 1))
        skipUntilString(
            FohServiceTestLogs.deliveryHandover(
                restId = 1,
                waitstaffId = 1,
                dishes = mapOf(MEAL_A to 5),
                driverId = 1,
                orderId = 2
            )
        )
    }
}

/**
 * Reading A: a driver only stops being available once it drives off, so the one still collecting
 * order 2 outside the restaurant is offered to group 3. This is what we implement
 * (`FrontOfHouse.getAvailableDrivers` counts IDLE and WAITING drivers).
 */
class PartiallyLoadedDriverStillAvailableSystemTest : PartialDriverAvailabilityScenario() {
    override val name = "PartiallyLoadedDriverStillAvailableSystemTest"
    override val description = "Reading A: a driver holding part of an order still counts as free, group 3 decides"

    override suspend fun run() {
        skipToDecisionOfTickEight()
        assertNextLine(TickStatusTestLogs.restDecision(groupId = 3, restId = 1))
    }
}

/**
 * Reading B: a driver is taken as soon as it receives its first meals, so the restaurant has no
 * free driver left and group 3 finds no restaurant at all.
 */
class PartiallyLoadedDriverIsBusySystemTest : PartialDriverAvailabilityScenario() {
    override val name = "PartiallyLoadedDriverIsBusySystemTest"
    override val description = "Reading B: a driver holding part of an order is busy, group 3 finds no restaurant"

    override suspend fun run() {
        skipToDecisionOfTickEight()
        assertNextLine(TickStatusTestLogs.restNoDecision(groupId = 3))
    }
}
