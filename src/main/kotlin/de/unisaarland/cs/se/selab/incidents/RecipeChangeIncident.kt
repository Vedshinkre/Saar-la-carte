package incidents

import food.Ingredient
import food.Recipe
import types.Evening
import types.Id

class RecipeChangeIncident(
    private val id: Id,
    private val evening: Evening,
    private val ingredients: List<Ingredient>,
    private val adaptation: Int,
    private val recipes: List<Recipe>
) : Incident(id, evening)