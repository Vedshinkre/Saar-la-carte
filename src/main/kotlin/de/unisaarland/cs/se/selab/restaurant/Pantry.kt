package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier

/**
 * Represents the pantry of the restaurant .
 */
class Pantry(
    private val inventory: MutableList<IngredientPackage>, private val supplier: Supplier
) {
    //explicit constructor with only stock used to create the supplier
    constructor(stock: Stock) : this(
        inventory = mutableListOf<IngredientPackage>(), supplier = Supplier(stock)
    )
    // functions with logic
    /**
     *   Check if we have ingredient in x amount in the pantry or not .
     */

    fun checkInventory(ingredient: Ingredient, amount: Int): Int {
        var totalAvailable = 0
        for (pkg in inventory) {
            // Only count packages that haven't expired and match the ingredient name
            val isExpired = pkg.hasExpired()
            val currentIngredient = pkg.getIngredient()
            if (currentIngredient.getName() == ingredient.getName() && !isExpired) {
                totalAvailable += pkg.getCurrentAmount()
            }
        }
        // Return the requested amount if we have enough, else return what's actually left
        if (totalAvailable >= amount) {
            return amount
        } else {
            return totalAvailable
        }
    }
    /**
     *  Return a list of all packages in the pantry matching the given ingredient.
     */

    fun getPackagesForIngredient(ingredient: Ingredient): List<IngredientPackage> {
        val matchingPackages = mutableListOf<IngredientPackage>()
        for (pkg in inventory) {
            val currentIngredient = pkg.getIngredient()
            if (currentIngredient.getName() == ingredient.getName()) {
                matchingPackages.add(pkg)
            }
        }
        return matchingPackages
    }
    /**
     *  Removes  packages that have expired based on the current evening.
     */
    fun throwExpiredIngredients(): Unit {

        val activePackages = mutableListOf<IngredientPackage>()

        for (pkg in inventory) {
            if (!pkg.hasExpired()) {
                activePackages.add(pkg) // Keep the ones that are still good
            }
        }

        // update the inventory
        inventory.clear()
        inventory.addAll(activePackages)
    }
    /**
     *  Check current Stock and procure new packages from the supplier if needed .
     */

    fun ensureQuantities(ingredientsToEnsure: Map<Ingredient, Int>) {

        throwExpiredIngredients()

        for ((ingredient, requiredAmount) in ingredientsToEnsure) {
            val currentAvailable = checkInventory(ingredient, requiredAmount)

            // If we don't have enough, calculate the deficit and buy more
            if (currentAvailable < requiredAmount) {
                val deficit = requiredAmount - currentAvailable
                val newPackages = supplier.procure(ingredient, deficit)

                // Add the newly procured packages to our pantry inventory
                for (pkg in newPackages) {
                    inventory.add(pkg)
                }
            }
        }
    }

    /**
     * Reserves a Single Ingredient based on its quantity.
     */
    // Needed this extra function to reduce complexity error by detekt
    private fun reserveSingleIngredient(ingredient: Ingredient, requiredAmount: Int) {
        var amountNeeded = requiredAmount

        // Filter out expired packages and grab only the packages matching this ingredient
        val matchingPackages = mutableListOf<IngredientPackage>()
        for (pkg in inventory) {
            val isExpired = pkg.hasExpired()
            val currentIngredient = pkg.getIngredient()
            if (!isExpired && currentIngredient.getName() == ingredient.getName()) {
                matchingPackages.add(pkg)
            }
        }

        // one for open packages, one for closed packages
        val openList = mutableListOf<IngredientPackage>()
        val closedList = mutableListOf<IngredientPackage>()

        // Sort them into their respective lists
        for (pkg in matchingPackages) {
            if (pkg.isOpen()) {
                openList.add(pkg)
            } else {
                closedList.add(pkg)
            }
        }

        // Sort each list by expiry date (earliest expiry first)
        openList.sortBy { pkg -> pkg.getExpiryDate() }
        closedList.sortBy { pkg -> pkg.getExpiryDate() }

        // Open packages first, then Closed packages
        matchingPackages.clear()
        matchingPackages.addAll(openList)
        matchingPackages.addAll(closedList)

        // Remove the amounts
        for (pkg in matchingPackages) {
            if (amountNeeded <= 0) {
                break
            }

            val removed = pkg.removeAmount(amountNeeded)
            amountNeeded -= removed
        }
    }
    /**
     * Remove desired amount of ingredients from pantry .
     */
    fun reserveIngredients(ingredientsToReserve: Map<Ingredient, Int>) {
        for ((ingredient, requiredAmount) in ingredientsToReserve) {
            reserveSingleIngredient(ingredient, requiredAmount)
        }
    }
}

