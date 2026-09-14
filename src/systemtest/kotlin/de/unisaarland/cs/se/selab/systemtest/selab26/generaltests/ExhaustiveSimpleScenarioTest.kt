package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension

/**
 * System test that exhaustively verifies a complete restaurant simulation up to Tick 5.
 *
 * This test  validates the initial configuration parsing and the
 * evening preparation phase, followed by a step-by-step verification of the serving phase:
 *
 * - **Tick 1:** Event group reservation and restaurant decision.
 * - **Tick 2:** Casual group arrival, waitstaff seating, order placement, and kitchen assignment.
 * - **Tick 3:** Kitchen cooking completion and waitstaff serving operations.
 * - **Tick 4:** Customer eating phase duration.
 * - **Tick 5:** Meal completion, escorting customers outside, and collecting experience ratings.
 */
class ExhaustiveSimpleScenarioTest : ExampleSystemTestExtension() {
    override val name = "ExhaustiveScenarioTest"
    override val description = "Tests the full scenario from initialization through Tick 5 exhaustively"
    override val restaurants = "simplejson/restaurants.json"
    override val scenario = "simplejson/scenario.json"
    override val food = "simplejson/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 5

    /**
     * Holds string constants for empty status logs(to avoid detekt issues).
     */
    companion object {
        private const val START_R1 = "[DEBUG] Restaurant Start (R 1): Restaurant 1 simulates a tick."
        private const val END_R1 = "[DEBUG] Restaurant End (R 1): Restaurant 1 finished simulating the tick."

        // Wrapped to solve max lines error
        private const val SEATING_EMPTY = "[DEBUG] FOH Seating Status (R 1): 0 waitstaff seated 0 " +
            "customers on 0 tables."
        private const val SERVING_EMPTY = "[DEBUG] FOH Serving Status (R 1): 0 waitstaff served 0 meals."
        private const val RATING_EMPTY = "[DEBUG] Rating Status (R 1): 0 groups performed ratings this tick."
        private const val ORDERING_EMPTY = "[DEBUG] FOH Ordering Status (R 1): The restaurant received orders " +
            "from 0 customers, 0 waitstaff took orders."
        private const val KITCHEN_EMPTY = "[DEBUG] Kitchen Status (R 1): 0 cooks were active cooking 0 and " +
            "finishing 0 meals. 0 meals can be served by the waitstaff."
        private const val EATING_EMPTY = "[DEBUG] FOH Eating Status (R 1): 0 customers are eating " +
            "and 0 customers have finished eating this tick."
        private const val ESCORTING_EMPTY = "[DEBUG] FOH Escorting Status (R 1): 0 waitstaff escorted 0 " +
            "customers this tick."
    }

    override suspend fun run() {
        assertInitAndPrep()
        assertTick1()
        assertTick2()
        assertTick3()
        assertTick4()
        assertTick5()
    }

    private suspend fun assertInitAndPrep() {
        assertNextLine("[INFO] Initialization Info: food successfully parsed and validated.")
        assertNextLine("[INFO] Initialization Info: restaurants successfully parsed and validated.")
        assertNextLine("[INFO] Initialization Info: scenario successfully parsed and validated.")
        assertNextLine("[INFO] Simulation Info: Simulation started.")
        assertNextLine("[IMPORTANT] Preparation: Preparation for evening 1 starts.")
        assertNextLine("[DEBUG] Pantry (R 1): Procured 2500 G of Chicken from the supplier.")
        assertNextLine("[DEBUG] Pantry (R 1): Procured 500 G of Tomato from the supplier.")
        assertNextLine("[INFO] Pantry (R 1): Restocked ingredients.")
        assertNextLine("[IMPORTANT] Serving: Serving of evening 1 starts.")
    }

    private suspend fun assertTick1() {
        assertNextLine("[IMPORTANT] Simulation: Tick 1 (1) started.")
        assertNextLine("[DEBUG] Restaurant Decision: Group 1 decided on restaurant 1.")
        assertNextLine(START_R1)
        assertNextLine(SEATING_EMPTY)
        assertNextLine(ORDERING_EMPTY)
        assertNextLine(KITCHEN_EMPTY)
        assertNextLine(SERVING_EMPTY)
        assertNextLine(EATING_EMPTY)
        assertNextLine(ESCORTING_EMPTY)
        assertNextLine(RATING_EMPTY)
        assertNextLine(END_R1)
    }

