package kitchentests

import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Recipe
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tutor-reported bug: a delivery a customer gave up on can still be delivered successfully.
 * Root cause: [DeliveryProcessor][de.unisaarland.cs.se.selab.restaurant.helpers.DeliveryProcessor]
 * `.processAbortions()` sets a dish's status to `ABORTED` directly, but `Cook.cookDishes()` finishes
 * cooking by unconditionally overwriting every assigned dish's status to `COOKED`
 * (`Cook.kt:134-136`), regardless of what happened to it while the cook wasn't looking. If a dish is
 * still `COOKING` when its customer gives up, the cook - unaware of the abort, and per spec correctly
 * "carries on" cooking it - resurrects it back to `COOKED` once done, making it servable again.
 *
 * Note: this is the same class of bug that
 * [EatingProcessor][de.unisaarland.cs.se.selab.restaurant.helpers.EatingProcessor] used to have for
 * in-house customers leaving without being served. That path was fixed by switching from
 * `dish.status = ABORTED` to a separate `dish.abandoned = true` flag, which `Cook.cookDishes()` never
 * touches. `DeliveryProcessor` was not updated the same way and still uses `status = ABORTED`, so this
 * test still reproduces a real, currently-unfixed bug in the delivery path specifically.
 */
class AbortedDishSurvivesCookCompletionTest {
    @Test
    fun `a dish aborted while still cooking stays ABORTED once the cook finishes it`() {
        val recipe = Recipe(
            1,
            "Soup",
            duration = 30,
            cookType = listOf(CookType.TOURNANT),
            ingredients = mutableMapOf(),
            basicDishFor = null,
        )
        val dish = Dish(recipe)
        val cook = Cook(CookType.TOURNANT)

        cook.startCooking(recipe, listOf(dish), baseOrderId = 1)
        assertEquals(DishStatus.COOKING, dish.status)

        // the customer gives up mid-cook: DeliveryProcessor.processAbortions sets the status
        // directly, the same way it does in production
        dish.status = DishStatus.ABORTED

        var result = cook.cookDishes()
        while (result.finishedThisTick == 0) {
            result = cook.cookDishes()
        }

        assertEquals(
            DishStatus.ABORTED,
            dish.status,
            "the cook finishing an already-aborted dish must not make it servable again"
        )
    }
}
