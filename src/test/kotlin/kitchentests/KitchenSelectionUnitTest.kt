package kitchentests

import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.restaurant.Kitchen
import de.unisaarland.cs.se.selab.restaurant.Pantry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for the cook-selection and recipe-priority logic in [Kitchen]:
 * [Kitchen.chooseCook] (which wraps the private eligibility check and rank tie-break),
 * [Kitchen.sortRecipesByBasicnessAndId] and [Kitchen.collectRecipes].
 *
 * These tests never call [Kitchen.processCooking] - they exercise the selection
 * helpers directly, so a recipe never needs real ingredients and the pantry is
 * never touched.
 */
class KitchenSelectionUnitTest {

    private fun kitchen(cooks: List<Cook>, restaurantType: RestaurantType = RestaurantType.ASIAN): Kitchen =
        Kitchen(cooks, Pantry(Stock(emptyList())), mutableListOf(), restaurantType)

    private fun recipe(
        id: Int,
        cookTypes: List<CookType> = listOf(CookType.TOURNANT),
        basicDishFor: RestaurantType? = null,
        duration: Int = 10
    ): Recipe = Recipe(id, "Recipe $id", duration, cookTypes, mutableMapOf(), basicDishFor)

    private fun cookWithId(id: Int, type: CookType): Cook =
        Cook(
            id = id,
            orderId = null,
            type = type,
            currentRecipe = null,
            remainingTicks = 0,
            isCooking = false
        )

    // ---- chooseCook / isCookEligible ----

    @Test
    fun `a busy cook is never chosen even if its type matches`() {
        val cook = Cook(CookType.TOURNANT)
        cook.startCooking(recipe(1), emptyList(), baseOrderId = 1)
        val kitchen = kitchen(listOf(cook))

        val chosen = kitchen.chooseCook(recipe(2, cookTypes = listOf(CookType.TOURNANT)))

        assertNull(chosen)
    }

    @Test
    fun `a free cook of a non matching type is not chosen`() {
        val cook = Cook(CookType.PASTRY)
        val kitchen = kitchen(listOf(cook))

        val chosen = kitchen.chooseCook(recipe(1, cookTypes = listOf(CookType.TOURNANT)))

        assertNull(chosen)
    }

    @Test
    fun `a free cook of a matching type is chosen`() {
        val cook = Cook(CookType.TOURNANT)
        val kitchen = kitchen(listOf(cook))

        val chosen = kitchen.chooseCook(recipe(1, cookTypes = listOf(CookType.TOURNANT)))

        assertSame(cook, chosen)
    }

    @Test
    fun `a recipe with several allowed cook types can be taken by any of them`() {
        val sousCook = Cook(CookType.SOUS)
        val kitchen = kitchen(listOf(sousCook))
        val multiTypeRecipe = recipe(1, cookTypes = listOf(CookType.EXEC, CookType.SOUS, CookType.TOURNANT))

        val chosen = kitchen.chooseCook(multiTypeRecipe)

        assertSame(sousCook, chosen)
    }

    @Test
    fun `chooseCook returns null instead of throwing when no cook is eligible`() {
        val kitchen = kitchen(emptyList())

        val chosen = kitchen.chooseCook(recipe(1))

        assertNull(chosen)
    }

    @Test
    fun `an id is only assigned to a cook that does not already have one`() {
        val freshCook = Cook(CookType.TOURNANT)
        val existingCook = cookWithId(id = 7, type = CookType.SOUS)
        val kitchen = kitchen(listOf(freshCook))

        kitchen.chooseCook(recipe(1, cookTypes = listOf(CookType.TOURNANT)))
        assertEquals(1, freshCook.id)

        val kitchenTwo = kitchen(listOf(existingCook))
        kitchenTwo.chooseCook(recipe(2, cookTypes = listOf(CookType.SOUS)))
        assertEquals(7, existingCook.id)
    }

    @Test
    fun `between two eligible cook types the less skilled one is spared - EXEC is reserved`() {
        val exec = Cook(CookType.EXEC)
        val pastry = Cook(CookType.PASTRY)
        val kitchen = kitchen(listOf(exec, pastry))
        val recipeForEither = recipe(1, cookTypes = listOf(CookType.EXEC, CookType.PASTRY))

        val chosen = kitchen.chooseCook(recipeForEither)

        assertSame(pastry, chosen)
    }

