package dishdecisiontests

import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import dishdecisiontests.DishDecisionFixtures.allIngredients
import dishdecisiontests.DishDecisionFixtures.almond
import dishdecisiontests.DishDecisionFixtures.chicken
import dishdecisiontests.DishDecisionFixtures.lentil
import dishdecisiontests.DishDecisionFixtures.mixedMenu
import dishdecisiontests.DishDecisionFixtures.simpleMenu
import dishdecisiontests.DishDecisionFixtures.tomato
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val FIRST_MISMATCHES = 5
private const val NO_DISH = "no dish"

/** the ingredient and dish names of a decision, so that a mismatch can be told from the report */
private data class Decision(
    val excluded: List<Ingredient>,
    val favourites: List<String>,
    val preferred: List<Ingredient>,
    val eventDish: String
) {
    override fun toString() = "excluded=${excluded.map { it.name }} favourites=$favourites " +
        "preferred=${preferred.map { it.name }} event='$eventDish'"
}

/**
 * A customer with the four things that decide on a dish, present or absent. The dishes they point
 * to are all different, so the expected dish shows which rule of the spec (2.3 "Ordering") fired:
 *
 * - excluded ingredient: tomato, removes the Tomato Soup, the favorite dish of the event
 * - the event favorite dish: Tomato Soup (recipe id 1)
 * - own favorite dish: Chicken Curry (recipe id 2)
 * - preferred ingredient: lentil, only in the Lentil Stew (recipe id 3)
 * - none of them: Almond Tart, the highest recipe id (4)
 */
private data class Layers(
    val excludesEventDish: Boolean,
    val hasFavourite: Boolean,
    val hasPreferred: Boolean,
    val atEvent: Boolean,
    val expected: String
) {
    val label = "excluded=$excludesEventDish favourite=$hasFavourite preferred=$hasPreferred event=$atEvent"

    fun decide(menu: List<Recipe>): String? = pick(
        menu,
        excluded = if (excludesEventDish) listOf(tomato) else emptyList(),
        preferred = if (hasPreferred) listOf(lentil) else emptyList(),
        favourites = if (hasFavourite) listOf(CURRY) else emptyList(),
        eventDish = if (atEvent) SOUP else ""
    )
}

/**
 * All permutations of the inputs of a dish decision: which of the four layers a customer has, the
 * order of the menu, and every combination of excluded ingredients, favorite dishes, preferred
 * ingredients and event dishes against a model of the spec text. The single rules are in
 * [DishDecisionRuleTest].
 */
class DishDecisionPermutationTest {

    // exclusion, favourite, preferred, event: the winner is the first layer that is present and edible
    private val layerMatrix = listOf(
        Layers(false, false, false, false, TART),
        Layers(false, false, false, true, SOUP),
        Layers(false, false, true, false, STEW),
        Layers(false, false, true, true, SOUP),
        Layers(false, true, false, false, CURRY),
        Layers(false, true, false, true, SOUP),
        Layers(false, true, true, false, CURRY),
        Layers(false, true, true, true, SOUP),
        // the event dish is excluded: it drops out, the other layers are untouched
        Layers(true, false, false, false, TART),
        Layers(true, false, false, true, TART),
        Layers(true, false, true, false, STEW),
        Layers(true, false, true, true, STEW),
        Layers(true, true, false, false, CURRY),
        Layers(true, true, false, true, CURRY),
        Layers(true, true, true, false, CURRY),
        Layers(true, true, true, true, CURRY)
    )

    @TestFactory
    fun `every combination of the four layers selects the dish of the first layer that applies`() =
        layerMatrix.map { row ->
            DynamicTest.dynamicTest(row.label) { assertEquals(row.expected, row.decide(simpleMenu)) }
        }

