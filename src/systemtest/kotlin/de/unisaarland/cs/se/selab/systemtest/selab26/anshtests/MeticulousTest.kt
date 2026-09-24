package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.DeliveryTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohArrivalTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.FohServiceTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.KitchenTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.StatisticsTestLogs
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

private const val PIZZA = "margherita"
private const val TIRAMISU = "tiramisu"
private const val RISOTTO = "risotto"
private const val SOUP = "mushroom soup"
private const val OMELETTE = "omelette"
private const val NIGIRI = "salmon nigiri"
private const val TERIYAKI = "salmon teriyaki"
private const val TOFU = "tofu bowl"
private const val TAMAGO = "tamago"
private const val SAUCE = "SAUCE"
private const val POSITIVE = "POSITIVE"
private const val GRAMS = "g"
private const val BASIL = "basil"
private const val SALMON = "salmon"
private const val MUSHROOM = "mushroom"
private const val TOURNANT = "TOURNANT"
private const val STAFF = "STAFF"

/**
 * 4 evenings plus the first 4 ticks of evening 5
 * R1 Trattoria (EUROPEAN, rating 2), R2 Sakura (ASIAN, open 2-22, rating 5, no events)
 *
 * dish choice per customer: exclusions, first favorite, most preferred ingredients, highest id,
 * the event dish beating own favorites, falling back once a favorite is sold out or has no cook
 *
 * kitchen: lowest ranked free cook, batching over orders, a cook only starts again next tick
 *
 * ratings: served 4 ticks after ordering and delivered exactly on the visiting tick are both neutral,
 * so ALWAYS groups rate positive and SOME groups stay quiet
 * incidents change milk and rice amounts, milk packages, salmon supply, cooks and drivers
 */
class MeticulousTest : ExampleSystemTestExtension() {
    override val name = "MeticulousTest"
    override val description = "Dish choice, cook ranking, neutral ratings and incident arithmetic over five evenings"
    override val food = "anshtests/meticulous/food.json"
    override val restaurants = "anshtests/meticulous/restaurants.json"
    override val scenario = "anshtests/meticulous/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 100

    override suspend fun run() {
        eveningOne()
        eveningTwo()
        eveningThree()
        eveningFour()
        eveningFiveAndStatistics()
    }

