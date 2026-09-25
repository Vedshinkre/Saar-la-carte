package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.KitchenLogger

private const val EXEC = 1
private const val SOUS = 2
private const val TOURNANT = 3
private const val SAUCE = 4
private const val FISH = 5
private const val ROAST = 6
private const val VEGETABLE = 7
private const val PASTRY = 8
private const val CEIL_TOP = 9
private const val CEIL_DENOMINATOR = 10

/**
 * Manages kitchen operations, staff scheduling, recipe execution, and ingredient planning.
 *
 * Manages cook assignments based on qualification hierarchies and lowest-rank priority, batches
 * identical dishes across pending customer orders, reduces tick-by-tick cooking countdowns, coordinates
 * inventory replenishment with the [Pantry], and resets kitchen resources at the end of each evening.
 *
 * @property cooks the list of [Cook] staff employed in the kitchen.
 * @property pantry the restaurant's storage repository holding ingredient packages.
 * @property orderQueue the active queue of pending customer orders awaiting preparation or serving.
 * @property finishedNumberOfMeals the count of meals completed during the active evaluation step.
 * @property numberOfCookedMeals the cumulative count of successfully cooked meals, tracked for statistics.
 * @property restaurantType the culinary category of the restaurant, used to evaluate basic dish rules.
 */
