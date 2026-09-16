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
import de.unisaarland.cs.se.selab.loggers.FohReceptionLogger
import de.unisaarland.cs.se.selab.restaurant.Countertop

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
     * returns true if the customerGroup is visiting this tick
     */
    open fun isVisitingThisTick(): Boolean {
        return Time.tick == visitingAt
    }

    /**
     * takes the order of a customer group
     */
    fun placeOrder(waiters: List<Waiter>, menu: List<Recipe>, countertop: Countertop): Boolean {
        val listOfDishes = mutableListOf<Dish>()
        val currentWaiter = waiters.firstOrNull()
        for (foodPreference in foodPreferences) {
            val availableDishes = countertop.getAvailableRecipes(menu)
            val customerDish = foodPreference.decideDish(availableDishes, "")
            if (customerDish != null) {
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
            FohReceptionLogger.logFohNoOrdering(
                id,
                size - customersRemainingInRestaurant
            )
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
     * returns true if group is visiting a restaurant tonight
     */
    abstract fun isVisitingTonight(): Boolean

    /**
     * updates the pantry and the tickLoad of the waiter which "registers" that a dish has been ordered
     */
    private fun registerDish(waiter: Waiter, dish: Dish, countertop: Countertop) {
        val recipe = dish.recipe
        countertop.reserveIngredients(recipe)
        waiter.addToTickLoad(ActionType.TAKE_ORDER, 1)
    }

    /**
     * updates the pantry which "registers" that a dish has been ordered
     */
    private fun registerDish(dish: Dish, countertop: Countertop) {
        val recipe = dish.recipe
        countertop.reserveIngredients(recipe)
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
