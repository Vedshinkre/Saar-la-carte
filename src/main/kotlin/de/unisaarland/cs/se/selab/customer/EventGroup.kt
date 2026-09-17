package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.FohReceptionLogger
import de.unisaarland.cs.se.selab.restaurant.Countertop

/** Represents an event customer group. */
class EventGroup(
    id: Id,
    size: Int,
    tableType: TableType,
    visitingAt: Tick,
    foodPreferences: List<FoodPreference>,
    val restaurantTypes: List<RestaurantType>,
    val eventEvening: Evening,
    val eventDishes: Map<RestaurantType, String>
) : CustomerGroup(id, size, tableType, visitingAt, foodPreferences) {
    var currentRestaurantType: RestaurantType? = null

    /**
     * beep beep I;m a document
     */
    fun visitingInThreeEvenings(): Boolean {
        return Time.evening + 3 == eventEvening
    }

    /**
     * sake of detect
     */

    override fun isVisitingTonight(): Boolean {
        return eventEvening == Time.evening
    }

    /**
     * sake of detect
     */
    override fun isVisitingThisTick(): Boolean {
        return visitingAt == Time.tick
    }

    /**
     * sake of detect
     */
    fun getCurrentEventDish(): String? {
        if (currentRestaurantType != null) {
            return eventDishes[currentRestaurantType]
        }
        return null
    }

    /**
     * takes the order of a customer group
     */
    override fun placeOrder(waiters: List<Waiter>, menu: List<Recipe>, countertop: Countertop): Boolean {
        val listOfDishes = mutableListOf<Dish>()
        val currentWaiter = waiters.firstOrNull()
        for (foodPreference in foodPreferences) {
            val availableDishes = countertop.getAvailableRecipes(menu)
            val eventFavoriteDish = requireNotNull(eventDishes[currentRestaurantType])
            val customerDish = foodPreference.decideDish(availableDishes, eventFavoriteDish)
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
}
