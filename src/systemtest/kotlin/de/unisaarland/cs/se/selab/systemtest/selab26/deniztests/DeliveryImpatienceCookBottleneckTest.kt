package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val STEAK = "steak"
private const val POTATO_SOUP = "potato soup"
private const val SALAD = "salad"
private const val POSITIVE = "POSITIVE"
private const val NEGATIVE = "NEGATIVE"
private const val TOURNANT = "TOURNANT"

private val steakDishMap = mapOf(STEAK to 1)
private val deliveryDishesMap = mapOf(POTATO_SOUP to 1, SALAD to 1)

/**
 * System test validating that an in-house customer bottleneck causes a delivery customer
 * to exceed their patience deadline, cancel their order with a negative rating, and reject
 * the late delivery upon driver arrival.
 */
class DeliveryImpatienceCookBottleneckTest : ExampleSystemTestExtension() {

    override val name = "DeliveryImpatienceCookBottleneckTest"
    override val description = "Delivery order is delayed due to kitchen bottleneck, causing customer impatience " +
        "give-up, failed delivery, and negative review"
    override val logLevel = "DEBUG"

    override val maxTicks = 10
    override val restaurants = "deniztests/cooks2/restaurants.json"
    override val food = "deniztests/cooks2/food.json"
    override val scenario = "deniztests/cooks2/scenario.json"

    override suspend fun run() {
        // Fast-forward through init and pantry procurement directly to the start of the serving phase
        skipUntilString(TickStatusTestLogs.servingStart(1))

        assertTick1()
        assertTicks2To4()
        assertTicks5And6()
        assertTicks7And8()
        assertTicks9And10AndStats()
    }

    private suspend fun assertTick1() {
        assertNextLine(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(2, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, steakDishMap, 1))
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 2, deliveryDishesMap, null))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 1, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 3, 1))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT, 1, STEAK, 1, listOf(1)))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTicks2To4() {
        for (tick in 2..3) {
            assertNextLine(TickStatusTestLogs.tickStart(tick, 1))
            assertNextLine(TickStatusTestLogs.restStart(1))
            assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
            assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
            assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 0))
            assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
            assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
            assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
            assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
            assertNextLine(TickStatusTestLogs.restEnd(1))
        }

        assertNextLine(TickStatusTestLogs.tickStart(4, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, STEAK, 3))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 1, 1))
        assertNextLine(FohServiceTestLogs.serving(1, 1, steakDishMap, 1, 3))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 1, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTicks5And6() {
        // Tick 5: Cook starts potato soup
        assertNextLine(TickStatusTestLogs.tickStart(5, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT, 1, POTATO_SOUP, 2, listOf(2)))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 1, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))

        // Tick 6: Cook finishes potato soup, Group 1 leaves and rates POSITIVE
        assertNextLine(TickStatusTestLogs.tickStart(6, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, POTATO_SOUP, 5))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 1, 1))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.finishedEating(1, 1, 1, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 1))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 1, 1, 1))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 1))
        assertNextLine(FohServiceTestLogs.rating(1, 1, POSITIVE, 11, 5))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTicks7And8() {
        // Tick 7: Cook starts salad
        assertNextLine(TickStatusTestLogs.tickStart(7, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, TOURNANT, 1, SALAD, 2, listOf(2)))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 0, 1))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))

        // Tick 8: Cook finishes salad, handover to driver, Group 2 gives up and rates NEGATIVE
        assertNextLine(TickStatusTestLogs.tickStart(8, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, SALAD, 7))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 1, 1, 2))
        assertNextLine(FohServiceTestLogs.deliveryHandover(1, 1, deliveryDishesMap, 1, 2))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 1, 2))
        assertNextLine(DeliveryTestLogs.deliveryPrep(1, 1, 2, 2, 1))
        assertNextLine(DeliveryTestLogs.deliveryGivenUp(1, 2, 2))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.rating(1, 2, NEGATIVE, 11, 6))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    private suspend fun assertTicks9And10AndStats() {
        // Tick 9: Driver arrives but delivery fails
        assertNextLine(TickStatusTestLogs.tickStart(9, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(DeliveryTestLogs.deliveryDriving(1, 1, 5, 0))
        assertNextLine(DeliveryTestLogs.deliveryArrival(1, 1, 2, 2))
        assertNextLine(DeliveryTestLogs.deliveryFailed(1, 1, 2, 2))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))

        // Tick 10: Driver returns to restaurant
        assertNextLine(TickStatusTestLogs.tickStart(10, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(DeliveryTestLogs.deliveryReturned(1, 1))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))

        // Simulation statistics for maxTicks = 10
        assertNextLine(StatisticsTestLogs.STATS_CALCULATED)
        assertNextLine(StatisticsTestLogs.statsCooked(1, 3))
        assertNextLine(StatisticsTestLogs.statsServed(1, 1))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 0))
        assertNextLine(StatisticsTestLogs.statsReceived(1, 2))
    }
}
