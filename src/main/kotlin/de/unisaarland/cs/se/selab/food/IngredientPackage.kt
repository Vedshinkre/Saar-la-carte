package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.Time
/**
 * Represents an Ingredient Package ie used to store ingredients in the pantry and is with the supplier .
 */
class IngredientPackage(
    private val ingredient: Ingredient,
    private var amount: Int,
    private val expiryDate: Int,
    private var isOpen: Boolean
) {

    // explicit constructor with only ingredients
    constructor(ingredient: Ingredient) : this(
        ingredient = ingredient,
        amount = ingredient.getPackagingVolume(),
        expiryDate = 1 + ingredient.getBestBefore(), // TODO ONCE TIME IS IMPLEMENTED
        isOpen = false
    )

    // explicit getters for relevant functions
    /** Explicit getter for the Ingredient in the Package */
    fun getIngredient(): Ingredient {
        return ingredient
    }

    /** Explicit getter to get amount of ingredient in the package */
    fun getCurrentAmount(): Int {
        return amount
    }

    /** Explicit getter to tell if the packet is open */
    fun isOpen(): Boolean {
        return isOpen
    }

    /** Explicit getter to get the expiry date of the Ingredient */
    fun getExpiryDate(): Int {
        return expiryDate
    }

    // functions with logic
    /** tells if the ingredients is expired or not  */
    fun hasExpired(): Boolean {
        return Time.evening >= this.expiryDate //  TODO ONCE TIME IS IMPLEMENTED
    }

    /** we remove quantity sized data from amount if it is possible  */
    fun removeAmount(quantity: Int): Int {
        // we remove quantity sized data from amount if it is possible ,
        // else return the amount of ingredients in this package
        if (quantity <= 0) {
            return 0
        }
        val actualRemoved =
            if (quantity > this.amount) {
                // check if we can remove the requested quantity from the package, else make it empty
                this.amount
            } else {
                quantity
            }

        this.amount -= actualRemoved // reduce the relevant amount from the package

        if (actualRemoved > 0) {
            this.isOpen = true
        }

        return actualRemoved
    }
}
