package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.DishStatus
import java.util.concurrent.atomic.AtomicInteger

/**
 * One order of a customer group: its dishes, and the state of the order as it goes through the
 * kitchen, serving and eating. Order ids count up across the whole run.
 *
 * @property firstDishCookedAt the tick the first dish was cooked; starts the partial-serving hold
 * @property lastDishServedAt the tick the order was first completely served; set by the eating step
 * @property orderedAt the tick the order was placed
 * @property deliveryGivenUp set when a delivery group gave up waiting; later delivery attempts fail
 * @property deliveredAt the tick a driver handed the order over to a delivery group
 */
class Order(val dishes: List<Dish>) {
    val id: Id = nextId.getAndIncrement()
    var firstDishCookedAt: Tick? = null
    var lastDishServedAt: Tick? = null
    val orderedAt: Tick = Time.tick
    var deliveryGivenUp: Boolean = false
    var deliveredAt: Tick? = null
    private var servingStarted: Boolean = false

    /** Hands out the order ids. */
    companion object {
        private val nextId = AtomicInteger(1)

        /** Restarts the ids at 1. Called once per simulation run so ids do not leak between runs. */
        fun resetIds() {
            nextId.set(1)
        }
    }

    init {
        require(id >= 1)
    }

    /** Whether every dish has been served. */
    fun areAllDishesServed(): Boolean {
        return dishes.all { it.status == DishStatus.SERVED }
    }

    /**
     * Whether serving has started: the table has already received dishes, or was once turned down
     * for lack of waiter capacity. A started order is served piece by piece, without being held back.
     */
    fun hasServingStarted(): Boolean {
        return servingStarted
    }

    /** Marks the order as started (see [hasServingStarted]). */
    fun startServing() {
        servingStarted = true
    }

    /**
     * Clears the started state (see [hasServingStarted]) once every dish has been served. Until then
     * it stays, so a partly served table carries on being served in the next tick.
     *
     * [lastDishServedAt] is left to the eating step, which uses it to detect the first tick the order
     * is complete.
     */
    // DOTO: rename this, maybe checkAndMarkFullyServed
    fun markFullyServed() {
        if (areAllDishesServed()) {
            servingStarted = false
        }
    }

    /**
     * The dishes that are cooked and waiting to be served, basic dishes first, then ascending
     * recipe id. Dishes of customers who walked out are left out.
     */
    fun getServableDishes(): List<Dish> {
        val cookedDishes = dishes.filter { it.status == DishStatus.COOKED && !it.abandoned }
        return cookedDishes.sortedWith(compareBy({ !it.isBasic }, { it.recipe.id }))
    }

    /** The dishes that have been served and are not eaten yet. */
    fun getServedDishes(): List<Dish> {
        val servedDishes = mutableListOf<Dish>()
        for (dish in dishes) {
            if (dish.status == DishStatus.SERVED) {
                servedDishes.add(dish)
            }
        }
        return servedDishes
    }

    /** The dishes no cook has started yet. */
    fun getUncookedDishes(): List<Dish> {
        val uncookedDishes = mutableListOf<Dish>()
        for (dish in dishes) {
            if (dish.status == DishStatus.UNCOOKED) {
                uncookedDishes.add(dish)
            }
        }
        return uncookedDishes
    }

    /** Whether every dish is cooked and none has been served yet. */
    fun areAllDishesCooked(): Boolean {
        for (dish in dishes) {
            if (dish.status != DishStatus.COOKED) {
                return false
            }
        }
        return true
    }

    /**
     * Whether the order is finished: every dish is eaten or aborted. Dishes of customers who
     * walked out are not waited for.
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

    /** Whether every dish is served or aborted, so the kitchen can drop the order. */
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

    /** How many of each dish were ordered, by dish name, for the ordering log. */
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
