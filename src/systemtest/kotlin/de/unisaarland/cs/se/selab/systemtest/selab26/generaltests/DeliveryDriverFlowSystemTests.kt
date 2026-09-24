package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.SystemTestSELab26
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val DIR = "deliverydriverflowjson"
private const val REST = 1
private const val SOUP = "Tomato Soup"
private const val POSITIVE = "POSITIVE"

/**
 * The life of a delivery driver, over one evening with two drivers and three delivery groups (one
 * meal each): group 1 (5 km, wants it at tick 8), group 2 (12 km, tick 10) and group 3 (5 km, tick 12).
 * Groups 1 and 2 order in tick 4 and get driver 1 and driver 2. Group 3 orders in tick 8, when driver 1
 * is back and driver 2 is still on the way home.
 */
fun deliveryDriverFlowSystemTests(): List<SystemTestSELab26> = listOf(
    DeliveryDriversAreNumberedInOrderOfTheirHandoverSystemTest(),
    DeliveryDrivingLogsCumulativeDistanceAndRemainingTicksSystemTest(),
    DeliveryReturnTripTakesAsLongAsTheOutwardTripSystemTest(),
    DeliveryDriverIsReusedOnlyAfterItReturnedSystemTest(),
    DeliveryGroupsFinishEatingTwoTicksAfterTheDeliverySystemTest(),
    DeliveryStatisticsCountDeliveredCustomersSystemTest(),
)

/** Shared restaurant, food and scenario of the driver flow tests. */
abstract class DeliveryDriverFlowSystemTest : ExampleSystemTestExtension() {
    override val restaurants = "$DIR/restaurants.json"
    override val food = "$DIR/food.json"
    override val scenario = "$DIR/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 20
}

/** Waiter 1 hands the meals of the orders 1 and 2 to the drivers 1 and 2, who then prepare their trips. */
class DeliveryDriversAreNumberedInOrderOfTheirHandoverSystemTest : DeliveryDriverFlowSystemTest() {
    override val name = "DeliveryDriversAreNumberedInOrderOfTheirHandoverSystemTest"
    override val description = "Drivers get ids 1, 2 when they receive meals and prepare in ascending group order"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        assertNextLine(TickStatusTestLogs.restDecision(1, REST))
        assertNextLine(TickStatusTestLogs.restDecision(2, REST))
        skipUntilString(FohServiceTestLogs.deliveryHandover(REST, 1, mapOf(SOUP to 1), 1, 1))
        assertNextLine(FohServiceTestLogs.deliveryHandover(REST, 1, mapOf(SOUP to 1), 2, 2))
        skipUntilString(DeliveryTestLogs.deliveryPrep(REST, 1, 1, 1, 1))
        assertNextLine(DeliveryTestLogs.deliveryPrep(REST, 2, 2, 2, 3))
    }
}

/** The driving log is cumulative: driver 2 drove 5, 10 and 12 km, and the last leg is shorter than 5 km. */
class DeliveryDrivingLogsCumulativeDistanceAndRemainingTicksSystemTest : DeliveryDriverFlowSystemTest() {
    override val name = "DeliveryDrivingLogsCumulativeDistanceAndRemainingTicksSystemTest"
    override val description = "Delivery Driving shows the km driven so far and the ticks left, arrival ends the trip"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(DeliveryTestLogs.deliveryDriving(REST, 1, 5, 0))
        assertNextLine(DeliveryTestLogs.deliveryDriving(REST, 2, 5, 2))
        assertNextLine(DeliveryTestLogs.deliveryArrival(REST, 1, 1, 1))
        assertNextLine(DeliveryTestLogs.deliveryFinished(REST, 1, 1, 1))

        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(DeliveryTestLogs.deliveryDriving(REST, 2, 10, 1))

        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(DeliveryTestLogs.deliveryDriving(REST, 2, 12, 0))
        assertNextLine(DeliveryTestLogs.deliveryArrival(REST, 2, 2, 2))
        assertNextLine(DeliveryTestLogs.deliveryFinished(REST, 2, 2, 2))
    }
}

