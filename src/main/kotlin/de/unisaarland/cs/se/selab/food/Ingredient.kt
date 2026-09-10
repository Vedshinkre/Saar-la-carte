package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.enums.MeasurementUnit

class Ingredient(
    private val name: String, private val unit: MeasurementUnit, val bestBefore: Int, var packagingVolume: Int
)
