package incidents

import Evening
import Id
import food.Ingredient
import food.Recipe

class RecipeChangeIncident(
	private val id: Id,
	private val evening: Evening,
	private val ingredients: List<Ingredient>,
	private val adaptation: Int,
	private val recipes: List<Recipe>
) : Incident(id, evening)