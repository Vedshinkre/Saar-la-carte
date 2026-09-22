package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.DishStatus
import java.util.concurrent.atomic.AtomicInteger

/**
 * The order that a customer orders. This class is used as a way to keep track of the status of an order as well.
 * It is created by the Fron Of House and then used
 */
class Order(val dishes: List<Dish>) {
    val id: Id = nextId.getAndIncrement()
    var firstDishCookedAt: Tick? = null
    var lastDishServedAt: Tick? = null
    val orderedAt: Tick = Time.tick
    var deliveryGivenUp: Boolean = false
    var deliveredAt: Tick? = null
    private var servingStarted: Boolean = false

    /**
     * object used to set the IDs of orders during the simulation
     */
    companion object {
        private val nextId = AtomicInteger(1)

        /** restarts order ids at 1; call once per simulation run so ids do not leak between runs */
        fun resetIds() {
            nextId.set(1)
        }
    }

    init {
        require(id >= 1)
    }

    /**
     * returns true if all dishes have been served, false if not
     */
    fun areAllDishesServed(): Boolean {
        return dishes.all { it.status == DishStatus.SERVED }
    }

    /**
     * returns true if serving has started
     */
    fun hasServingStarted(): Boolean {
        return servingStarted
    }

    /**
     * returns true if any dish has been served already
     */
    fun startServing() {
        servingStarted = true
    }

    /**
     * Clears the "one-by-one serving is under way" flag, but only once every dish really has been
     * served. A partially served table has to keep the flag so the next tick carries on serving
     * instead of falling back into the waiting branch (items 109, 110).
     *
     * [lastDishServedAt] is deliberately not touched here: it is owned by the eating step, which
     * uses "still null" to detect the first tick an order is complete
     * (see [de.unisaarland.cs.se.selab.restaurant.helpers.EatingProcessor]).
     */
    fun markFullyServed() {
        if (areAllDishesServed()) {
            servingStarted = false
        }
    }

    /** get dishes in the order that can be served this tick, ordered by basic dishes first then ascending recipe id */
    fun getServableDishes(): List<Dish> {
        val cookedDishes = dishes.filter { it.status == DishStatus.COOKED && !it.abandoned }
        return cookedDishes.sortedWith(compareBy({ !it.isBasic }, { it.recipe.id }))
    }

    // functions with logic

    /**
     * returns List of dishes that have been served from this order
     */
    fun getServedDishes(): List<Dish> {
        val servedDishes = mutableListOf<Dish>()
        for (dish in dishes) {
            if (dish.status == DishStatus.SERVED) {
                servedDishes.add(dish)
            }
        }
        return servedDishes
    }

    /**
     * returns List of dishes that are uncooked from this order
     */
    fun getUncookedDishes(): List<Dish> {
        val uncookedDishes = mutableListOf<Dish>()
        for (dish in dishes) {
            if (dish.status == DishStatus.UNCOOKED) {
                uncookedDishes.add(dish)
            }
        }
        return uncookedDishes
    }

    /**
     * returns true if all the dishes in this order have been cooked
     */
    fun areAllDishesCooked(): Boolean {
        for (dish in dishes) {
            if (dish.status != DishStatus.COOKED) {
                return false
            }
        }
        return true
    }

    /**
     * returns true if all the dishes in this order have been eaten or aborted
     */
    fun areAllDishesEaten(): Boolean {
        for (dish in dishes) {
            if (dish.abandoned) continue
            if (dish.status != DishStatus.EATEN && dish.status != DishStatus.ABORTED) {
                return false
            }
        }
        return true
    }

    /**
     * returns true if all the dishes in this order have been served or aborted
     */
    fun areAllDishesServedOrAborted(): Boolean {
        for (dish in dishes) {
            val status = dish.status

            // If even a single dish is still being processed, the whole order is NOT done
            if (status != DishStatus.SERVED && status != DishStatus.ABORTED) {
                return false
            }
        }
        return true
    }

    /**
     * returns true if 2 ticks have passed since the order has been placed
     */
    fun needsToBePartiallyServed(): Boolean {
        val currentTime = Time.tick
        return currentTime - orderedAt >= 2
    }

    /**
     * returns map of dish names to the amount that was ordered
     */
    fun dishNameToAmount(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (dish in dishes) {
            if (map.containsKey(dish.recipe.name)) {
                val currentAmount = map[dish.recipe.name]
                if (currentAmount != null) {
                    map[dish.recipe.name] = currentAmount + 1
                }
            } else {
                map[dish.recipe.name] = 1
            }
        }

        return map
    }
}
