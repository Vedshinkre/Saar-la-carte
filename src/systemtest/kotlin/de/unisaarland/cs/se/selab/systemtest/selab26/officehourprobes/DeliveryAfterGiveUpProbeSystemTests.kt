package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A/B probes for what happens to a delivery order after its group gives up while the meal has not
 * even been started - the area of the DeluluVery and YinAndYang failures.
 *
 * The delivery log section of the specification, on the given-up line: "Customers can decide to
 * give up waiting for a delivery [...]. All following delivery attempts of this order will fail."
 * That sentence only makes sense if a given-up order can still be attempted: cooked, handed to a
 * driver, driven out, and then
 * refused at the door with a "Delivery Failed" line. It also matches what the reference confirmed
 * for in-house customers (WalkedOutMealIsStillCookedSystemTest): a meal that was ordered is cooked,
 * whoever walked away from it.
 *
 * We do the opposite for deliveries: giving up marks the meal aborted, the kitchen drops the order,
 * and nothing about it is logged again.
 *
 * The scenario: one TOURNANT cook. Group 1 dines in and orders Slow A and Slow B, which keep the cook
 * busy from tick 1 to tick 8. Group 2 orders a Quick D for delivery in tick 1, wants it in tick 5 and
 * gives up in tick 8, when its dish is still queued behind Slow B.
 *
 * Results 10: reading B is the reference's. The premise and both B probes pass there, and
 * GivenUpDeliveryIsDroppedFromTheKitchenSystemTest fails. Both B probes fail on our dev branch
 * because of code that is not mine (Ansh's DeliveryProcessor.processAbortions): giving up sets
 * every dish of the order to ABORTED, so the kitchen drops the order and releaseStrandedDrivers
 * frees the driver. Fix: only set order.deliveryGivenUp (and the negative experience) there. Then
 * make Driver.handOverToCustomer fail on that flag as it does for ABORTED dishes, and make sure the
 * group does not rate a second time after the failed attempt.
 */
abstract class DeliveryAfterGiveUpScenario : ExampleSystemTestExtension() {
    override val restaurants = "officehourjson/givenupkitchen/restaurants.json"
    override val scenario = "officehourjson/givenupkitchen/scenario.json"
    override val food = "officehourjson/givenupkitchen/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 16

    /** the premise both readings share: the group gives up in tick 8, not before and not after */
    protected suspend fun skipToGiveUp() {
        skipUntilString(TickStatusTestLogs.tickStart(GIVE_UP_TICK, 1))
        skipUntilString(DeliveryTestLogs.deliveryGivenUp(1, 2, 2))
        skipUntilString(TickStatusTestLogs.tickStart(GIVE_UP_TICK + 1, 1))
    }

    protected companion object {
        /** visitingTick 5 plus the three ticks a delivery group waits */
        const val GIVE_UP_TICK = 8
        const val QUICK_D_COOKED =
            "[IMPORTANT] Kitchen Meal Cooked (R 1): Cook 1 finished cooking 1 meals of dish Quick D"
    }
}

/** Setup: the give-up happens in tick 8 on both implementations, so the other probes are comparable. */
class GivenUpDeliveryGivesUpInTickEightSystemTest : DeliveryAfterGiveUpScenario() {
    override val name = "GivenUpDeliveryGivesUpInTickEightSystemTest"
    override val description = "A delivery group whose dish is still queued gives up three ticks after its wanted tick"

    override suspend fun run() = skipToGiveUp()
}

/**
 * Reading A: giving up takes the meal out of the kitchen, so it is never cooked and the restaurant
 * ends the evening having cooked only group 1's two meals. This is what we implement.
 */
class GivenUpDeliveryIsDroppedFromTheKitchenSystemTest : DeliveryAfterGiveUpScenario() {
    override val name = "GivenUpDeliveryIsDroppedFromTheKitchenSystemTest"
    override val description = "The dish of a given-up delivery is never cooked"

    override suspend fun run() {
        skipToGiveUp()
        skipUntilString(StatisticsTestLogs.statsCooked(1, 2))
    }
}

/**
 * Reading B: the kitchen carries on with the order as it does for in-house customers who walked
 * out, so Quick D is still cooked after the give-up and counts as a third cooked meal.
 */
class GivenUpDeliveryIsStillCookedSystemTest : DeliveryAfterGiveUpScenario() {
    override val name = "GivenUpDeliveryIsStillCookedSystemTest"
    override val description = "The dish of a given-up delivery is still cooked and counted"

    override suspend fun run() {
        skipToGiveUp()
        skipUntilString(QUICK_D_COOKED)
        skipUntilString(StatisticsTestLogs.statsCooked(1, 3))
    }
}

/**
 * Reading B, carried through to the door: the cooked meal is handed to a driver, driven out and
 * refused, exactly as "all following delivery attempts of this order will fail" describes.
 */
class GivenUpDeliveryIsStillDrivenOutAndFailsSystemTest : DeliveryAfterGiveUpScenario() {
    override val name = "GivenUpDeliveryIsStillDrivenOutAndFailsSystemTest"
    override val description = "A given-up delivery is still driven out and fails at the customer"

    override suspend fun run() {
        skipToGiveUp()
        skipUntilString(DeliveryTestLogs.deliveryFailed(1, 1, 2, 2))
    }
}