    private suspend fun eveningOne() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 200, GRAMS, BASIL))
        skipUntilString(InitialAndPrepTestLogs.pantryProcured(2, 2000, GRAMS, SALMON))

        // R2 can't host events, so the event reserves at R1 despite the worse rating
        skipUntilString(TickStatusTestLogs.tickStart(1, 1))
        assertNextLine(TickStatusTestLogs.restDecision(5, 1))

        // regular: excluder and mushroom fan both get soup, favorite pizza, no prefs -> highest id
        skipUntilString(FohArrivalTestLogs.arrival(1, 1))
        assertNextLine(FohArrivalTestLogs.seating(1, 1, 1, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 1, 1, mapOf(PIZZA to 1, SOUP to 2, OMELETTE to 1), 1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 42))
        assertNextLine(FohArrivalTestLogs.seating(1, 42, 5, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 42, 2, mapOf(PIZZA to 1, OMELETTE to 2), 1))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 7, 2))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 7, 1))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, "EXEC", 2, PIZZA, 1, listOf(1, 2)))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 2, SAUCE, 2, SOUP, 1, listOf(1)))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 2, 4, 0, 0))

        // no TOURNANT, so the omelettes wait for the SAUCE cook
        skipUntilString(TickStatusTestLogs.tickStart(4, 1))
        skipUntilString(KitchenTestLogs.kitchenAssign(1, 2, SAUCE, 3, OMELETTE, 1, listOf(1, 2)))
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 2, 3, OMELETTE, 2))
        assertNextLine(KitchenTestLogs.kitchenStatus(1, 1, 3, 3, 7))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(PIZZA to 1, SOUP to 2, OMELETTE to 1), 1, 2))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(PIZZA to 1, OMELETTE to 2), 5, 2))
        skipUntilString(FohArrivalTestLogs.ordering(2, 30, 3, mapOf(TAMAGO to 2, TOFU to 2), 1))
        skipUntilString(KitchenTestLogs.kitchenAssign(2, 1, "VEGETABLE", 2, TOFU, 3, listOf(3)))
        assertNextLine(KitchenTestLogs.kitchenAssign(2, 2, TOURNANT, 2, TAMAGO, 3, listOf(3)))

        // tick 5: both waiters are at their seating limit for group 44, it tries again next tick
        skipUntilString(FohArrivalTestLogs.seating(1, 40, 3, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 40, 4, mapOf(SOUP to 2, OMELETTE to 2, RISOTTO to 2), 1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 43))
        assertNextLine(FohArrivalTestLogs.seating(1, 43, 8, listOf(2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 43, 5, mapOf(SOUP to 5), 2))
        assertNextLine(FohArrivalTestLogs.arrival(1, 44))
        assertNextLine(FohArrivalTestLogs.noSeatingNoWaitstaff(1, 44))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 2, 11, 2))
        skipUntilString(KitchenTestLogs.kitchenAssign(1, 3, "VEGETABLE", 2, RISOTTO, 4, listOf(4)))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 2, SAUCE, 7, SOUP, 4, listOf(4, 5)))

        // the mushrooms only last for 4 more risottos, the last two fans take the highest id
        skipUntilString(TickStatusTestLogs.tickStart(6, 1))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.mergingTables(1, 44, listOf(2, 7), 2))
        assertNextLine(FohArrivalTestLogs.seating(1, 44, 2, listOf(2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 44, 6, mapOf(OMELETTE to 2, RISOTTO to 4), 2))
        skipUntilString(KitchenTestLogs.kitchenAssign(1, 4, "SOUS", 4, RISOTTO, 6, listOf(6)))

        skipUntilString(FohArrivalTestLogs.ordering(1, 10, 7, mapOf(TIRAMISU to 3), 1))
        skipUntilString(KitchenTestLogs.kitchenAssign(1, 2, SAUCE, 4, OMELETTE, 4, listOf(4, 6)))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 5, "PASTRY", 3, TIRAMISU, 7, listOf(7)))

        // teriyaki takes 3 ticks and keeps the FISH cook busy, the nigiri of group 31 has to wait
        skipUntilString(TickStatusTestLogs.tickStart(8, 1))
        assertNextLine(TickStatusTestLogs.restDecision(20, 2))
        skipUntilString(KitchenTestLogs.kitchenAssign(2, 3, "FISH", 2, TERIYAKI, 8, listOf(8)))
        skipUntilString(FohArrivalTestLogs.ordering(1, 22, 9, mapOf(OMELETTE to 4), null))
        skipUntilString(FohArrivalTestLogs.ordering(2, 31, 10, mapOf(NIGIRI to 5), 1))
        skipUntilString(KitchenTestLogs.kitchenStatus(2, 1, 2, 0, 0))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(TIRAMISU to 3), 5, 3))
        skipUntilString(KitchenTestLogs.kitchenAssign(2, 3, "FISH", 5, NIGIRI, 10, listOf(10)))

        // delivery right on its visiting tick 12 and nigiri 4 ticks after ordering are both neutral
        skipUntilString(DeliveryTestLogs.deliveryArrival(2, 1, 20, 8))
        skipUntilString(FohServiceTestLogs.serving(2, 1, mapOf(NIGIRI to 5), 3, 4))
        skipUntilString(FohServiceTestLogs.rating(2, 20, POSITIVE, 7, 0))
        skipUntilString(FohServiceTestLogs.rating(2, 31, POSITIVE, 8, 0))
    }

    private suspend fun eveningTwo() {
        skipUntilString(InitialAndPrepTestLogs.incident(1, "RECIPE", 2))
        assertNextLine(InitialAndPrepTestLogs.incident(2, "UNAVAILABLE", 2))
        assertNextLine(InitialAndPrepTestLogs.incident(3, STAFF, 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 60, "X", "egg"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 2000, GRAMS, MUSHROOM))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))

        // no VEGETABLE cook anymore, the TOURNANT cooks tofu bowl first and tamago one tick later
        skipUntilString(TickStatusTestLogs.tickStart(4, 2))
        skipUntilString(KitchenTestLogs.kitchenAssign(2, 1, TOURNANT, 2, TOFU, 12, listOf(12)))
        skipUntilString(FohServiceTestLogs.noServing(2, 1, 2, 1))
        skipUntilString(KitchenTestLogs.kitchenAssign(2, 1, TOURNANT, 2, TAMAGO, 12, listOf(12)))
        assertNextLine(KitchenTestLogs.kitchenCooked(2, 1, 2, TAMAGO, 1))
        skipUntilString(FohServiceTestLogs.serving(2, 1, mapOf(TAMAGO to 2, TOFU to 2), 1, 1))

        // group 41 prefers ASIAN but can't eat anything at R2
        skipUntilString(TickStatusTestLogs.tickStart(7, 2))
        assertNextLine(TickStatusTestLogs.restDecision(41, 1))
        skipUntilString(FohArrivalTestLogs.ordering(1, 41, 14, mapOf(SOUP to 3), 1))

        // salmon from evening 1 is still good, so teriyaki can be ordered although none can be bought
        skipUntilString(TickStatusTestLogs.restDecision(21, 2))
        skipUntilString(FohArrivalTestLogs.ordering(2, 21, 15, mapOf(TERIYAKI to 1), null))
        skipUntilString(DeliveryTestLogs.deliveryArrival(2, 1, 21, 15))

        // on time is neutral and a SOME group doesn't rate that
        skipUntilString(TickStatusTestLogs.tickStart(17, 2))
        skipUntilString(DeliveryTestLogs.deliveryReturned(2, 1))
        assertNextLine(DeliveryTestLogs.deliveryFinishedEating(2, 21))
        assertNextLine(FohServiceTestLogs.eatingStatus(2, 0, 0))
        assertNextLine(FohServiceTestLogs.escortingStatus(2, 0, 0))
        assertNextLine(FohServiceTestLogs.ratingStatus(2, 0))
    }

    private suspend fun eveningThree() {
        // milk is now sold in 300 mL packages, the expired salmon can't be replaced
        skipUntilString(InitialAndPrepTestLogs.incident(4, "RECIPE", 3))
        assertNextLine(InitialAndPrepTestLogs.incident(5, "PACKAGING", 3))
        assertNextLine(InitialAndPrepTestLogs.incident(6, STAFF, 3))
        assertNextLine(InitialAndPrepTestLogs.prepStart(3))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 170, GRAMS, BASIL))
        skipUntilString(InitialAndPrepTestLogs.pantryProcured(1, 900, "mL", "milk"))
        skipUntilString(InitialAndPrepTestLogs.pantryRemoved(2, 180, GRAMS, BASIL))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(2, 1050, GRAMS, SALMON))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, 200, GRAMS, BASIL))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))

        // no PASTRY cook, the tiramisu fans fall back to omelette and get batched with group 40
        skipUntilString(TickStatusTestLogs.tickStart(6, 3))
        skipUntilString(FohArrivalTestLogs.seating(1, 11, 5, listOf(2)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 11, 19, mapOf(OMELETTE to 3), 2))
        skipUntilString(KitchenTestLogs.kitchenAssign(1, 2, SAUCE, 5, OMELETTE, 18, listOf(18, 19)))
        skipUntilString(FohServiceTestLogs.serving(1, 2, mapOf(OMELETTE to 3), 5, 1))
        assertNextLine(FohServiceTestLogs.serving(1, 1, mapOf(SOUP to 2, OMELETTE to 2, RISOTTO to 2), 3, 2))

        // no salmon at all now, the teriyaki fans order tamago
        skipUntilString(TickStatusTestLogs.tickStart(8, 3))
        assertNextLine(TickStatusTestLogs.restDecision(20, 2))
        skipUntilString(FohArrivalTestLogs.ordering(2, 20, 20, mapOf(TAMAGO to 2), null))
        skipUntilString(FohServiceTestLogs.rating(1, 11, POSITIVE, 14, 1))
        assertNextLine(FohServiceTestLogs.rating(1, 40, POSITIVE, 15, 1))
        skipUntilString(FohServiceTestLogs.rating(2, 20, POSITIVE, 11, 0))
    }

    private suspend fun eveningFour() {
        skipUntilString(InitialAndPrepTestLogs.incident(7, STAFF, 4))
        assertNextLine(InitialAndPrepTestLogs.prepStart(4))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 500, GRAMS, MUSHROOM))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 2000, GRAMS, "flour"))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 600, "mL", "milk"))
        skipUntilString(InitialAndPrepTestLogs.pantryProcured(2, 2000, GRAMS, SALMON))

        // the event goes to the busiest waiter; its dish beats the risotto favorites, only the flour hater differs
        skipUntilString(TickStatusTestLogs.tickStart(3, 4))
        assertNextLine(TickStatusTestLogs.restStart(1))
        assertNextLine(FohArrivalTestLogs.arrival(1, 5))
        assertNextLine(FohArrivalTestLogs.seating(1, 5, 4, listOf(1)))
        assertNextLine(FohArrivalTestLogs.ordering(1, 5, 22, mapOf(PIZZA to 9, OMELETTE to 1), listOf(1)))
        assertNextLine(FohArrivalTestLogs.seatingStatus(1, 1, 10, 1))
        assertNextLine(FohArrivalTestLogs.orderingStatus(1, 10, 1))
        // the EXEC finishes the regular's pizza this tick and only starts the event pizzas next tick
        assertNextLine(KitchenTestLogs.kitchenCooked(1, 1, 1, PIZZA, 1))

        skipUntilString(TickStatusTestLogs.tickStart(4, 4))
        skipUntilString(KitchenTestLogs.kitchenAssign(1, 2, SAUCE, 2, OMELETTE, 21, listOf(21, 22)))
        assertNextLine(KitchenTestLogs.kitchenAssign(1, 1, "EXEC", 9, PIZZA, 22, listOf(22)))
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(PIZZA to 9, OMELETTE to 1), 4, 2))
        skipUntilString(FohServiceTestLogs.escorting(1, 1, 10, 5, 4))

        // R1 lost its only driver and R2 isn't EUROPEAN
        skipUntilString(TickStatusTestLogs.tickStart(9, 4))
        assertNextLine(TickStatusTestLogs.restNoDecision(22))
        assertNextLine(TickStatusTestLogs.restDecision(31, 2))
        skipUntilString(FohServiceTestLogs.serving(2, 1, mapOf(NIGIRI to 5), 3, 1))
        skipUntilString(FohServiceTestLogs.rating(2, 31, POSITIVE, 13, 0))
    }

    private suspend fun eveningFiveAndStatistics() {
        skipUntilString(TickStatusTestLogs.servingEnd(4))
        assertNextLine(InitialAndPrepTestLogs.prepStart(5))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 90, GRAMS, BASIL))
        assertNextLine(InitialAndPrepTestLogs.pantryRemoved(1, 1700, GRAMS, MUSHROOM))

        // 100 ticks stop right after tick 4 of evening 5
        skipUntilString(FohServiceTestLogs.serving(1, 1, mapOf(PIZZA to 1, SOUP to 2, OMELETTE to 1), 1, 2))
        skipUntilString(TickStatusTestLogs.restEnd(2))
        assertNextLine(StatisticsTestLogs.STATS_CALCULATED)
        assertNextLine(StatisticsTestLogs.statsCooked(1, 72))
        assertNextLine(StatisticsTestLogs.statsServed(1, 68))
        assertNextLine(StatisticsTestLogs.statsDelivered(1, 4))
        assertNextLine(StatisticsTestLogs.statsReceived(1, 14))
        assertNextLine(StatisticsTestLogs.statsCooked(2, 31))
        assertNextLine(StatisticsTestLogs.statsServed(2, 26))
        assertNextLine(StatisticsTestLogs.statsDelivered(2, 5))
        assertNextLine(StatisticsTestLogs.statsReceived(2, 8))
    }
}