/** Driver 1 (1 tick out) is back in tick 6, driver 2 (3 ticks out) in tick 10, no return log in between. */
class DeliveryReturnTripTakesAsLongAsTheOutwardTripSystemTest : DeliveryDriverFlowSystemTest() {
    override val name = "DeliveryReturnTripTakesAsLongAsTheOutwardTripSystemTest"
    override val description = "A driver returns after as many ticks as the outward trip took"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(DeliveryTestLogs.deliveryReturned(REST, 1))
        // driver 2 must not be back before its 3 return ticks (8, 9, 10) are over
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        skipUntilString(DeliveryTestLogs.deliveryReturned(REST, 2))
    }
}

/**
 * Group 3 orders in tick 8 and gets driver 1, which returned in tick 6. It never gets driver 2, which is
 * still on its way home. Both drivers arrive back in tick 10, logged in the order of the groups they
 * served last (group 2 before group 3), not by driver id.
 */
class DeliveryDriverIsReusedOnlyAfterItReturnedSystemTest : DeliveryDriverFlowSystemTest() {
    override val name = "DeliveryDriverIsReusedOnlyAfterItReturnedSystemTest"
    override val description = "A returned driver takes the next delivery, returning logs follow the group ids"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(8, 1))
        assertNextLine(TickStatusTestLogs.restDecision(3, REST))
        skipUntilString(FohServiceTestLogs.deliveryHandover(REST, 1, mapOf(SOUP to 1), 1, 3))
        skipUntilString(DeliveryTestLogs.deliveryPrep(REST, 1, 3, 3, 1))

        skipUntilString(TickStatusTestLogs.tickStart(9, 1))
        skipUntilString(DeliveryTestLogs.deliveryDriving(REST, 1, 5, 0))
        assertNextLine(DeliveryTestLogs.deliveryArrival(REST, 1, 3, 3))
        assertNextLine(DeliveryTestLogs.deliveryFinished(REST, 1, 3, 3))

        skipUntilString(TickStatusTestLogs.tickStart(10, 1))
        skipUntilString(DeliveryTestLogs.deliveryReturned(REST, 2))
        assertNextLine(DeliveryTestLogs.deliveryReturned(REST, 1))
    }
}

/**
 * The meals arrive in tick 5, 7 and 9, so the groups finish eating in tick 7, 9 and 11 (2 full ticks) and rate
 * positively in the same tick: the delivery came before the visiting tick.
 */
class DeliveryGroupsFinishEatingTwoTicksAfterTheDeliverySystemTest : DeliveryDriverFlowSystemTest() {
    override val name = "DeliveryGroupsFinishEatingTwoTicksAfterTheDeliverySystemTest"
    override val description = "Delivery groups finish eating 2 ticks after the delivery and rate positively"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        skipUntilString(TickStatusTestLogs.tickStart(7, 1))
        skipUntilString(DeliveryTestLogs.deliveryDriving(REST, 2, 12, 0))
        assertNextLine(DeliveryTestLogs.deliveryArrival(REST, 2, 2, 2))
        assertNextLine(DeliveryTestLogs.deliveryFinished(REST, 2, 2, 2))
        assertNextLine(DeliveryTestLogs.deliveryFinishedEating(REST, 1))
        skipUntilString(FohServiceTestLogs.rating(REST, 1, POSITIVE, 1, 0))

        skipUntilString(TickStatusTestLogs.tickStart(9, 1))
        skipUntilString(DeliveryTestLogs.deliveryFinishedEating(REST, 2))
        skipUntilString(FohServiceTestLogs.rating(REST, 2, POSITIVE, 2, 0))

        skipUntilString(TickStatusTestLogs.tickStart(11, 1))
        skipUntilString(DeliveryTestLogs.deliveryFinishedEating(REST, 3))
        skipUntilString(FohServiceTestLogs.rating(REST, 3, POSITIVE, 3, 0))
    }
}

/** Three delivered meals, three delivered customers, three ratings and no in-house customer. */
class DeliveryStatisticsCountDeliveredCustomersSystemTest : DeliveryDriverFlowSystemTest() {
    override val name = "DeliveryStatisticsCountDeliveredCustomersSystemTest"
    override val description = "The statistics count delivered customers separately from served customers"

    override suspend fun run() {
        skipUntilString(StatisticsTestLogs.statsCooked(REST, 3))
        assertNextLine(StatisticsTestLogs.statsServed(REST, 0))
        assertNextLine(StatisticsTestLogs.statsDelivered(REST, 3))
        assertNextLine(StatisticsTestLogs.statsReceived(REST, 3))
    }
}
