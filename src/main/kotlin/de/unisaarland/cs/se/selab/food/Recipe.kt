package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.RestaurantType

class Recipe(
    private val id: Id,
    private val name: String,
    private val duration: Int,
    private val cookTypes: List<CookType>,
    private val ingredients: MutableMap<Ingredient, Int>,
    val basicDishFor: RestaurantType?
)