class Kitchen(
    private var cooks: List<Cook>,
    private val pantry: Pantry,
    private val orderQueue: MutableList<Order>,
    private val readyDishes: MutableList<Dish>,
    private var finishedNumberOfMeals: Int,
    var numberOfCookedMeals: Int, // for statistics
    private var restaurantType: RestaurantType
) { // INTERNAL ATTRIBUTE: The ticket dispenser for Cook IDs
    private var nextAvailableCookId: Int = 1

    private val orderedAtByOrderId: MutableMap<Int, Int> = mutableMapOf()

    // explicit constructor with only list of cooks, pantry and orderQueue
    constructor(cooks: List<Cook>, pantry: Pantry, orderQueue: MutableList<Order>, restaurantType: RestaurantType) :
        this(
            cooks = cooks,
            pantry = pantry,
            orderQueue = orderQueue,
            restaurantType = restaurantType,
            readyDishes = mutableListOf(),
            finishedNumberOfMeals = 0,
            numberOfCookedMeals = 0
        )

    // helper functions:
    /**
     * Caches the placement tick of all currently queued orders to track preparation upon completion.
     */
    private fun rememberOrderTicks() {
        for (order in orderQueue) {
            orderedAtByOrderId[order.id] = order.orderedAt
        }
    }

    /**
     * Records the current simulation tick as the first-cooked timestamp on an order once any dish finishes cooking.
     */
    private fun recordFirstCookedTicks() {
        for (order in orderQueue) {
            if (order.firstDishCookedAt != null) {
                continue
            }
            if (order.dishes.any { it.status == DishStatus.COOKED }) {
                order.firstDishCookedAt = Time.tick
            }
        }
    }
    // sorting function
    /**
     * Sorts the cooks ONLY on the basis of their ids, null id is put at last .
     * @return the sorted list of cooks.
     */
    fun getCooksSorted(): List<Cook> {
        // copy  of mutable list of cooks
        val sortedCooks = mutableListOf<Cook>()
        sortedCooks.addAll(cooks)

        // sort cooks based on their ids in ascending orders, where ids don't exist we ut it in the end of the list
        sortedCooks.sortBy { cook ->
            val id = cook.id
            id ?: Int.MAX_VALUE
        }

        return sortedCooks
    }

    /**
     * Sorts the recipes first by basicness first and then by id  .
     * @param recipes the collection of candidate recipes to sort.
     * @return the ordered list of recipes with basic dishes prioritized.
     */
    fun sortRecipesByBasicnessAndId(recipes: List<Recipe>): List<Recipe> {
        val basicRecipes = mutableListOf<Recipe>()
        val normalRecipes = mutableListOf<Recipe>()

        //  Separate the recipes into two distinct lists
        for (recipe in recipes) {
            if (recipe.basicDishFor == this.restaurantType) {
                basicRecipes.add(recipe)
            } else {
                normalRecipes.add(recipe)
            }
        }

        //  Sort both lists individually by ID
        basicRecipes.sortBy { recipe -> recipe.id }

        normalRecipes.sortBy { recipe -> recipe.id }

        //  Combine them back together
        val finalSortedList = mutableListOf<Recipe>()

        //  basic dishes first
        for (recipe in basicRecipes) {
            finalSortedList.add(recipe)
        }

        //  normal dishes second
        for (recipe in normalRecipes) {
            finalSortedList.add(recipe)
        }

        return finalSortedList
    }

    /**
     * Reset everything at the end of the evening with the cooks.
     */
    // functions with logic
    fun resetKitchen() {
        while (orderQueue.isNotEmpty()) {
            val order = orderQueue.removeFirst()
            for (dish in order.dishes) {
                if (dish.status == DishStatus.SERVED || dish.status == DishStatus.EATEN) continue
                dish.status = DishStatus.ABORTED
            }
        }
        for (cook in cooks) {
            cook.id = null
            cook.orderId = null
            cook.currentRecipe = null
            cook.setRemainingTicks(0)
            cook.isCooking = false
        }
        nextAvailableCookId = 1
        orderedAtByOrderId.clear()
    }

    /**
     * Generate ids for the cooks that don't have one already.
     * @return the dispensed numeric ID.
     */
    fun getNextCookId(): Int {
        val allocatedId = nextAvailableCookId
        nextAvailableCookId += 1
        return allocatedId
    }

    /**
     * Extracts unique recipes from a list of dishes, deduplicated by recipe name.
     *
     * @param currentDishes the list of dishes to inspect.
     * @return a list containing distinct [Recipe] instances.
     */
    fun getUniqueRecipes(currentDishes: List<Dish>): List<Recipe> {
        val uniqueRecipes = mutableListOf<Recipe>()

        for (dish in currentDishes) {
            val currentRecipe = dish.recipe
            var isAlreadyAdded = false

            // check if we already have this recipe in our list
            for (item in uniqueRecipes) {
                if (item.name == currentRecipe.name) {
                    isAlreadyAdded = true
                    break // Stop searching as we already have this recipe
                }
            }

            // If it was not found in the list, add it
            if (!isAlreadyAdded) {
                uniqueRecipes.add(currentRecipe)
            }
        }

        return uniqueRecipes
    }

    /**
     * Counts the total number of dishes across all queued orders that are fully cooked and ready to be served.
     * @return the number of cooked, servable dishes.
     */
    fun getServableDishesNumber(): Int {
        var servableCount = 0

        // check in each order in kitchen
        for (order in orderQueue) {
            // Check every dish within that order
            for (dish in order.dishes) {
                if (dish.status == DishStatus.COOKED) {
                    servableCount += 1
                }
            }
        }

        return servableCount
    }

    /**
     * Selects the most appropriate available cook to prepare the given recipe.
     *
     * Filters for idle cooks qualified for the recipe, selects the lowest-ranking candidate (tie-breaking
     * by lowest ID), and assigns a new cook ID if the selected cook lacks one.
     *(kept the tie breaking consistent with the project specification, although the server seems to do it vice versa)
     * @param recipe the recipe needing preparation.
     * @return the chosen [Cook], or `null` if no qualified cook is currently free.
     */
    fun chooseCook(recipe: Recipe): Cook? {
        val eligibleCooks = mutableListOf<Cook>()

        // get all eligible cooks
        for (cook in cooks) {
            if (isCookEligible(cook, recipe)) {
                eligibleCooks.add(cook)
            }
        }

        // we return null if no cooks are free
        if (eligibleCooks.isEmpty()) {
            return null
        }

        // get the lowest ranking cook
        val chosenCook = findLowestRankingCook(eligibleCooks)

        //  assign an ID if none exists
        if (chosenCook.id == null) {
            chosenCook.setId(getNextCookId())
        }

        return chosenCook
    }

    /**
     * Maps a [CookType] to an integer rank value where
     * 1 is highest ([CookType.EXEC]) and 8 is lowest ([CookType.PASTRY]).
     * @param type the cook role to rank.
     * @return the numeric rank value.
     */
    private fun getNumericalRank(type: CookType): Int {
        return when (type) {
            CookType.EXEC -> EXEC
            CookType.SOUS -> SOUS
            CookType.TOURNANT -> TOURNANT
            CookType.SAUCE -> SAUCE
            CookType.FISH -> FISH
            CookType.ROAST -> ROAST
            CookType.VEGETABLE -> VEGETABLE
            CookType.PASTRY -> PASTRY
        }
    }

    /**
     * Helper to check if a cook is free and allowed to cook the recipe.
     *  @param cook the cook being evaluated.
     *  @param recipe the target recipe.
     *  @return `true` if the cook is not cooking and their role is included in [Recipe.cookType], `false` otherwise.
     */
    private fun isCookEligible(cook: Cook, recipe: Recipe): Boolean {
        // If they are already cooking, they are not eligible
        if (cook.isCooking) {
            return false
        }
        val cookType = cook.type
        // Check if this cook's type is in the recipe's allowed list
        for (allowedType in recipe.cookType) {
            if (cookType == allowedType) {
                return true
            }
        }

        return false
    }

    /**
     * Locates the lowest-ranking cook among eligible candidates, resolving ties by lowest ID.
     *
     * @param eligibleCooks the non-empty list of available and qualified cooks.
     * @return the cook with the lowest rank (highest numerical value) and lowest ID.
     */
    private fun findLowestRankingCook(eligibleCooks: List<Cook>): Cook {
        var chosenCook = eligibleCooks[0]
        var lowestRankValue = getNumericalRank(chosenCook.type)

        for (i in 1 until eligibleCooks.size) {
            val currentCook = eligibleCooks[i]
            val currentRankValue = getNumericalRank(currentCook.type)

            // If the current cook has a lower rank we choose them
            if (currentRankValue > lowestRankValue) {
                chosenCook = currentCook
                lowestRankValue = currentRankValue
            }
            // tie-break by lowest ID for same rank
            else if (currentRankValue == lowestRankValue) {
                val chosenId = chosenCook.id
                val currentId = currentCook.id

                // chefs can be without ids as well
                val safeChosenId = chosenId ?: Int.MAX_VALUE
                val safeCurrentId = currentId ?: Int.MAX_VALUE

                if (safeCurrentId < safeChosenId) {
                    chosenCook = currentCook
                    lowestRankValue = currentRankValue
                }
            }
        }
        return chosenCook
    }

    /**
     * Collects all uncooked dishes from an order that match the given recipe.
     *
     * @param recipe the recipe blueprint to match against.
     * @param order the order whose dishes are inspected.
     * @return a pair containing the order ID and the list of matching uncooked dishes.
     */
    fun collectRecipes(recipe: Recipe, order: Order): Pair<Int, List<Dish>> {
        val matchingDishes = mutableListOf<Dish>()

        // Iterate through all dishes in the order
        for (dish in order.getUncookedDishes()) { // CHANGE: we get uncooked dishes now, not getdishes()
            // Match the recipe by its unique ID
            val dishRecipe = dish.recipe
            if (dishRecipe.id == recipe.id) {
                matchingDishes.add(dish)
            }
        }

        return Pair(order.id, matchingDishes)
    }

    /**
     * Constructs a ingredient shopping list for the upcoming evening.
     *
     * Aggregates ingredient amounts from historical/pre-ordered dishes and calculates estimated
     * ingredient requirements for remaining front-of-house capacity using ceiling integer division.
     *
     * @param ingredientsFromHistory a map of recipes to pre-ordered/historical demand counts.
     * @param frontCapacity the remaining guest capacity in front of house.
     * @param menu the collection of available recipes on the restaurant menu.
     * @return a map specifying total required ingredient quantities.
     */

    fun createShoppingList(
        ingredientsFromHistory: Map<Recipe, Int>,
        frontCapacity: Int,
        menu: List<Recipe>
    ): Map<Ingredient, Int> {
        val shoppingList = mutableMapOf<Ingredient, Int>()

        //  ingredients from History of orders (regulars & events)
        for ((recipe, count) in ingredientsFromHistory) {
            addRecipeToShoppingList(recipe, count, shoppingList)
        }

        // estimated ingredients for remaining front of house seats
        val estimatedVariable = (frontCapacity + CEIL_TOP) / CEIL_DENOMINATOR
        // can also be simulated by ceil function,but need Int

        if (estimatedVariable > 0) {
            for (recipe in menu) {
                addRecipeToShoppingList(recipe, estimatedVariable, shoppingList)
            }
        }

        return shoppingList
    }

    /**
     * Helper function to multiply and add ingredients to the map for dishes from order history(detekt tests).
     * @param recipe the recipe whose ingredients are being added.
     * @param multiplier the number of portions required.
     * @param shoppingList the mutable ingredient accumulator map.
     */
    private fun addRecipeToShoppingList(
        recipe: Recipe,
        multiplier: Int,
        shoppingList: MutableMap<Ingredient, Int>
    ) {
        val recipeIngredients = recipe.ingredients

        for ((ingredient, amount) in recipeIngredients) {
            val amountToAdd = amount * multiplier

            if (shoppingList.containsKey(ingredient)) {
                val currentTotal = shoppingList[ingredient] ?: 0
                shoppingList[ingredient] = currentTotal + amountToAdd
            } else {
                // ingredient not in map yet, so we add amount
                shoppingList[ingredient] = amountToAdd
            }
        }
    }

    /**
     * Plans and procures the ingredients required for the upcoming evening.
     * Discards expired pantry stock, totals historical demand from regular guests and event favorites,
     * builds the final shopping list including capacity estimates, and delegates inventory procurement to the [Pantry].
     * @param orderHistory previously known orders from regular groups.
     * @param frontCapacity remaining guest capacity in front of house.
     * @param menu the current active menu recipes.
     * @param eventGroupFavDishes pre-ordered favorite recipes and customer counts for event groups.
     */
    fun planForIngredients(
        orderHistory: List<Order>,
        frontCapacity: Int,
        menu: List<Recipe>,
        eventGroupFavDishes: List<Pair<Recipe, Int>>
    ) {
        //  Throw all expired ingredients from the pantry
        pantry.throwExpiredIngredients()

        // Calculate known order history from Regulars and Events
        val knownOrderHistory = mutableMapOf<Recipe, Int>()

        // Add history from Regular groups
        for (order in orderHistory) {
            for (dish in order.dishes) {
                val recipe = dish.recipe

                if (knownOrderHistory.containsKey(recipe)) {
                    val currentCount = knownOrderHistory[recipe] ?: 0
                    knownOrderHistory[recipe] = currentCount + 1
                } else {
                    knownOrderHistory[recipe] = 1
                }
            }
        }

        // pre-ordered favorite dishes from Event groups
        for (eventPair in eventGroupFavDishes) {
            val recipe = eventPair.first
            val numberOfCustomers = eventPair.second

            if (knownOrderHistory.containsKey(recipe)) {
                val currentCount = knownOrderHistory[recipe] ?: 0
                knownOrderHistory[recipe] = currentCount + numberOfCustomers
            } else {
                knownOrderHistory[recipe] = numberOfCustomers
            }
        }

        //  final shopping list
        val shoppingList = createShoppingList(knownOrderHistory, frontCapacity, menu)

        //  the pantry checks inventory and procure missing ingredients
        pantry.ensureQuantities(shoppingList)
    }

    /**
     * Processes all cooking activities for the current tick.
     */
    fun processCooking() {
        rememberOrderTicks()
        cleanOrderQueue()
        assignRecipesToCooks()
        executeCookingStep()
        recordFirstCookedTicks()
    }

    /**
     * Removes orders that are fully served or aborted.
     */
    private fun cleanOrderQueue() {
        // loop through the order queue
        var i = orderQueue.size - 1
        while (i >= 0) {
            val currentOrder = orderQueue[i]
            if (currentOrder.areAllDishesServedOrAborted()) {
                orderQueue.removeAt(i)
            }
            i--
        }
    }

    /**
     * Scans pending orders in the queue and assigns their uncooked dishes to qualified idle cooks.
     * For each order, recipes are sorted prioritizing basic dishes, and each recipe is assigned
     * to the lowest-ranking qualified cook available.
     */
    private fun assignRecipesToCooks() {
        // get all uncooked dishes from the orders in the order queue
        for (order in orderQueue) {
            // Extract unique recipes strictly from this order's remaining uncooked dishes.
            val uncookedDishesForOrder = order.getUncookedDishes()
            if (uncookedDishesForOrder.isEmpty()) {
                continue
            }

            val uniqueRecipes = getUniqueRecipes(uncookedDishesForOrder)

            //  within this order: basic dishes first, then lower recipe id
            val sortedRecipes = sortRecipesByBasicnessAndId(uniqueRecipes)

            // Assign each recipe in this order to an eligible cook
            for (recipe in sortedRecipes) {
                val chosenCook = chooseCook(recipe)

                if (chosenCook != null) {
                    findDishesForRecipe(recipe, chosenCook)
                }
            }
        }
    }

    /**
     * Aggregates all matching uncooked dishes for a recipe across the entire order queue
     * and dispatches them to the cook.
     * @param recipe the recipe to prepare.
     * @param cook the assigned cook.
     */
    private fun findDishesForRecipe(recipe: Recipe, cook: Cook) {
        val allDishes = mutableListOf<Dish>()
        val allOrderIds = mutableListOf<Int>()
        var baseOrderId = -1
        val dishesByOrder = mutableMapOf<Int, List<Dish>>()
        for (order in orderQueue) {
            val (orderId, matchingDishes) = collectRecipes(recipe, order)

            if (matchingDishes.isNotEmpty()) {
                for (dish in matchingDishes) {
                    allDishes.add(dish)
                }
                allOrderIds.add(orderId)
                dishesByOrder[orderId] = matchingDishes

                // The first order we find becomes the base order ID
                if (baseOrderId == -1) {
                    baseOrderId = orderId
                }
            }
        }

        if (allDishes.isNotEmpty()) {
            cook.startCooking(recipe, allDishes, baseOrderId, dishesByOrder)
            val cookIDCurrent = cook.id ?: -1
            val cookTypeCurrent = cook.type.name
            val curDishName = recipe.name
            KitchenLogger.logKitchenDishAssignment(
                cookId = cookIDCurrent,
                cookType = cookTypeCurrent,
                numberOfMeals = allDishes.size,
                dishName = curDishName,
                baseOrderId = baseOrderId,
                allOrderIds = allOrderIds.sorted()
            )
        }
    }

    /**
     * Tells all cooks to cook for the tick and accumulates their statistics.
     */
    private fun executeCookingStep() {
        var activeCooks = 0
        var totalMeals = 0
        var finishedMeals = 0

        val sortedCooks = getCooksSorted()

        for (cook in sortedCooks) {
            // capture the dish/order being cooked before cookDishes() clears it on completion
            val dishNameBeforeCooking = cook.currentRecipe?.name
            val baseOrderIdBeforeCooking = cook.orderId

            // access the output of cookDishes()
            val (chefWasActive, totalAssigned, finished) = cook.cookDishes()

            if (chefWasActive) {
                activeCooks += 1
            }
            totalMeals += totalAssigned
            finishedMeals += finished
            numberOfCookedMeals += finished
            if (finished > 0) {
                logFinishedMeals(cook, finished, dishNameBeforeCooking, baseOrderIdBeforeCooking)
            }
        }

        val servableMeals = getServableDishesNumber()
        KitchenLogger.logKitchenStatus(
            numberOfCooks = activeCooks,
            totalNumberOfMeals = totalMeals,
            finishedNumberOfMeals = finishedMeals,
            servableMeals = servableMeals
        )
    }

    /**
     * Helper function to find the base order and log finished meals (to solve detekt issue).
     * @param cook the cook who completed the dish batch.
     * @param finished the number of finished meals.
     * @param dishNameBeforeCooking the name of the prepared dish captured prior to state reset.
     * @param baseOrderId the initiating order ID captured prior to state reset.
     */
    private fun logFinishedMeals(cook: Cook, finished: Int, dishNameBeforeCooking: String?, baseOrderId: Int?) {
        val cookId = cook.id ?: -1
        val dishName = dishNameBeforeCooking ?: "Unknown Dish"

        // get the base order
        val orderedAt = orderedAtByOrderId[baseOrderId]
        val cookDurationTick = if (orderedAt != null) Time.tick - orderedAt else 0

        KitchenLogger.logKitchenMealCooked(
            cookId = cookId,
            numberOfMeals = finished,
            dishName = dishName,
            cookDurationTick = cookDurationTick
        )
    }
}
