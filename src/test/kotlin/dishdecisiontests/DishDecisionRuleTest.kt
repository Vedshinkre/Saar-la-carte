package dishdecisiontests

import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.RestaurantType
import dishdecisiontests.DishDecisionFixtures.allIngredients
import dishdecisiontests.DishDecisionFixtures.almond
import dishdecisiontests.DishDecisionFixtures.chicken
import dishdecisiontests.DishDecisionFixtures.dish
import dishdecisiontests.DishDecisionFixtures.lentil
import dishdecisiontests.DishDecisionFixtures.mixedMenu
import dishdecisiontests.DishDecisionFixtures.saffron
import dishdecisiontests.DishDecisionFixtures.simpleMenu
import dishdecisiontests.DishDecisionFixtures.tomato
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for how a customer decides on a dish (spec 2.3 "Ordering"): they only select among
 * dishes where they eat all ingredients. Among those they choose their first matching favorite
 * dish, otherwise the dish with the most of their preferred ingredients (counted by enumeration,
 * not by quantity), and in case of a tie the one with the highest recipe id. Without preferences
 * that is the dish with the highest recipe id. Event customers prioritize the favorite dish of the
 * event over their own favorite dish or preferred ingredients.
 *
 * The recipe ids are 1 Tomato Soup, 2 Chicken Curry, 3 Lentil Stew and 4 Almond Tart, see
 * [DishDecisionFixtures]. Every menu is listed in a scrambled order so the tests cannot pass by
 * accident of the list order.
 */
class DishDecisionRuleTest {

    @Nested
    inner class NoPreferences {
        @Test
        fun `a customer without preferences selects the dish with the highest recipe id`() {
            assertEquals(TART, pick(simpleMenu))
        }

        @Test
        fun `the highest recipe id wins whatever the amounts of the ingredients are`() {
            // the curry has by far the largest ingredient amount, the stew the higher id
            assertEquals(STEW, pick(listOf(dish(simpleMenu, CURRY), dish(simpleMenu, STEW))))
        }

        @Test
        fun `basic dishes are not favoured over dishes with a higher recipe id`() {
            assertEquals(CURRY, pick(listOf(dish(simpleMenu, SOUP), dish(simpleMenu, CURRY))))
        }

        @Test
        fun `a menu with a single dish gives that dish`() {
            assertEquals(SOUP, pick(listOf(dish(simpleMenu, SOUP))))
        }

        @Test
        fun `an empty menu gives no dish`() {
            assertNull(pick(emptyList()))
        }
    }

    @Nested
    inner class ExcludedIngredients {
        @Test
        fun `an excluded ingredient removes every dish that contains it`() {
            val menu = listOf(dish(simpleMenu, SOUP), dish(simpleMenu, CURRY))

            assertEquals(CURRY, pick(menu))
            assertEquals(SOUP, pick(menu, excluded = listOf(chicken)))
        }

        @Test
        fun `a single excluded ingredient among several removes the whole dish`() {
            val menu = listOf(dish(mixedMenu, CURRY), dish(mixedMenu, STEW))

            assertEquals(STEW, pick(menu))
            assertEquals(CURRY, pick(menu, excluded = listOf(tomato)))
        }

        @Test
        fun `all excluded ingredients count together`() {
            assertEquals(STEW, pick(simpleMenu, excluded = listOf(chicken, almond)))
        }

        @Test
        fun `an excluded ingredient that is in no dish changes nothing`() {
            assertEquals(TART, pick(simpleMenu, excluded = listOf(saffron)))
        }

        @Test
        fun `the only dish that is left is chosen whatever else the customer wants`() {
            val decided = pick(
                simpleMenu,
                excluded = listOf(tomato, chicken, almond),
                preferred = listOf(tomato),
                favourites = listOf(SOUP, TART)
            )

            assertEquals(STEW, decided)
        }

        @Test
        fun `a customer who excludes an ingredient of every dish decides on none`() {
            assertNull(pick(simpleMenu, excluded = allIngredients))
        }

        @Test
        fun `a customer who excludes everything decides on none even with preferences and an event dish`() {
            val decided = pick(
                simpleMenu,
                excluded = allIngredients,
                preferred = listOf(lentil),
                favourites = listOf(CURRY),
                eventDish = SOUP
            )

            assertNull(decided)
        }

        @Test
        fun `an excluded ingredient beats a favorite dish that contains it`() {
            assertEquals(TART, pick(simpleMenu, excluded = listOf(tomato), favourites = listOf(SOUP)))
        }

        @Test
        fun `an excluded ingredient beats a preferred ingredient of the same dish`() {
            assertEquals(TART, pick(simpleMenu, excluded = listOf(tomato), preferred = listOf(tomato)))
        }

        @Test
        fun `an excluded ingredient beats the favorite dish of the event`() {
            assertEquals(TART, pick(simpleMenu, excluded = listOf(tomato), eventDish = SOUP))
        }
    }

