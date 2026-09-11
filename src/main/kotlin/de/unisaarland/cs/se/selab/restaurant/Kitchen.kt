package de.unisaarland.cs.se.selab.restaurant


import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Dish
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe


private const val EXEC = 1
private const val SOUS = 2
private const val TOURNANT = 3
private const val SAUCE = 4
private const val FISH = 5
private const val ROAST = 6
private const val VEGETABLE = 7
private const val PASTRY = 8

/**
 * Kitchen class where all the cooking and chef handling happens .
 */
class Kitchen(
    private var cooks: List<Cook>,
    private val pantry: Pantry,
    private val orderQueue: MutableList<Order>,
    private val readyDishes: MutableList<Dish>,
    private var finishedNumberOfMeals: Int,
    private var numberOfCookedMeals: Int,
    private var restaurantType: RestaurantType
){  // INTERNAL ATTRIBUTE: The ticket dispenser for Cook IDs
    private var nextAvailableCookId: Int = 1

    //explicit constructor with only list of cooks, pantry and orderQueue
    constructor(cooks: List<Cook>, pantry: Pantry, orderQueue: MutableList<Order>,restaurantType: RestaurantType) :
            this(
                cooks = cooks,
                pantry = pantry,
                orderQueue = orderQueue,
                restaurantType = restaurantType,
                readyDishes = mutableListOf(),
                finishedNumberOfMeals = 0,
                numberOfCookedMeals = 0
            )
    // sorting function
    /**
     * Sorts the cooks ONLY on the basis of their ids, null id is put at last .
     */
    fun getCooksSorted(): List<Cook> {
        // copy  of mutable list of cooks
        val sortedCooks = mutableListOf<Cook>()
        sortedCooks.addAll(cooks)

        // sort cooks based on their ids in ascending orders, where ids don't exist we ut it in the end of the list
        sortedCooks.sortBy { cook ->
            val id = cook.getId()
            id ?: Int.MAX_VALUE
        }

        return sortedCooks
    }
    /**
     * Sorts the recipes first by basicness first and then by id  .
     */
    fun sortRecipesByBasicnessAndId(recipes: List<Recipe>): List<Recipe> {
        val basicRecipes = mutableListOf<Recipe>()
        val normalRecipes = mutableListOf<Recipe>()

        //  Separate the recipes into two distinct lists
        for (recipe in recipes) {
            if (recipe.getBasicDishFor() == this.restaurantType) {
                basicRecipes.add(recipe)
            } else {
                normalRecipes.add(recipe)
            }
        }

        //  Sort both lists individually by ID
        basicRecipes.sortBy { recipe -> recipe.getId() }

        normalRecipes.sortBy { recipe -> recipe.getId() }

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
     * Reset everything in the end of the evening with the cooks   .
     */
    // functions with logic
    fun resetCooks():Unit{
        // TODO skerdi reset everything in each cook at the end of the evening
    }

    /**
     * Generate ids for the cooks that don't have one already  .
     */
    fun getNextCookId(): Int {
        val allocatedId = nextAvailableCookId
        nextAvailableCookId += 1
        return allocatedId
    }

    /**
     * gets unique recipes from the dishes that can be cooked  .
     */
    fun getUniqueRecipes(currentDishes: List<Dish>): List<Recipe> {
        val uniqueRecipes = mutableListOf<Recipe>()

        for (dish in currentDishes) {
            val currentRecipe = dish.getRecipe()
            var isAlreadyAdded = false

            // check if we already have this recipe in our list
            for (item in uniqueRecipes) {

                if (item.getName() == currentRecipe.getName()) {
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
     * gets number of servable dishes in that tick .
     */
    fun getServableDishesNumber(): Int {
        var servableCount = 0

        // check in each order in kitchen
        for (order in orderQueue) {
            // Check every dish within that order
            for (dish in order.getDishes()) { // TODO WAITING FOR ORDER IMPLEMENTATION
                if (dish.getStatus() == DishStatus.COOKED) {
                    servableCount += 1
                }
            }
        }

        return servableCount
    }
    /**
     * Choose the perfect cook for a given recipe.
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
        if (chosenCook.getId() == null) {
            chosenCook.setId(getNextCookId())
        }

        return chosenCook
    }
    /**
     * Gives every cook type a rank for selection purposes.
     */
    // Helper to give the cook types rank (1 is highest rank, 8 is lowest rank)
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
     * Helper to check if a cook is free and allowed to cook the recipe(needed due to detekt tests).
     */
    private fun isCookEligible(cook: Cook, recipe: Recipe): Boolean {
        // If they are already cooking, they are not eligible
        if (cook.getIsCooking()) {
            return false
        }
        val cookType = cook.getCookType()
        // Check if this cook's type is in the recipe's allowed list
        for (allowedType in recipe.getCookTypes()) {
            if (cookType == allowedType) {
                return true
            }
        }

        return false
    }

    /**
     * Helper function to find the lowest ranking eligible cook(needed due to detekt tests) .
     */
    private fun findLowestRankingCook(eligibleCooks: List<Cook>): Cook {
        var chosenCook = eligibleCooks[0]
        var lowestRankValue = getNumericalRank(chosenCook.getCookType())

        for (i in 1 until eligibleCooks.size) {
            val currentCook = eligibleCooks[i]
            val currentRankValue = getNumericalRank(currentCook.getCookType())

            // If the current cook has a lower rank we choose them
            if (currentRankValue > lowestRankValue) {
                chosenCook = currentCook
                lowestRankValue = currentRankValue
            }
            // tie-break by lowest ID for same rank
            else if (currentRankValue == lowestRankValue) {
                val chosenId = chosenCook.getId()
                val currentId = currentCook.getId()

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


}