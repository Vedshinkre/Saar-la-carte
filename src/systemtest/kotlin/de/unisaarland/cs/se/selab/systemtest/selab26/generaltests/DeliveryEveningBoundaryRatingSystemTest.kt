package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * F20/F27/P05: a delivery that is handed over on the very last tick of the evening (tick 24) still has
 * an unambiguous, already-computed experience -- [de.unisaarland.cs.se.selab.actors.Driver
 * .handOverToCustomer] sets it the moment the driver hands the food over, based on whether that happens
 * before, at, or after the group's `visitingTick`. Here the group's `visitingTick` is 21 (the latest a
 * delivery's desired arrival may validly be, per the "not in the last 3 ticks" rule) but a blocking
 * order ahead of it in the kitchen queue delays the actual hand-over to tick 24, three ticks late, which
 * `handOverToCustomer` correctly scores as [de.unisaarland.cs.se.selab.enums.ExperienceType.NEGATIVE].
 *
 * Confirmed bug (reproduced by running this exact fixture against our own jar): the group never rates
 * on that NEGATIVE experience. `Dish.updateEating()` needs 3 calls (2 full ticks after hand-over) to
 * reach EATEN, so it cannot finish within evening 1. But `Simulation.simulateEvening()` calls
 * `customers.forEach { it.resetForNewEvening() }` for *every* customer at the very start of evening 2,
 * unconditionally -- including this group, whose delivery from evening 1 was never resolved. That reset
 * wipes `currentOrder` to null and `experience` back to NEUTRAL. `RatingProcessor.processRatings()`
 * then treats `order == null` as "eligible to rate" for a delivery group, so the group rates on evening
 * 2's first tick anyway, but off the freshly-reset NEUTRAL experience rather than the NEGATIVE one that
 * was already correctly computed. With `ratingLikelihood: ALWAYS` ("additionally... positive rating in
 * neutral experiences"), this produces a POSITIVE rating for what should be a late, negative delivery,
 * and no "Delivery Finished Eating" log is ever produced (the group's order is gone before
 * `processDeliveryEating` can log it).
 *
 * This test asserts the behaviour the spec implies is correct (a NEGATIVE rating), so it currently
 * fails against our own jar -- that failure is the point, it documents the bug for whoever owns
 * delivery/eating/rating (F20/F27/P05) to fix. All ticks below were verified against our own jar.
 */
class DeliveryEveningBoundaryRatingSystemTest : ExampleSystemTestExtension() {
    override val name = "DeliveryEveningBoundaryRatingSystemTest"
    override val description = "A delivery handed over three ticks late right at evening's end still rates negative"

    override val restaurants = "deliveryeveningboundaryjson/restaurants.json"
    override val food = "deliveryeveningboundaryjson/food.json"
    override val scenario = "deliveryeveningboundaryjson/scenario.json"

    override val logLevel = "DEBUG"
    override val maxTicks = 30

    override suspend fun run() {
        // group 1 (dine-in) occupies the only cook until tick 19, delaying group 2's delivery dish
        skipUntilString(TickStatusTestLogs.tickStart(23, 1))
        skipUntilString(FohServiceTestLogs.deliveryHandover(1, 1, mapOf("Delivered Dish" to 1), 1, 2))

        // hand-over happens on the evening's last tick, three ticks after group 2's visitingTick of 21
        skipUntilString(TickStatusTestLogs.tickStart(24, 1))
        skipUntilString(DeliveryTestLogs.deliveryFinished(restId = 1, driverId = 1, orderId = 2, groupId = 2))

        // the delivery needs two more ticks to finish eating, which do not exist in this evening, but
        // the group must still rate negatively on its already-computed late-delivery experience once
        // it is resolved, whenever that happens -- not positively off a wiped, reset experience
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        skipUntilString(FohServiceTestLogs.rating(restId = 1, groupId = 2, rating = "NEGATIVE", pos = 10, neg = 1))
    }
}
