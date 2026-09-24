package de.unisaarland.cs.se.selab.food
/**
 * Represents the supplier that has stock .
 * Interacts directly with the global [Stock] registry to evaluate ingredient availability
 * and manufactures packaged inventory units in discrete batch sizes determined by packaging volume.
 * @property stock the global [Stock] inventory registry tracking ingredient availability.
 */
class Supplier(
    val stock: Stock
) {
    // functions with logic
    /**
     * checks if an ingredient is in stock.
     */
    fun isAvailable(ingredient: Ingredient): Boolean {
        return stock.isIngredientAvailable(ingredient)
    }

    /**
     * Procures ingredient packages in discrete packaging units to satisfy a requested quantity.
     *
     * Produces newly sealed [IngredientPackage] instances until the cumulative volume meets
     * or exceeds the target amount. If the ingredient is marked unavailable or the requested
     * amount is non-positive, procurement immediately yields an empty list.
     *
     * @param ingredient the target ingredient to procure.
     * @param amount the minimum quantity of the ingredient required.
     * @return a list of freshly sealed [IngredientPackage] units fulfilling the request,
     * or an empty list if unavailable or if amount is less than or equal to 0.
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