    @Test
    fun `between two eligible cook types of different rank the least skilled matching one wins`() {
        val sous = Cook(CookType.SOUS)
        val vegetable = Cook(CookType.VEGETABLE)
        val kitchen = kitchen(listOf(sous, vegetable))
        val recipeForEither = recipe(1, cookTypes = listOf(CookType.SOUS, CookType.VEGETABLE))

        val chosen = kitchen.chooseCook(recipeForEither)

        assertSame(vegetable, chosen)
    }

    @Test
    fun `two eligible cooks of the same type tie break on the lowest id`() {
        val higherId = cookWithId(id = 5, type = CookType.TOURNANT)
        val lowerId = cookWithId(id = 2, type = CookType.TOURNANT)
        val kitchen = kitchen(listOf(higherId, lowerId))

        val chosen = kitchen.chooseCook(recipe(1, cookTypes = listOf(CookType.TOURNANT)))

        assertSame(lowerId, chosen)
    }

    @Test
    fun `a cook without an id yet loses the tie break to one that already has an id`() {
        val withId = cookWithId(id = 3, type = CookType.TOURNANT)
        val withoutId = Cook(CookType.TOURNANT)
        val kitchen = kitchen(listOf(withoutId, withId))

        val chosen = kitchen.chooseCook(recipe(1, cookTypes = listOf(CookType.TOURNANT)))

        assertSame(withId, chosen)
    }

    // ---- sortRecipesByBasicnessAndId ----

    @Test
    fun `a basic dish for this restaurant type is sorted before a non basic one with a lower id`() {
        val kitchen = kitchen(emptyList(), restaurantType = RestaurantType.ASIAN)
        val basic = recipe(id = 9, basicDishFor = RestaurantType.ASIAN)
        val nonBasic = recipe(id = 1, basicDishFor = null)

        val sorted = kitchen.sortRecipesByBasicnessAndId(listOf(nonBasic, basic))

        assertEquals(listOf(basic, nonBasic), sorted)
    }

    @Test
    fun `two basic dishes are ordered by ascending id`() {
        val kitchen = kitchen(emptyList(), restaurantType = RestaurantType.ASIAN)
        val basicFive = recipe(id = 5, basicDishFor = RestaurantType.ASIAN)
        val basicTwo = recipe(id = 2, basicDishFor = RestaurantType.ASIAN)

        val sorted = kitchen.sortRecipesByBasicnessAndId(listOf(basicFive, basicTwo))

        assertEquals(listOf(basicTwo, basicFive), sorted)
    }

    @Test
    fun `two non basic dishes are ordered by ascending id`() {
        val kitchen = kitchen(emptyList(), restaurantType = RestaurantType.ASIAN)
        val nine = recipe(id = 9)
        val three = recipe(id = 3)

        val sorted = kitchen.sortRecipesByBasicnessAndId(listOf(nine, three))

        assertEquals(listOf(three, nine), sorted)
    }

    @Test
    fun `a basic dish for a different restaurant type is treated as non basic here`() {
        val kitchen = kitchen(emptyList(), restaurantType = RestaurantType.ASIAN)
        val basicForOtherType = recipe(id = 8, basicDishFor = RestaurantType.EUROPEAN)
        val plainLowerId = recipe(id = 1, basicDishFor = null)

        val sorted = kitchen.sortRecipesByBasicnessAndId(listOf(basicForOtherType, plainLowerId))

        assertEquals(listOf(plainLowerId, basicForOtherType), sorted)
    }

    // ---- collectRecipes ----

    @Test
    fun `collectRecipes only returns uncooked dishes matching the given recipe id`() {
        val kitchen = kitchen(emptyList())
        val targetRecipe = recipe(id = 1)
        val otherRecipe = recipe(id = 2)

        val matchingDish = Dish(targetRecipe)
        val alreadyCookedMatchingDish = Dish(targetRecipe).apply { status = DishStatus.COOKED }
        val otherDish = Dish(otherRecipe)
        val order = Order(listOf(matchingDish, alreadyCookedMatchingDish, otherDish))

        val (orderId, dishes) = kitchen.collectRecipes(targetRecipe, order)

        assertEquals(order.id, orderId)
        assertEquals(listOf(matchingDish), dishes)
    }

    @Test
    fun `collectRecipes returns an empty list when the order has no dish of that recipe`() {
        val kitchen = kitchen(emptyList())
        val targetRecipe = recipe(id = 1)
        val otherRecipe = recipe(id = 2)
        val order = Order(listOf(Dish(otherRecipe)))

        val (_, dishes) = kitchen.collectRecipes(targetRecipe, order)

        assertTrue(dishes.isEmpty())
    }
}
