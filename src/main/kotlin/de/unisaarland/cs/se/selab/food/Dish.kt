package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.enums.DishStatus

class Dish(
    private val recipe: Recipe,
) {
    private val isBasic: Boolean = recipe.basicDishFor != null
    private var eatingProgress: Int = 2
    var status: DishStatus = DishStatus.UNCOOKED
}
