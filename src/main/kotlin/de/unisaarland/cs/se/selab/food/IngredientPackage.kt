package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.Time

/**
 * Represents an Ingredient Package ie used to store ingredients in the pantry and is with the supplier .
 */
class IngredientPackage(
    val ingredient: Ingredient,
    currentAmount: Int,
    val expiryDate: Int,
    isOpen: Boolean
) {
    var currentAmount: Int = currentAmount
        private set

    var isOpen: Boolean = isOpen
        private set

    // explicit constructor with only ingredients
    constructor(ingredient: Ingredient) : this(
        ingredient = ingredient,
        currentAmount = ingredient.packagingVolume,
        expiryDate = Time.evening + ingredient.bestBefore,
        isOpen = false
    )

    // functions with logic
    /** tells if the ingredients is expired or not  */
    fun hasExpired(): Boolean {
        return Time.evening >= this.expiryDate
    }

    /** we remove quantity sized data from amount if it is possible  */
    fun removeAmount(quantity: Int): Int {
        // we remove quantity sized data from amount if it is possible ,
        // else return the amount of ingredients in this package
        if (quantity <= 0) {
            return 0
        }
        val actualRemoved =
            if (quantity > this.currentAmount) {
                // check if we can remove the requested quantity from the package, else make it empty
                this.currentAmount
            } else {
                quantity
            }

        this.currentAmount -= actualRemoved // reduce the relevant amount from the package

        if (actualRemoved > 0) {
            this.isOpen = true
        }

        return actualRemoved
    }
}
