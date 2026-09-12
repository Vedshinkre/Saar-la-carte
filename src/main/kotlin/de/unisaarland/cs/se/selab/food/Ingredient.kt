package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.enums.MeasurementUnit

/**
 * Represents an ingredient used in a recipe and stored in pantries .
 */
class Ingredient(
    private val name: String,
    private val unit: MeasurementUnit,
    private val bestBefore: Int,
    private var packagingVolume: Int
) {
    // explicit getters for relevant functions
    /**
     * Explicit getter for the Ingredient Name .
     */
    fun getName(): String {
        return name
    }

    /**
     * Explicit getter for the Ingredient Unit .
     */
    fun getUnit(): MeasurementUnit {
        return unit
    }

    /**
     * Explicit getter for the Ingredient packagingVolume .
     */
    fun getPackagingVolume(): Int {
        return packagingVolume
    }

    /**
     * Explicit getter for the Ingredient Best Before Tick .
     */
    fun getBestBefore(): Int {
        return bestBefore
    }

    /**
     * Explicit setter for the Ingredient packagingVolume .
     */
    fun setPackagingVolume(newVolume: Int) {
        require(newVolume > 0) { "Packaging volume must be > 0." }
        packagingVolume = newVolume
    }
}
