package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.enums.MeasurementUnit

/**
 * Represents an ingredient used in a recipe and stored in pantries .
 */
class Ingredient(
    val name: String,
    val unit: MeasurementUnit,
    val bestBefore: Int,
    initialPackagingVolume: Int
) {
    var packagingVolume: Int = initialPackagingVolume
        set(value) {
            require(value > 0) { "Packaging volume must be > 0." }
            field = value
        }
}