    @Nested
    inner class FavouriteDishes {
        @Test
        fun `a favorite dish beats the dish with the highest recipe id`() {
            assertEquals(SOUP, pick(simpleMenu, favourites = listOf(SOUP)))
        }

        @Test
        fun `a favorite dish beats a preferred ingredient of another dish`() {
            assertEquals(SOUP, pick(simpleMenu, preferred = listOf(almond), favourites = listOf(SOUP)))
        }

        @Test
        fun `the first favorite dish of the customer wins, whatever the recipe ids and the menu order are`() {
            assertEquals(SOUP, pick(simpleMenu, favourites = listOf(SOUP, TART)))
            assertEquals(TART, pick(simpleMenu, favourites = listOf(TART, SOUP)))
            assertEquals(STEW, pick(simpleMenu, favourites = listOf(STEW, CURRY, SOUP)))
        }

        @Test
        fun `a favorite dish that is not on the menu is skipped for the next favorite dish`() {
            assertEquals(CURRY, pick(simpleMenu, favourites = listOf(GHOST, CURRY)))
        }

        @Test
        fun `a favorite dish with an excluded ingredient is skipped for the next favorite dish`() {
            assertEquals(STEW, pick(simpleMenu, excluded = listOf(tomato), favourites = listOf(SOUP, STEW)))
        }

        @Test
        fun `without any favorite dish on the menu the customer falls back to the highest recipe id`() {
            assertEquals(TART, pick(simpleMenu, favourites = listOf(GHOST)))
        }

        @Test
        fun `without any favorite dish on the menu the customer falls back to the preferred ingredients`() {
            assertEquals(STEW, pick(simpleMenu, preferred = listOf(lentil), favourites = listOf(GHOST)))
        }

        @Test
        fun `without any edible favorite dish the customer falls back to the preferred ingredients`() {
            val decided = pick(
                simpleMenu,
                excluded = listOf(tomato),
                preferred = listOf(lentil),
                favourites = listOf(SOUP)
            )

            assertEquals(STEW, decided)
        }
    }

    @Nested
    inner class PreferredIngredients {
        @Test
        fun `the dish with the most preferred ingredients wins`() {
            // stew: lentil and tomato, curry: lentil, soup: tomato
            assertEquals(STEW, pick(mixedMenu, preferred = listOf(lentil, tomato)))
        }

        @Test
        fun `the dish with more preferred ingredients wins over a dish with a higher recipe id`() {
            // curry: chicken and lentil, stew: lentil only
            assertEquals(CURRY, pick(mixedMenu, preferred = listOf(chicken, lentil)))
        }

        @Test
        fun `a preferred ingredient of a single dish gives that dish, whatever its recipe id is`() {
            assertEquals(SOUP, pick(simpleMenu, preferred = listOf(tomato)))
        }

        @Test
        fun `preferred ingredients are counted by enumeration and not by quantity`() {
            // curry: 900 g of chicken, soup: 1 tomato, stew: 1 tomato. Each dish has one preferred
            // ingredient, so they tie and the highest recipe id wins instead of the largest amount
            assertEquals(STEW, pick(mixedMenu, preferred = listOf(chicken, tomato)))
        }

        @Test
        fun `dishes with the same number of preferred ingredients tie and the highest recipe id wins`() {
            // curry and stew both contain lentils
            assertEquals(STEW, pick(mixedMenu, preferred = listOf(lentil)))
        }

        @Test
        fun `preferred ingredients that are in no dish leave all dishes tied, the highest recipe id wins`() {
            assertEquals(TART, pick(simpleMenu, preferred = listOf(saffron)))
        }

        @Test
        fun `preferred ingredients only narrow down the dishes without excluded ingredients`() {
            // the stew has the most preferred ingredients, but contains the excluded tomato
            val decided = pick(mixedMenu, excluded = listOf(tomato), preferred = listOf(lentil, tomato))

            assertEquals(CURRY, decided)
        }
    }

