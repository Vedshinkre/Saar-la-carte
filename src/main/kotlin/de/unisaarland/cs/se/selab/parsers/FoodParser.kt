package de.unisaarland.cs.se.selab.parsers

import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val KEY_NAME = "name"
private const val KEY_DISH_NAME = "dishName"
private const val KEY_UNIT = "unit"
private const val MIN_DURATION = 2
private const val MAX_DURATION = 40

/**
 * Parses Ingredients and Recipes  .
 */
class FoodParser {
    /**
     * Function to call Parse Ingredients and Recipes accordingly   .
     */
    fun parse(ingredientsArray: JsonArray, recipesArray: JsonArray): Pair<List<Ingredient>, List<Recipe>> {
        // Parse Ingredients
        val ingredientsList = parseIngredients(ingredientsArray)

        // Parse Recipes
        val recipesList = parseRecipes(recipesArray, ingredientsList)

        return Pair(ingredientsList, recipesList)
    }

    /**
     * Function to cParse Ingredients    .
     */
    private fun parseIngredients(array: JsonArray): List<Ingredient> {
        val parsedIngredients = mutableListOf<Ingredient>()

        //  Must exist at least one ingredient
        // require(!array.isEmpty()) { "The ingredients list cannot be empty." }

        // Loop through every item
        for (i in 0 until array.size) {
            val ingredientJson = array[i].jsonObject

            //  Extract values
            val name = ingredientJson[KEY_NAME]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Ingredient is missing a 'name'")

            val unitStr = ingredientJson[KEY_UNIT]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Ingredient '$name' is missing a 'unit'")

            val packagingVolume = ingredientJson["packagingVolume"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("Ingredient '$name' is missing 'packagingVolume'")

            val bestBefore = ingredientJson["bestBefore"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("Ingredient '$name' is missing 'bestBefore'")

            // check uniqueness
            val isUnique = checkUniquenessOfIngredient(name, parsedIngredients)
            require(isUnique) { "Duplicate ingredient name found: $name" }

            // Convert to Enum
            val unitEnum = MeasurementUnit.valueOf(unitStr.uppercase())

            //  parse the ingredient
            val newIngredient = Ingredient(name, unitEnum, bestBefore, packagingVolume)
            // maybe initial packaging volume is becoming too strict
            val ingredientGood = validateIngredient(newIngredient)
            // validateIngredient
            require(ingredientGood) { "Ingredient $name failed logical validation." }

            parsedIngredients.add(newIngredient)
        }

        return parsedIngredients
    }

    /**
     * Function to cross validate Ingredient    .
     */
    // cross validation function
    private fun validateIngredient(ingredient: Ingredient): Boolean {
        //  Packaging volume must be greater than 0
        if (ingredient.packagingVolume <= 0) {
            return false
        }

        //  Best before must be greater than 0
        if (ingredient.bestBefore <= 0) {
            return false
        }
        // if all checks pass
        return true
    }

    /**
     * Function to check uniqueness of Ingredient    .
     */
    // check Uniqueness
    private fun checkUniquenessOfIngredient(name: String, parsedIngredients: List<Ingredient>): Boolean {
        for (existingIng in parsedIngredients) {
            if (existingIng.name == name) {
                return false
            }
        }
        return true
    }

    /**
     * Function to parse recipes .
     */
    private fun parseRecipes(array: JsonArray, availableIngredients: List<Ingredient>): List<Recipe> {
        val parsedRecipes = mutableListOf<Recipe>()

        for (i in 0 until array.size) {
            val recipeJson = array[i].jsonObject

            // Extract values
            val id = recipeJson["id"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("Recipe is missing an 'id'")
            val name = recipeJson[KEY_DISH_NAME]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Recipe '$id' is missing a 'dishName'")
            val duration = recipeJson["duration"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("Recipe '$name' is missing 'duration'")

            // basic dish (can be null as well)
            var basicDishFor: RestaurantType? = null
            if (recipeJson.containsKey("basicDishFor")) {
                val restaurantTypeStr = recipeJson["basicDishFor"]?.jsonPrimitive?.content
                if (restaurantTypeStr != null) {
                    basicDishFor = RestaurantType.valueOf(restaurantTypeStr)
                }
            }
            val isBasicDish = basicDishFor != null

            // check uniqueness
            val isUnique = checkUniquenessOfRecipe(id, name, isBasicDish, parsedRecipes)
            require(isUnique) { "Recipe failed uniqueness check (Duplicate ID or conflicting basic dish name)" }

            // cook types array
            // maybe I need to convert the cooktype to enum of cooktype , like I did with unit enm
            val cookTypesArray = recipeJson["cookType"]?.jsonArray
                ?: throw IllegalArgumentException("Recipe '$name' is missing 'cookType'")

            // NEW
            // require(cookTypesArray.isNotEmpty()) { "Recipe '$name' must allow at least one cook type." }

            val allowedCooks = mutableListOf<CookType>()
            for (j in 0 until cookTypesArray.size) {
                val cookTypeStr = cookTypesArray[j].jsonPrimitive.content
                allowedCooks.add(CookType.valueOf(cookTypeStr))
            }

            // Ingredients Array
            val recipeIngredientsArray = recipeJson["ingredients"]?.jsonArray
                ?: throw IllegalArgumentException("Recipe '$name' is missing 'ingredients'")

            // NEW
            //  require(recipeIngredientsArray.isNotEmpty()) { "Recipe '$name' must require at least one ingredient." }

            // helper function to parse ingredients(needed due to detekt tests)
            val recipeIngredientsMap = parseRecipeIngredientsMap(
                recipeIngredientsArray,
                name,
                availableIngredients
            )

            // parse the recipe and add to the list
            val newRecipe = Recipe(id, name, duration, allowedCooks, recipeIngredientsMap, basicDishFor)
            val isRecipeValid = validateRecipe(newRecipe)
            require(isRecipeValid) { "Recipe $name failed logical validation (duration out of bounds)." }

            parsedRecipes.add(newRecipe)
        }

        return parsedRecipes
    }

    /**
     * Function to cross validate Recipe    .
     */
    private fun validateRecipe(recipe: Recipe): Boolean {
        val duration = recipe.duration

        //  Duration must be 2 <= duration && duration <= 40
        if (duration < MIN_DURATION || duration > MAX_DURATION) {
            return false
        }

        return true
    }

    /**
     * Function to check uniqueness of recipe    .
     */
    private fun checkUniquenessOfRecipe(
        id: Int,
        name: String,
        isBasicDish: Boolean,
        parsedRecipes: List<Recipe>
    ): Boolean {
        for (existingRec in parsedRecipes) {
            // duplicate ID check
            if (existingRec.id == id) {
                return false
            }

            //  There must exist exactly 1 default recipe per basic dish name
            val isExistingBasic = existingRec.basicDishFor != null
            if (isBasicDish && isExistingBasic) {
                if (existingRec.name == name) {
                    return false // two different recipes have the same name and both claim to be basic recipe
                }
            }
        }

        // completely unique
        return true
    }

    /**
     * Function to check whether the ingredient exists in the available ones   .
     */
    private fun checkIngredientExists(
        name: String,
        availableIngredients: List<Ingredient>
    ): Ingredient? {
        for (ing in availableIngredients) {
            if (ing.name == name) {
                return ing
            }
        }
        return null
    }

    /**
     * Helper function required to pass the detekt test, parses the ingredients required for a single recipe.
     */
    private fun parseRecipeIngredientsMap(
        recipeIngredientsArray: JsonArray,
        recipeName: String,
        availableIngredients: List<Ingredient>
    ): MutableMap<Ingredient, Int> {
        val recipeIngredientsMap = mutableMapOf<Ingredient, Int>()

        for (k in 0 until recipeIngredientsArray.size) {
            val recipeIngredientJson = recipeIngredientsArray[k].jsonObject

            val recipeIngredientName = recipeIngredientJson[KEY_NAME]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Recipe ingredient missing 'name'")

            // val recipeIngredientUnitStr = recipeIngredientJson[KEY_UNIT]?.jsonPrimitive?.content // POSSIBLE

            val recipeIngredientAmount = recipeIngredientJson["amount"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("Recipe ingredient missing 'amount'")

            // THE CROSS VALIDATION
            require(recipeIngredientAmount > 0) { "Recipe ingredient amount must be greater than 0." }

            // get the ingredient from the available ingredients, and crash if it does not exist
            val foundIngredient = checkIngredientExists(
                recipeIngredientName,
                availableIngredients
            ) ?: throw IllegalArgumentException("'$recipeName' requires '$recipeIngredientName' that does not exist.")

            // if a unit was specified, it must match the ingredient's defined unit
            // if (recipeIngredientUnitStr != null) { // POSSIBLE
            //     val recipeIngredientUnit = MeasurementUnit.valueOf(recipeIngredientUnitStr.uppercase()) // POSSIBLE
            //     require(recipeIngredientUnit == foundIngredient.unit) { // POSSIBLE
            //   "'$recipeName' ingredient '$recipeIngredientName' has unit '$recipeIngredientUnitStr' " + // POSSIBLE
            //             "that does not match its defined unit '${foundIngredient.unit}'." // POSSIBLE
            //     } // POSSIBLE
            // } // POSSIBLE

            // add valid ingredient and amount to the map
            recipeIngredientsMap[foundIngredient] = recipeIngredientAmount
        }

        return recipeIngredientsMap
    }
}
