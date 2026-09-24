package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.Time

/**
 * Represents a physical package of an ingredient stored in a pantry or held by a supplier.
 *
 * Tracks the remaining ingredient volume, seal state, and expiry date relative
 * to the simulation's evening progression.
 *
 * @property ingredient the [Ingredient] type contained within this package.
 * @param currentAmount the  quantity of ingredient present in this package.
 * @property expiryDate the simulation evening on or after which the package is expired.
 * @param isOpen whether the package has been opened.
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
    /** tells if the ingredients is expired or not
     * * @return `true` if the current simulation evening is greater than or equal to [expiryDate], `false` otherwise.
     */
    fun hasExpired(): Boolean {
        return Time.evening >= this.expiryDate
    }

    /** we remove quantity sized data from amount if it is possible
     * @param quantity the desired amount of ingredient to extract.
     * @return the actual quantity successfully extracted from the package.
     */
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
