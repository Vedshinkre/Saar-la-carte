package de.unisaarland.cs.se.selab.parsers

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import kotlinx.serialization.json.*

/**
 * Parses Ingrdients and Recipes  .
 */
class FoodParser {
    fun parseFood(foodJson: JsonObject): Pair<List<Ingredient>, List<Recipe>>? {
        try {
            // Parse Ingredients
            val ingredientsArray = foodJson["ingredients"]?.jsonArray
                ?: throw IllegalArgumentException("Missing 'ingredients' array in JSON")

            val ingredientsList = parseIngredients(ingredientsArray)

            // Parse Recipes
            val recipesArray = foodJson["recipes"]?.jsonArray
                ?: throw IllegalArgumentException("Missing 'recipes' array in JSON")

            val recipesList = parseRecipes(recipesArray, ingredientsList)

            return Pair(ingredientsList, recipesList)
        } catch (e: IllegalArgumentException) {
            println("Logical Validation Error: ${e.message}")
            return null
        } catch (e: Exception) {
            println("Unexpected Error: ${e.message}")
            return null
        }
    }

    private fun parseIngredients(array: JsonArray): List<Ingredient> {
        val parsedIngredients = mutableListOf<Ingredient>()

        //  Must exist at least one ingredient
        if (array.isEmpty()) {
            throw IllegalArgumentException("The ingredients list cannot be empty.")
        }

        // Loop through every item
        for (i in 0 until array.size) {
            val ingredientJson = array[i].jsonObject

            //  Extract values
            val name = ingredientJson["name"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Ingredient is missing a 'name'")

            val unitStr = ingredientJson["unit"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Ingredient '$name' is missing a 'unit'")

            val packagingVolume = ingredientJson["packagingVolume"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("Ingredient '$name' is missing 'packagingVolume'")

            val bestBefore = ingredientJson["bestBefore"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("Ingredient '$name' is missing 'bestBefore'")

            // check uniqueness
            val isUnique = checkUniquenessOfIngredient(name, parsedIngredients)
            if (!isUnique) {
                throw IllegalArgumentException("Duplicate ingredient name found: $name")
            }

            // Convert to Enum
            val unitEnum = MeasurementUnit.valueOf(unitStr)

            //  parse the ingredient
            val newIngredient = Ingredient(name, unitEnum, packagingVolume, bestBefore)
            val ingredientGood = validateIngredient(newIngredient)
            // validateIngredient
            if (!ingredientGood) {
                throw IllegalArgumentException("Ingredient $name failed logical validation.")
            }
            parsedIngredients.add(newIngredient)
        }

        return parsedIngredients
    }

    // cross validation function
    private fun validateIngredient(ingredient: Ingredient): Boolean {
        //  Packaging volume must be greater than 0
        if (ingredient.getPackagingVolume() <= 0) {
            return false
        }

        //  Best before must be greater than 0
        if (ingredient.getBestBefore() <= 0) {
            return false
        }
        // if all checks pass
        return true
    }

    // check Uniqueness
    private fun checkUniquenessOfIngredient(name: String, parsedIngredients: List<Ingredient>): Boolean {
        for (existingIng in parsedIngredients) {
            if (existingIng.getName() == name) {
                return false
            }
        }
        return true
    }

    private fun parseRecipes(array: JsonArray, availableIngredients: List<Ingredient>): List<Recipe> {
        val parsedRecipes = mutableListOf<Recipe>()

        for (i in 0 until array.size) {
            val restaurantJson = array[i].jsonObject

            //  Extract values
            val id = restaurantJson["id"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("Recipe is missing an 'id'")

            val name = restaurantJson["name"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Recipe '$id' is missing a 'name'")

            val duration = restaurantJson["duration"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("Recipe '$name' is missing 'duration'")

            //  basic dish (can be null as well)
            var basicDishFor: RestaurantType? = null
            if (restaurantJson.containsKey("basicDishFor")) {
                val restaurantTypeStr = restaurantJson["basicDishFor"]?.jsonPrimitive?.content
                if (restaurantTypeStr != null) {
                    basicDishFor = RestaurantType.valueOf(restaurantTypeStr)
                }
            }
            val isBasicDish = basicDishFor != null

            //  check uniqueness
            val isUnique = checkUniquenessOfRecipe(id, name, isBasicDish, parsedRecipes)
            if (!isUnique) {
                throw IllegalArgumentException(
                    "Recipe failed uniqueness check (Duplicate ID or conflicting basic dish name): $id / $name"
                )
            }

            // cook types array
            val cookTypesArray = restaurantJson["cookType"]?.jsonArray
                ?: throw IllegalArgumentException("Recipe '$name' is missing 'cookType'")

            val allowedCooks = mutableListOf<CookType>()
            for (j in 0 until cookTypesArray.size) {
                val cookTypeStr = cookTypesArray[j].jsonPrimitive.content
                allowedCooks.add(CookType.valueOf(cookTypeStr))
            }

            // Ingredients Array
            val recipeIngredientsArray = restaurantJson["ingredients"]?.jsonArray
                ?: throw IllegalArgumentException("Recipe '$name' is missing 'ingredients'")

            val recipeIngredientsMap = mutableMapOf<Ingredient, Int>()

            for (k in 0 until recipeIngredientsArray.size) {
                val recipeIngredientJson = recipeIngredientsArray[k].jsonObject

                val recipeIngredientName = recipeIngredientJson["name"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Recipe ingredient missing 'name'")
                val recipeIngredientUnitStr = recipeIngredientJson["unit"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Recipe ingredient missing 'unit'")
                val recipeIngredientAmount = recipeIngredientJson["amount"]?.jsonPrimitive?.int
                    ?: throw IllegalArgumentException("Recipe ingredient missing 'amount'")

                // --- THE CROSS VALIDATION ---
                if (recipeIngredientAmount <= 0) {
                    throw IllegalArgumentException("Recipe ingredient amount must be greater than 0.")
                }

                // Convert string unit to MeasurementUnit enum
                val recipeIngredientUnit = MeasurementUnit.valueOf(recipeIngredientUnitStr)

                // get the ingredient from the available ingredients, if  it exists
                val foundIngredient = checkIngredientExists(
                    recipeIngredientName,
                    recipeIngredientUnit,
                    availableIngredients
                )

                // ingredient for the recipe not availabe
                if (foundIngredient == null) {
                    throw IllegalArgumentException(
                        "Recipe '$name' requires '$recipeIngredientName', but it does not exist."
                    )
                }

                // add valid ingredient and amount to the map
                recipeIngredientsMap[foundIngredient] = recipeIngredientAmount
            }

            // parse the recipe and add to the list
            val newRecipe = Recipe(id, name, duration, allowedCooks, recipeIngredientsMap, basicDishFor)
            var isRecipeValid = validateRecipe(newRecipe)

            if (!isRecipeValid) {
                throw IllegalArgumentException("Recipe $name failed logical validation (duration out of bounds).")
            }

            parsedRecipes.add(newRecipe)
        }

        return parsedRecipes
    }

    private fun validateRecipe(recipe: Recipe): Boolean {
        val duration = recipe.getDuration()

        //  Duration must be 2 <= duration && duration <= 40
        if (duration < 2 || duration > 40) {
            return false
        }

        return true
    }

    private fun checkUniquenessOfRecipe(
        id: Int,
        name: String,
        isBasicDish: Boolean,
        parsedRecipes: List<Recipe>
    ): Boolean {
        for (existingRec in parsedRecipes) {
            // duplicate ID check
            if (existingRec.getId() == id) {
                return false
            }

            //  There must exist exactly 1 default recipe per basic dish name
            val isExistingBasic = existingRec.getBasicDishFor() != null
            if (isBasicDish && isExistingBasic) {
                if (existingRec.getName() == name) {
                    return false // two different recipes have the same name and both claim to be basic recipe
                }
            }
        }

        // completely unique
        return true
    }

    private fun checkIngredientExists(
        name: String,
        unit: MeasurementUnit,
        availableIngredients: List<Ingredient>
    ): Ingredient? {
        for (ing in availableIngredients) {
            if (ing.getName() == name && ing.getUnit() == unit) {
                return ing
            }
        }
        return null
    }
}
