package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe

class RecipeChangeIncident(
    private val id: Id,
    private val evening: Evening,
    private val ingredient: Ingredient,
    private val adaptation: Int,
    private val recipes: List<Recipe>
) : Incident(id, evening)