    @Nested
    inner class EventFavouriteDish {
        @Test
        fun `the favorite dish of the event beats the own favorite dish`() {
            assertEquals(SOUP, pick(simpleMenu, favourites = listOf(CURRY), eventDish = SOUP))
        }

        @Test
        fun `the favorite dish of the event beats the own favorite dish even if it is listed first`() {
            assertEquals(SOUP, pick(simpleMenu, favourites = listOf(TART, SOUP), eventDish = SOUP))
        }

        @Test
        fun `the favorite dish of the event beats a preferred ingredient`() {
            assertEquals(SOUP, pick(simpleMenu, preferred = listOf(lentil), eventDish = SOUP))
        }

        @Test
        fun `the favorite dish of the event beats the highest recipe id of a customer without preferences`() {
            assertEquals(SOUP, pick(simpleMenu, eventDish = SOUP))
        }

        @Test
        fun `the favorite dish of the event beats own favorite dish and preferred ingredient together`() {
            val decided = pick(simpleMenu, preferred = listOf(lentil), favourites = listOf(CURRY), eventDish = SOUP)

            assertEquals(SOUP, decided)
        }

        @Test
        fun `the favorite dish of the event that is also the own favorite dish is chosen`() {
            assertEquals(CURRY, pick(simpleMenu, favourites = listOf(CURRY), eventDish = CURRY))
        }

        @Test
        fun `the same customer at no event keeps the own favorite dish`() {
            assertEquals(CURRY, pick(simpleMenu, favourites = listOf(CURRY), eventDish = ""))
        }

        @Test
        fun `a customer who cannot eat the event dish uses the own favorite dish`() {
            val decided = pick(simpleMenu, excluded = listOf(tomato), favourites = listOf(CURRY), eventDish = SOUP)

            assertEquals(CURRY, decided)
        }

        @Test
        fun `a customer who cannot eat the event dish uses the preferred ingredients`() {
            val decided = pick(simpleMenu, excluded = listOf(tomato), preferred = listOf(lentil), eventDish = SOUP)

            assertEquals(STEW, decided)
        }

        @Test
        fun `a customer without preferences who cannot eat the event dish selects the highest recipe id`() {
            assertEquals(TART, pick(simpleMenu, excluded = listOf(tomato), eventDish = SOUP))
        }

        @Test
        fun `an event dish that is not on the menu leaves the own preferences in charge`() {
            assertEquals(CURRY, pick(simpleMenu, favourites = listOf(CURRY), eventDish = GHOST))
            assertEquals(STEW, pick(simpleMenu, preferred = listOf(lentil), eventDish = GHOST))
            assertEquals(TART, pick(simpleMenu, eventDish = GHOST))
        }
    }

    @Nested
    inner class DecidedDish {
        @Test
        fun `the dish is a new uncooked meal of the chosen recipe`() {
            val decided = FoodPreference(emptyList(), emptyList(), listOf(CURRY))
                .decideDish(simpleMenu, "", RestaurantType.EUROPEAN)

            assertEquals(dish(simpleMenu, CURRY), decided?.recipe)
            assertEquals(DishStatus.UNCOOKED, decided?.status)
        }

        @Test
        fun `the dish is basic if the recipe is the basic dish of the restaurant type`() {
            val preference = FoodPreference(emptyList(), emptyList(), listOf(SOUP))

            assertTrue(preference.decideDish(simpleMenu, "", RestaurantType.EUROPEAN)!!.isBasic)
            assertFalse(preference.decideDish(simpleMenu, "", RestaurantType.ASIAN)!!.isBasic)
        }
    }
}
