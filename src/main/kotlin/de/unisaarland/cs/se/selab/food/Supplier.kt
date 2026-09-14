package de.unisaarland.cs.se.selab.food
/**
 * Represents the supplier that has stock .
 */
class Supplier(
    private val stock: Stock
) {
    // functions with logic
    /**
     * checks if an ingredient is in stock.
     */
    fun isAvailable(ingredient: Ingredient): Boolean {
        return stock.isIngredientAvailable(ingredient)
    }

    /**
     * gets the ingredient package from the stock
     */
    fun procure(ingredient: Ingredient, amount: Int): List<IngredientPackage> {
        //  check if the ingredient is available in stock
        val currentStock = stock.isIngredientAvailable(ingredient)
        if (!currentStock) {
            return emptyList() // Cannot procure if ingredient is unavailable
        }

        // if noting is being procured
        if (amount <= 0) {
            return emptyList()
        }

        // Find the specific ingredient from stock(with safety check , just for the sake of it)
        val actualIngredient = stock.getIngredient(ingredient) ?: ingredient

        // Get the standard size of one single package for this ingredient
        val packageSize = actualIngredient.packagingVolume

        val procuredPackages = mutableListOf<IngredientPackage>()
        var accumulatedAmount = 0

        //  Keep adding full packages until we meet or exceed the requested amount
        while (accumulatedAmount < amount) {
            val newPackage = IngredientPackage(actualIngredient)
            procuredPackages.add(newPackage)

            // Add this package's full volume to the running total
            accumulatedAmount += packageSize
        }

        return procuredPackages
    }
}
