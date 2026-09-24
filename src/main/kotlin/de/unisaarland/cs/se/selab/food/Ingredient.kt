package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.enums.MeasurementUnit

/**
 * Represents an ingredient used in recipes and stocked within restaurant pantries.
 *
 * Encapsulates the ingredient's measurement specifications, perishable shelf-life limit,
 * and current packaging capacity.
 *
 * @property name the unique name of the ingredient.
 * @property unit the physical unit of measurement .
 * @property bestBefore the duration in ticks after unpacking before the ingredient expires.
 * @param initialPackagingVolume the initial volume per package unit when acquired.
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