    private suspend fun assertTick2() {
        assertNextLine("[IMPORTANT] Simulation: Tick 2 (1) started.")
        assertNextLine("[DEBUG] Restaurant Decision: Group 2 decided on restaurant 1.")
        assertNextLine(START_R1)
        assertNextLine("[INFO] Restaurant Arrival (R 1): Group 2 arrived at restaurant 1.")
        assertNextLine("[IMPORTANT] FOH Seating (R 1): Group 2 seated at table 1 by waitstaff 1.")

        assertNextLine(
            "[IMPORTANT] FOH Ordering (R 1): Group 2 placed order 1 of " +
                "Grilled Chicken:4 with waitstaff 1."
        )
        assertNextLine("[DEBUG] FOH Seating Status (R 1): 1 waitstaff seated 4 customers on 1 tables.")
        assertNextLine(
            "[DEBUG] FOH Ordering Status (R 1): The restaurant received orders from 4 customers, " +
                "1 waitstaff took orders."
        )
        assertNextLine(
            "[IMPORTANT] Kitchen Dish Assignment (R 1): Cook 1 of type ROAST starts cooking 4 " +
                "meals of dish Grilled Chicken based on order 1 for orders 1."
        )
        assertNextLine(
            "[DEBUG] Kitchen Status (R 1): 1 cooks were active cooking 4 and finishing 0 meals. " +
                "0 meals can be served by the waitstaff."
        )
        assertNextLine(SERVING_EMPTY)
        assertNextLine(EATING_EMPTY)
        assertNextLine(ESCORTING_EMPTY)
        assertNextLine(RATING_EMPTY)
        assertNextLine(END_R1)
    }

    private suspend fun assertTick3() {
        assertNextLine("[IMPORTANT] Simulation: Tick 3 (1) started.")
        assertNextLine(START_R1)
        assertNextLine(SEATING_EMPTY)
        assertNextLine(ORDERING_EMPTY)
        assertNextLine(
            "[IMPORTANT] Kitchen Meal Cooked (R 1): Cook 1 finished cooking 4 meals " +
                "of dish Grilled Chicken 1 ticks after ordering."
        )
        assertNextLine(
            "[DEBUG] Kitchen Status (R 1): 1 cooks were active cooking 4 and " +
                "finishing 4 meals. 4 meals can be served by the waitstaff."
        )
        assertNextLine(
            "[IMPORTANT] FOH Serving (R 1): Waitstaff 1 serves Grilled Chicken:4 " +
                "to table 1 1 ticks after ordering."
        )
        assertNextLine("[DEBUG] FOH Serving Status (R 1): 1 waitstaff served 4 meals.")
        assertNextLine(
            "[DEBUG] FOH Eating Status (R 1): 4 customers are eating and " +
                "0 customers have finished eating this tick."
        )
        assertNextLine(ESCORTING_EMPTY)
        assertNextLine(RATING_EMPTY)
        assertNextLine(END_R1)
    }

    private suspend fun assertTick4() {
        assertNextLine("[IMPORTANT] Simulation: Tick 4 (1) started.")
        assertNextLine(START_R1)
        assertNextLine(SEATING_EMPTY)
        assertNextLine(ORDERING_EMPTY)
        assertNextLine(KITCHEN_EMPTY)
        assertNextLine(SERVING_EMPTY)
        assertNextLine(
            "[DEBUG] FOH Eating Status (R 1): 4 customers are eating and " +
                "0 customers have finished eating this tick."
        )
        assertNextLine(ESCORTING_EMPTY)
        assertNextLine(RATING_EMPTY)
        assertNextLine(END_R1)
    }

    private suspend fun assertTick5() {
        assertNextLine("[IMPORTANT] Simulation: Tick 5 (1) started.")
        assertNextLine(START_R1)
        assertNextLine(SEATING_EMPTY)
        assertNextLine(ORDERING_EMPTY)
        assertNextLine(KITCHEN_EMPTY)
        assertNextLine(SERVING_EMPTY)
        assertNextLine(
            "[INFO] FOH Finished Eating (R 1): 4 customers of group 2 have " +
                "finished eating at table 1."
        )
        assertNextLine(
            "[DEBUG] FOH Eating Status (R 1): 0 customers are eating and " +
                "4 customers have finished eating this tick."
        )
        assertNextLine(
            "[IMPORTANT] FOH Escorting (R 1): Waitstaff 1 escorts 4 customers " +
                "of group 2 from table 1 outside."
        )
        assertNextLine("[DEBUG] FOH Escorting Status (R 1): 1 waitstaff escorted 4 customers this tick.")
        assertNextLine(
            "[INFO] Rating (R 1): Group 2 rates the restaurant 1 with POSITIVE rating, " +
                "leading to 11 positive ratings and 3 negative ratings."
        )
        assertNextLine("[DEBUG] Rating Status (R 1): 1 groups performed ratings this tick.")
        assertNextLine(END_R1)
    }
}
