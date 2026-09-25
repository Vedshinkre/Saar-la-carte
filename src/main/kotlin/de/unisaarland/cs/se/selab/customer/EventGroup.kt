package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.ActionType
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
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
     * Whether the event this group is attending falls exactly three evenings from now.
     *
     * @return `true` if [eventEvening] is the current evening plus three
     */
    fun visitingInThreeEvenings(): Boolean {
        return Time.evening + 3 == eventEvening
    }

    /**
     * Whether the group visits its restaurant tonight, i.e. on the evening the event is held.
     *
     * @return `true` if the current evening is [eventEvening]
     */
    override fun isVisitingTonight(): Boolean {
        return eventEvening == Time.evening
    }

    /**
     * Whether the group arrives at its table in the current tick.
     *
     * @return `true` if the current tick is [visitingAt]
     */
    override fun isVisitingThisTick(): Boolean {
        return visitingAt == Time.tick
    }

    /**
     * The event's featured dish for the restaurant type the group currently sits at, if any.
     *
     * @return the dish name from [eventDishes], or `null` before the group has been seated at a restaurant
     */
    fun getCurrentEventDish(): String? {
        if (currentRestaurantType != null) {
            return eventDishes[currentRestaurantType]
        }
        return null
    }

    /**
     * Lets every customer in the group choose a dish and places the order with the kitchen,
     * using waiters that still have order-taking capacity left in [waitersToTakeOrder]. Each
     * waiter's remaining capacity is decremented as it is used; customers who find no dish leave,
     * which makes the experience negative.
     *
     * @param waitersToTakeOrder waiters still available to take an order, mapped to how many more
     *   orders each can take this tick
     * @param menu the recipes the restaurant can currently prepare from
     * @param countertop the countertop to reserve ingredients from and place the order on
     * @return `false` if nobody could order, in which case no order is placed
     */
    fun placeOrder(waitersToTakeOrder: MutableMap<Waiter, Int>, menu: List<Recipe>, countertop: Countertop): Boolean {
        val listOfDishes = mutableListOf<Dish>()

        for (foodPreference in orderingSequence()) {
            val currentWaiter = waitersToTakeOrder.filter { it.value > 0 }.keys.firstOrNull { waiter ->
                waiter.getTickLoad(ActionType.TAKE_ORDER) < Constants.ACTION_LIMIT
            }
            if (currentWaiter == null) {
                break
            }

            val availableDishes = countertop.getAvailableRecipes(menu)
            val eventFavoriteDish = eventDishes[currentRestaurantType].orEmpty()
            val customerDish = foodPreference.decideDish(availableDishes, eventFavoriteDish, countertop.restaurantType)
            waitersToTakeOrder[currentWaiter] = waitersToTakeOrder.getValue(currentWaiter) - 1
            if (customerDish != null) {
                registerDish(currentWaiter, customerDish, countertop)
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
}