    @TestFactory
    fun `the layer combinations select the same dish for every order of the menu`() =
        layerMatrix.map { row ->
            DynamicTest.dynamicTest(row.label) {
                permutations(simpleMenu).forEach { menu ->
                    assertEquals(row.expected, row.decide(menu), "menu ${menu.map { it.name }}")
                }
            }
        }

    @Test
    fun `preferred ingredients select the same dish for every order of a menu with mixed dishes`() {
        val cases = mapOf(
            listOf(lentil, tomato) to STEW,
            listOf(chicken, lentil) to CURRY,
            listOf(chicken, tomato) to STEW,
            listOf(lentil) to STEW,
            listOf(almond) to TART,
            emptyList<Ingredient>() to TART
        )

        permutations(mixedMenu).forEach { menu ->
            cases.forEach { (preferred, expected) ->
                assertEquals(expected, pick(menu, preferred = preferred), "$preferred on ${menu.map { it.name }}")
            }
        }
    }

    @Test
    fun `every combination of exclusions, favorites, preferences and event dish follows the spec`() {
        val favouriteLists = listOf(
            emptyList(),
            listOf(CURRY),
            listOf(TART, CURRY),
            listOf(GHOST, STEW),
            listOf(SOUP, GHOST)
        )
        val preferredLists = listOf(
            emptyList(),
            listOf(lentil),
            listOf(lentil, tomato),
            listOf(chicken, tomato),
            listOf(almond, lentil, tomato)
        )
        val eventDishes = listOf("", SOUP, TART, GHOST)

        val inputs = subsets(allIngredients).flatMap { excluded ->
            favouriteLists.flatMap { favourites ->
                preferredLists.flatMap { preferred ->
                    eventDishes.map { Decision(excluded, favourites, preferred, it) }
                }
            }
        }

        for (menu in listOf(simpleMenu, mixedMenu)) {
            val mismatches = inputs.mapNotNull { mismatch(menu, it) }
            val report = "${mismatches.size} mismatches, first ones:\n" +
                mismatches.take(FIRST_MISMATCHES).joinToString("\n")
            assertTrue(mismatches.isEmpty(), report)
        }
    }

    /** a description of the disagreement between the implementation and the model, `null` if they agree */
    private fun mismatch(menu: List<Recipe>, input: Decision): String? {
        val actual = pick(menu, input.excluded, input.preferred, input.favourites, input.eventDish)
        val expected = model(menu, input.excluded, input.preferred, input.favourites, input.eventDish)
        return if (actual == expected) {
            null
        } else {
            "$input: expected ${expected ?: NO_DISH} but was ${actual ?: NO_DISH}"
        }
    }

    /** the sentence of the spec as a ladder: edible dishes, event dish, favorites, most preferred, highest id */
    private fun model(
        menu: List<Recipe>,
        excluded: List<Ingredient>,
        preferred: List<Ingredient>,
        favourites: List<String>,
        eventDish: String
    ): String? {
        val edible = menu.filter { recipe -> excluded.none { it in recipe.ingredients } }
        if (edible.isEmpty()) return null
        edible.firstOrNull { eventDish.isNotEmpty() && it.name == eventDish }?.let { return it.name }
        for (favourite in favourites) {
            edible.firstOrNull { it.name == favourite }?.let { return it.name }
        }
        val preferredCount = { recipe: Recipe -> preferred.count { it in recipe.ingredients } }
        val most = edible.maxOf(preferredCount)
        return edible.filter { preferredCount(it) == most }.maxBy { it.id }.name
    }

    private fun <T> subsets(items: List<T>): List<List<T>> =
        (0 until (1 shl items.size)).map { mask -> items.filterIndexed { index, _ -> mask and (1 shl index) != 0 } }

    private fun <T> permutations(items: List<T>): List<List<T>> {
        if (items.size <= 1) return listOf(items)
        return items.indices.flatMap { index ->
            permutations(items.filterIndexed { other, _ -> other != index }).map { listOf(items[index]) + it }
        }
    }
}
