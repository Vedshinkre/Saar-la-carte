package de.unisaarland.cs.se.selab.customer

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

const val TEN = 10

/** Represents an abstract customer group. */
sealed class CustomerGroup(
    val id: Id,
    val size: Int,
    val tableType: TableType,
    val visitingAt: Tick,
    val foodPreferences: List<FoodPreference>, // Could you make this public
) {
    open var isWaitingToBeSeated: Boolean = true
        get() = Time.tick - visitingAt == 0 && field
    var experience = ExperienceType.NEUTRAL

    // relevant to F27
    var currentOrder: Order? = null
    var customersRemainingInRestaurant = size

    /**
     * takes the order of a customer group
     */
    fun placeOrder(waiters: List<Waiter>, menu: List<Recipe>, countertop: Countertop) {
        val listOfDishes = mutableListOf<Dish>()
        for (foodPreference in foodPreferences) {
            val currentWaiters = waiters.filter { it.getTickLoad(ActionType.TAKE_ORDER) < TEN }
            val currentWaiter = currentWaiters.firstOrNull() ?: break
            val availableDishes = countertop.getAvailableRecipes(menu)
            val customerDish = foodPreference.decideDish(availableDishes, "")
            if (customerDish != null) {
                registerDish(currentWaiter, customerDish, countertop)
                listOfDishes.addFirst(customerDish)
            }
        }
        if (listOfDishes.size < customersRemainingInRestaurant) {
            customersRemainingInRestaurant = listOfDishes.size
            experience = ExperienceType.NEGATIVE
        }
    }

    /**
     * returns true if group is visiting a restaurant tonight
     */
    fun isVisitingTonight(): Boolean {
        return Time.tick == visitingAt
    }

    /**
     * updates the pantry and the tickLoad of the waiter which "registers" that a dish has been ordered
     */
    private fun registerDish(waiter: Waiter, dish: Dish, countertop: Countertop) {
        val recipe = dish.recipe
        countertop.reserveIngredients(recipe)
        waiter.addToTickLoad(ActionType.TAKE_ORDER, 1)
    }

    /** determines rating, computes with likelihood, overridden for casual groups */
    open fun determineRating(): RatingType {
        return when (experience) {
            ExperienceType.NEGATIVE -> RatingType.NEGATIVE
            ExperienceType.NEUTRAL -> RatingType.POSITIVE
            ExperienceType.POSITIVE -> RatingType.POSITIVE
        }
    }
}
