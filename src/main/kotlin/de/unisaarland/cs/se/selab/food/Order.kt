package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.DishStatus
import java.util.concurrent.atomic.AtomicInteger

/**
 * The order that a customer orders. This class is used as a way to keep track of the status of an order aswell.
 * It is created by the Fron Of House and then used
 */
class Order(private val dishes: List<Dish>) {
    private val id: Id = nextId.getAndIncrement()
    private val orderedAt: Tick = Time.tick
    var servedAt: Tick? = null
    var deliveredAt: Tick? = null

    private companion object {
        val nextId = AtomicInteger(1)
    }

    init {
        require(id >= 1)
    }

    /** get dishes in the order that can be served this tick, ordered by basic dishes first then ascending recipe id */
    fun getServableDishes(): List<Dish> {
        val cookedDishes = dishes.filter { it.getStatus() == DishStatus.COOKED }
        return cookedDishes.sortedWith(compareBy({ !it.getIsBasic() }, { it.getRecipe().getId() }))
    }

    // explicit getters for relevant functions
    /**
     * returns Id of the order
     */
    fun getId(): Int {
        return id
    }

    /**
     * returns the list of dishes inside of an order
     */
    fun getDishes(): List<Dish> {
        return dishes
    }
    // functions with logic

    /**
     * returns List of dishes that have been served from this order
     */
    fun getServedDishes(): List<Dish> {
        val servedDishes = mutableListOf<Dish>()
        for (dish in dishes) {
            if (dish.getStatus() == DishStatus.SERVED) {
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
            if (dish.getStatus() == DishStatus.UNCOOKED) {
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
            if (dish.getStatus() != DishStatus.COOKED) {
                return false
            }
        }
        return true
    }

    /**
     * returns true if all the dishes in this order have been eaten
     */
    fun areAllDishesEaten(): Boolean {
        for (dish in dishes) {
            if (dish.getStatus() != DishStatus.EATEN) {
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
            val status = dish.getStatus()

            // If even a single dish is still being processed, the whole order is NOT done
            if (status != DishStatus.SERVED && status != DishStatus.ABORTED) {
                return false
            }
        }
        return true
    }

    /**
     * returns true if all the dishes in this order have been served or aborted
     */
    fun needsToBePartiallyServed(): Boolean {
        val currentTime = Time.tick
        return currentTime - orderedAt >= 2
    }
}
