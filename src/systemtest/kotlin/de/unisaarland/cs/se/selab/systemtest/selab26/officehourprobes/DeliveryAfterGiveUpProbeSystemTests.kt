package de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * A/B probes (Sep 23) for a delivery order whose group gives up before its meal is even started.
 * Written for the failing DeluluVery and YinAndYang full tests. At the time our implementation
 * dropped the order from the kitchen.
 *
 * The specification's given-up log says "All following delivery attempts of this order will fail".
 * That only makes sense if the order is still cooked, driven out, and refused at the door.
 * Adjustment #21 and forum topic 126 say the same. The reference chose reading B (runs 9-12), and
 * our implementation was changed to match.
 *
 * One TOURNANT cook. Group 1 dines in and its Slow A and Slow B keep the cook busy until tick 8.
 * Group 2 orders Quick D for delivery in tick 1, wants it in tick 5, and gives up in tick 8, while
 * Quick D is still queued.
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

/** Premise shared by both readings: group 2 gives up within tick 8 (visiting tick 5 plus three ticks). */
class GivenUpDeliveryGivesUpInTickEightSystemTest : DeliveryAfterGiveUpScenario() {
    override val name = "GivenUpDeliveryGivesUpInTickEightSystemTest"
    override val description = "A delivery group whose dish is still queued gives up three ticks after its wanted tick"

    override suspend fun run() = skipToGiveUp()
}

/**
 * Reading A (rejected, fails by design): giving up takes the meal out of the kitchen, so only
 * group 1's two meals are cooked.
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
 * Reading B (the reference's): the kitchen still cooks Quick D after the give-up, as it does for
 * in-house customers who walked out, and it counts as a third cooked meal.
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
 * Reading B at the door (the reference's): the cooked meal is driven out and the attempt ends with
 * "Delivery Failed", as "all following delivery attempts of this order will fail" describes.
 */
class GivenUpDeliveryIsStillDrivenOutAndFailsSystemTest : DeliveryAfterGiveUpScenario() {
    override val name = "GivenUpDeliveryIsStillDrivenOutAndFailsSystemTest"
    override val description = "A given-up delivery is still driven out and fails at the customer"

    override suspend fun run() {
        skipToGiveUp()
        skipUntilString(DeliveryTestLogs.deliveryFailed(1, 1, 2, 2))
    }
}
