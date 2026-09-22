package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * An evening runs for all 24 ticks even after its restaurants have closed, and it has to end there:
 * nothing an evening started may still be logged in the next one.
 *
 * Specification page 23: "Only deliveries already given to a driver continue after the opening time
 * of the restaurant until the end of the evening." Page 24 adds that at the end of the evening the
 * remaining deliveries are aborted "without rating or other consequences", which is what separates
 * the two halves of this file: a delivery that lands in time still rates, inside its own evening,
 * while one that does not is simply gone.
 *
 * Every evening 2 assertion below is a whole tick asserted line by line rather than a skip, because
 * what is being ruled out is an *extra* line. A leftover rating lands between the escorting status
 * and the rating status and breaks the chain there.
 */
abstract class EveningBoundaryScenario : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
    override val maxTicks = TWO_EVENINGS

    /**
     * Asserts the whole of the given tick of evening 2 for a restaurant that has nothing to do. Any
     * log carried over from evening 1 lands inside this block and breaks it.
     */
    protected suspend fun assertQuietTickOfSecondEvening(tick: Int) {
        skipUntilString(TickStatusTestLogs.tickStart(tick, 2))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 0, 0, 0))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 0, 0))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 0, 0, 0, 0))
        assertNextLine(FohServiceTestLogs.servingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 0))
        assertNextLine(TickStatusTestLogs.restEnd(1))
    }

    protected companion object {
        const val TWO_EVENINGS = 48
        const val DISH = "Quick Bowl"
    }
}

/**
 * The restaurant closes after tick 12, but the delivery it handed to a driver in tick 5 keeps going:
 * the driver needs eight ticks for the 40 km, arrives in tick 13, and the group eats its two full
 * ticks and rates in tick 15, three ticks after the restaurant shut its doors.
 */
abstract class LateRatingScenario : EveningBoundaryScenario() {
    override val restaurants = "officehourjson/lateratings/restaurants.json"
    override val scenario = "officehourjson/lateratings/scenario.json"
    override val food = "officehourjson/lateratings/food.json"
}

/** The delivery that was already on the road is still finished after the opening time ended. */
class ClosedRestaurantStillFinishesDeliverySystemTest : LateRatingScenario() {
    override val name = "ClosedRestaurantStillFinishesDeliverySystemTest"
    override val description = "A delivery handed over before closing still arrives after the restaurant closed"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(5, 1))
        skipUntilString(FohServiceTestLogs.deliveryHandover(1, 1, mapOf(DISH to 1), 1, 1))
        // the restaurant closes after tick 12 while the driver is still out
        skipUntilString(TickStatusTestLogs.tickStart(ARRIVAL_TICK, 1))
        skipUntilString(DeliveryTestLogs.deliveryArrival(1, 1, 1, 1))
        assertNextLine(DeliveryTestLogs.deliveryFinished(1, 1, 1, 1))
        skipUntilString(TickStatusTestLogs.tickStart(ARRIVAL_TICK + 1, 1))
    }

    private companion object {
        const val ARRIVAL_TICK = 13
    }
}

/**
 * The rating of that delivery is collected in tick 15, well after the restaurant closed and well
 * before the evening runs out, and it is counted by the rating status of that same tick.
 */
class RatingAfterClosingIsStillCollectedSystemTest : LateRatingScenario() {
    override val name = "RatingAfterClosingIsStillCollectedSystemTest"
    override val description = "A rating earned after the restaurant closed is still logged in that evening"

    override suspend fun run() {
        // nothing may be rated before tick 15, so the skip starts there
        skipUntilString(TickStatusTestLogs.tickStart(RATING_TICK, 1))
        skipUntilString(DeliveryTestLogs.deliveryFinishedEating(1, 1))
        // the restaurant closed after tick 12, so only delivering, eating and rating still run and
        // the eating status is followed straight by the rating, with no escorting status between
        assertNextLine(FohServiceTestLogs.eatingStatus(1, 0, 0))
        assertNextLine(FohServiceTestLogs.rating(1, 1, "POSITIVE", 1, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(1, 1))
        // and it belongs to this tick, not to a later one
        skipUntilString(TickStatusTestLogs.tickStart(RATING_TICK + 1, 1))
    }

    private companion object {
        const val RATING_TICK = 15
    }
}

/** That rating belongs to evening 1 only: evening 2 opens with nothing carried over. */
class RatingDoesNotRepeatInTheNextEveningSystemTest : LateRatingScenario() {
    override val name = "RatingDoesNotRepeatInTheNextEveningSystemTest"
    override val description = "A group that rated in evening 1 does not rate again in evening 2"

    override suspend fun run() {
        skipUntilString(FohServiceTestLogs.rating(1, 1, "POSITIVE", 1, 0))
        skipUntilString(TickStatusTestLogs.servingEnd(1))
        assertQuietTickOfSecondEvening(1)
    }
}

/**
 * The other half: a delivery that only reaches the customer in tick 23 leaves them still eating when
 * the evening runs out. Two earlier groups keep the single cook busy, so the Quick Bowl ordered in
 * tick 17 is only cooked in tick 22, handed over in tick 22 and delivered in tick 23, one tick short
 * of the two full ticks the group needs to eat it.
 *
 * The group therefore never earns a rating at all, and the point of the test is that it does not
 * collect one in the first tick of evening 2 either, once the front of house has been reset.
 */
abstract class StaleRatingScenario : EveningBoundaryScenario() {
    override val restaurants = "officehourjson/staleratings/restaurants.json"
    override val scenario = "officehourjson/staleratings/scenario.json"
    override val food = "officehourjson/staleratings/food.json"
}

/** Setup: the delivery really does arrive in tick 23, with the evening ending one tick later. */
class UnfinishedDeliveryArrivesInTheLastTicksSystemTest : StaleRatingScenario() {
    override val name = "UnfinishedDeliveryArrivesInTheLastTicksSystemTest"
    override val description = "The delayed delivery reaches the group in tick 23, too late to be eaten"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(ARRIVAL_TICK, 1))
        skipUntilString(DeliveryTestLogs.deliveryFinished(1, 1, 3, 3))
        // the group is still eating when the evening ends, so nothing of theirs is rated before it
        skipUntilString(TickStatusTestLogs.servingEnd(1))
    }

    private companion object {
        const val ARRIVAL_TICK = 23
    }
}

/** The group that was still eating at the end of evening 1 does not rate in evening 2. */
class UnfinishedDeliveryDoesNotRateNextEveningSystemTest : StaleRatingScenario() {
    override val name = "UnfinishedDeliveryDoesNotRateNextEveningSystemTest"
    override val description = "A delivery group still eating at the end of an evening does not rate in the next one"

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.servingEnd(1))
        assertQuietTickOfSecondEvening(1)
        assertQuietTickOfSecondEvening(2)
    }
}
