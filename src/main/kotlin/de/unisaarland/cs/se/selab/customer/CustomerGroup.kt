package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.RatingType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.restaurant.Countertop

/**
 * Hands out the waiters taking a group's order one customer at a time, moving on to the next
 * waiter once the current one has reached their TAKE_ORDER action limit for this tick. Groups
 * with a single assigned waiter simply keep using that waiter.
 */
internal class WaiterRota(private val waiters: List<Waiter>) {
    private var index = 0

    /** the waiter who takes the next customer's order, or `null` for a delivery order */
    fun next(): Waiter? {
        while (index < waiters.size - 1 &&
            waiters[index].getTickLoad(ActionType.TAKE_ORDER) >= Constants.ACTION_LIMIT
        ) {
            index++
        }
        // once even the last waiter is at their limit the overflow stays with them: a group larger
        // than the action limit has to be taken by somebody, and the spec names no other waiter.
        return waiters.getOrNull(index)
    }
}

/** Represents an abstract customer group. */
sealed class CustomerGroup(
    val id: Id,
    val size: Int,
    val tableType: TableType,
    val visitingAt: Tick,
    val foodPreferences: List<FoodPreference>, // Could you make this public
) {
    var experience = ExperienceType.NEUTRAL

    // relevant to F27
    var currentOrder: Order? = null
    var customersRemainingInRestaurant = size

    /**
     * Wipes everything that belongs to a single visit, so a group coming back on a later evening
     * starts from scratch instead of carrying last evening's order, headcount and experience with
     * it. Call once per visit, when the group joins a restaurant's customer queue.
     */
    fun startNewVisit() {
        currentOrder = null
        customersRemainingInRestaurant = size
        experience = ExperienceType.NEUTRAL
    }

    /**
     * clears everything a group accumulated during a previous visit, so that a returning group starts fresh
     */
    fun resetForNewEvening() {
        experience = ExperienceType.NEUTRAL
        currentOrder = null
        customersRemainingInRestaurant = size
    }

    /** Whether the group arrives in the current tick. */
    open fun isVisitingThisTick(): Boolean {
        return Time.tick == visitingAt
    }

    /** How many of the group's customers have already left. */
    fun getCustomersWhoLeft(): Int {
        return size - customersRemainingInRestaurant
    }

    /**
     * Lets every customer choose a dish, in [orderingSequence], and places the order with the
     * kitchen. Each chosen dish reserves its ingredients at once, so later customers see less
     * stock. Customers who find no dish leave, which makes the experience negative.
     *
     * @param waiters the waiters taking the order, in the order they take customers; empty for a
     *   delivery order
     * @return `false` if nobody could order, in which case no order is placed
     */
    open fun placeOrder(waiters: List<Waiter>, menu: List<Recipe>, countertop: Countertop): Boolean {
        val listOfDishes = mutableListOf<Dish>()
        val waiterRota = WaiterRota(waiters)
        for (foodPreference in orderingSequence()) {
            val availableDishes = countertop.getAvailableRecipes(menu)
            val customerDish = foodPreference.decideDish(availableDishes, "", countertop.restaurantType)
            if (customerDish != null) {
                val currentWaiter = waiterRota.next()
                if (currentWaiter != null) {
                    registerDish(currentWaiter, customerDish, countertop)
                } else {
                    registerDish(customerDish, countertop = countertop)
                }
                listOfDishes.add(customerDish)
            }
        }
        if (listOfDishes.size < customersRemainingInRestaurant) {
            customersRemainingInRestaurant = listOfDishes.size
            experience = ExperienceType.NEGATIVE
        }

        if (customersRemainingInRestaurant == 0) {
            return false
        }

        val customerOrder = Order(listOfDishes)
        currentOrder = customerOrder
        countertop.addOrder(customerOrder)

        return true
    }

    /**
     * The group's customers in the sequence in which they order: the customer(s) with the most
     * excluded ingredients first, then the ones with the fewest favourite dishes. Ties keep the
     * order the preferences were listed in the group's JSON (the sort is stable).
     */
    protected fun orderingSequence(): List<FoodPreference> = foodPreferences.sortedWith(
        compareByDescending<FoodPreference> { it.excludedIngredients.size }
            .thenBy { it.favouriteDishes.size }
    )

    /** Whether the group visits a restaurant tonight. */
    abstract fun isVisitingTonight(): Boolean

    /** Registers an ordered [dish]: reserves its ingredients and counts one order action for [waiter]. */
    fun registerDish(waiter: Waiter, dish: Dish, countertop: Countertop) {
        val recipe = dish.recipe
        countertop.reserveIngredients(recipe)
        waiter.addToTickLoad(ActionType.TAKE_ORDER, 1)
    }

    /** Registers an ordered [dish] without a waiter (a delivery order): reserves its ingredients. */
    fun registerDish(dish: Dish, countertop: Countertop) {
        val recipe = dish.recipe
        countertop.reserveIngredients(recipe)
    }
    // made this public to acces in Event Group

    /** determines rating, computes with likelihood, overridden for casual groups */
    open fun determineRating(): RatingType {
        return when (experience) {
            ExperienceType.NEGATIVE -> RatingType.NEGATIVE
            ExperienceType.NEUTRAL -> RatingType.POSITIVE
            ExperienceType.POSITIVE -> RatingType.POSITIVE
        }
    }
}
