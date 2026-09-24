package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val POSITIVE = "POSITIVE"
private const val TOURNANT = "TOURNANT"
private const val ROAST = "ROAST"
private const val SOUP = "soup"
private const val STEW = "stew"
private const val ROAST_PLATTER = "roast platter"

/**
 * Kitchen scheduling under real cooking durations: two casual groups ordering the same dish in
 * the same tick are batched into one cooking job, a dish cookable by both a generalist and a
 * specialist goes to the more specialized idle cook, and the basic dish still cooks instantly
 * for the EXEC. An EVENT visit later reuses the same kitchen once everything is idle again.
 */
class FullSystemTest5 : ExampleSystemTestExtension() {
    override val name = "FullSystemTest5"
    override val description =
        "Same-tick orders batch into one cook job, dual-eligible dish picks the specialist cook"
    override val food = "deniztests/fullsystemtest5/food.json"
    override val restaurants = "deniztests/fullsystemtest5/restaurants.json"
    override val scenario = "deniztests/fullsystemtest5/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        assertRegularSoupThenBatchedAndSpecializedOrders()
        assertCookingFinishesAtTheirOwnDurations()
        assertEventVisitOnEveningFour()
    }

    private suspend fun assertRegularSoupThenBatchedAndSpecializedOrders() {
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        // The event group reserves its table three evenings before its actual visit
        assertNextLine(TickStatusTestLogs.restDecision(5, 1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(SOUP to 2), 1))

        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restDecision(2, 1))
        assertNextLine(TickStatusTestLogs.restDecision(3, 1))
        assertNextLine(TickStatusTestLogs.restDecision(4, 1))
        skipUntilString(FohArrivalTestLogs.arrival(1, 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 2, 2, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 2, 2, mapOf(STEW to 2), 1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 3))
        assertNextLine(FohArrivalTestLogs.seating(1, 3, 3, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 3, 3, mapOf(STEW to 2), 1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 4))
        assertNextLine(FohArrivalTestLogs.seating(1, 4, 4, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 4, 4, mapOf(ROAST_PLATTER to 2), 1))
        // Groups 2 and 3 both ordered stew before the kitchen assigned it: one batched job
        skipUntilString(KitchenTestLogs.kitchenAssign(1, 2, TOURNANT, 4, STEW, 2, listOf(2, 3)))
        // The roast platter is cookable by TOURNANT or ROAST: the specialist ROAST cook wins
        // even though a second, idle TOURNANT cook is available
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 3, ROAST, 2, ROAST_PLATTER, 4, listOf(4)))
    }

    private suspend fun assertCookingFinishesAtTheirOwnDurations() {
        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        skipUntilString(KitchenTestLogs.kitchenCooked(1, 3, 2, ROAST_PLATTER, 1))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 6, 2, 2))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(ROAST_PLATTER to 2), 4, 1))
        skipUntilString(FohServiceTestLogs.rating(1, 1, POSITIVE, 3, 0))

        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        skipUntilString(KitchenTestLogs.kitchenCooked(1, 2, 4, STEW, 2))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 4, 4, 4))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(STEW to 2), 2, 2))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(STEW to 2), 3, 2))

        // Neither casual group ever rates: both have ratingLikelihood NEVER
        skipUntilString(FohServiceTestLogs.finishedEating(1, 2, 4, 4))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 2, 2, 2))
        assertNextLine(FohServiceTestLogs.finishedEating(1, 2, 3, 3))
    }

    private suspend fun assertEventVisitOnEveningFour() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 4))
        skipUntilString(FohArrivalTestLogs.arrival(1, 5))
        assertNextLine(FohArrivalTestLogs.seating(1, 5, 5, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 5, 8, mapOf(SOUP to 4), listOf(1)))

        skipUntilString(FohServiceTestLogs.rating(1, 1, POSITIVE, 6, 0))
        skipUntilString(FohServiceTestLogs.finishedEating(1, 4, 5, 5))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 4))
        assertNextLine(FohServiceTestLogs.escorting(1, 1, 4, 5, 5))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 1, 4))
        assertNextLine(FohServiceTestLogs.rating(1, 5, POSITIVE, 7, 0))
    }
}
