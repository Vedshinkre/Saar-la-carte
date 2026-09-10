package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.Time

class IngredientPackage(private val ingredient: Ingredient) {
    private var amount: Int = ingredient.packagingVolume
    private val expiryDate: Int = Time.evening + ingredient.bestBefore
    var isOpen: Boolean = false
        private set
}
