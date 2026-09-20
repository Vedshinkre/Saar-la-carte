package recipechangeincidenttset

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.incidents.RecipeChangeIncident
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.system.Simulation
import de.unisaarland.cs.se.selab.system.SimulationConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class RecipeChangeIntegrationTest {

    @BeforeTest
    fun setUp() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 1
        Time.ticksElapsed = 0

        val output = StringWriter()
        Logger.setup(PrintWriter(output))
        Logger.setup(LogLevel.DEBUG)
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
        Time.maxTicks = 0
        Time.ticksElapsed = 0
    }

    @Test
    fun `simulation correctly executes recipe change incident in the evening loop`() {
        // real tomato
        val tomato = Ingredient("Tomato", MeasurementUnit.G, 5, 1000)

        val recipe = Recipe(
            id = 1,
            name = "Tomato Soup",
            duration = 10,
            cookType = listOf(CookType.TOURNANT),
            ingredients = mutableMapOf(tomato to 100),
            basicDishFor = RestaurantType.EUROPEAN
        )

        val incident = RecipeChangeIncident(
            id = 1,
            evening = 1,
            ingredient = tomato,
            adaptation = 50, // +50% increase (100 -> 150)
            recipes = listOf(recipe)
        )

        // ingredient
        val config = SimulationConfig().apply {
            this.ingredients = listOf(tomato)
            this.recipes = listOf(recipe)
            this.incidents = listOf(incident)
            this.restaurants = emptyList() // Safe to leave empty, simulation won't crash
            this.customers = emptyList()
            this.stock = Stock(emptyList())
        }

        // run simulation
        val simulation = Simulation(config)
        simulation.runSimulation()

        // the actual recipe should change
        assertEquals(150, recipe.ingredients[tomato])
    }

    @Test
    fun `recipe change - multiple dishes sharing the same ingredient`() {
        // one shared onion
        val onion = Ingredient("onion", MeasurementUnit.G, 5, 1000)

        // distinct recipes representing different restaurants' adaptations
        val restaurantOneSoup = Recipe(
            id = 1,
            name = "Basic Soup",
            duration = 10,
            cookType = listOf(CookType.TOURNANT),
            ingredients = mutableMapOf(onion to 50), // Starts at 50g
            basicDishFor = RestaurantType.EUROPEAN
        )

        val restaurantTwoStew = Recipe(
            id = 2,
            name = "Stew",
            duration = 15,
            cookType = listOf(CookType.TOURNANT),
            ingredients = mutableMapOf(onion to 100), // Starts at 100g
            basicDishFor = RestaurantType.EUROPEAN
        )

        // the incident is worked on both the recipes
        val incident = RecipeChangeIncident(
            id = 1,
            evening = 1,
            ingredient = onion,
            adaptation = 3, // +3% increase
            recipes = listOf(restaurantOneSoup, restaurantTwoStew)
        )

        val config = SimulationConfig().apply {
            this.ingredients = listOf(onion)
            this.recipes = listOf(restaurantOneSoup, restaurantTwoStew)
            this.incidents = listOf(incident)
            this.restaurants = emptyList()
            this.customers = emptyList()
            this.stock = Stock(emptyList())
        }

        // running the simulation
        val simulation = Simulation(config)
        simulation.runSimulation()

        // both recipes should be permanently changed
        // 50 * 1.03 = 51.5 -> floors to 51
        assertEquals(51, restaurantOneSoup.ingredients[onion])
        // 100 * 1.03 = 103
        assertEquals(103, restaurantTwoStew.ingredients[onion])
    }
}
