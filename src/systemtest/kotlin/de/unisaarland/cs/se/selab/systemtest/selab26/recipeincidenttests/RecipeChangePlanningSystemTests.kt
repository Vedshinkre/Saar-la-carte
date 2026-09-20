package de.unisaarland.cs.se.selab.systemtest.selab26.recipeincidenttests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

private const val PLAN_DIR = "recipeprobejson"
private const val PLAN_UNIT = "g"
private const val PLAN_RECIPE = "RECIPE"
private const val FOUR_EVENINGS = 96
private const val QUINOA = "quinoa"

private fun planFile(file: String) = "$PLAN_DIR/$file"

/**
 * Probes for the interaction between a RECIPE incident and the kitchen's ingredient planning
 * (specification page 11 lines 31-35 and page 12, and forum topic 131: the planning uses the
 * *current* recipes of the meals ordered on previous visits, not the amounts of back then).
 *
 * Ingredients are sold in 1 g packages, so procured amounts equal planned amounts exactly.
 */
abstract class RecipeChangePlanningSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
}

/**
 * A regular group of 2 visits every evening and orders 2 meals of the only dish. The restaurant
 * has a 2 seat table, which the group reserves, and a 30 seat table, which leaves 30 other seats,
 * so the estimate is 3 meals of the dish per evening.
 *
 * Rice starts at 10 g. Evening 1 has no history: 3 * 10 = 30 planned and procured, the group
 * reserves 2 * 10 = 20 for its order, leaving 10 in the pantry. A +50% incident then raises the
 * recipe to 15 g before evening 2, where the estimate is 3 * 15 = 45 and the single visit in the
 * history is 2 * 15 = 30, so 75 are required and 65 are bought on top of the 10 left over.
 *
 * Planning the history with the old 10 g would require only 65 and buy 55.
 */
class RecipeChangeOrderHistorySystemTest : RecipeChangePlanningSystemTest() {
    override val name = "RecipeChangeOrderHistorySystemTest"
    override val description = "Ingredient planning for a regular's past visit uses the changed recipe"
    override val food = planFile("historyFood.json")
    override val restaurants = planFile("historyRestaurants.json")
    override val scenario = planFile("historyScenario.json")
    override val maxTicks = FOUR_EVENINGS

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.SIM_START)
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, FIRST_EVENING, PLAN_UNIT, "rice"))

        skipUntilString(InitialAndPrepTestLogs.incident(1, PLAN_RECIPE, 2))
        assertNextLine(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SECOND_EVENING, PLAN_UNIT, "rice"))
    }

    private companion object {
        /** the estimate alone: 3 meals of 10 g */
        const val FIRST_EVENING = 30

        /** 3 * 15 estimated plus 2 * 15 from the one visit in the history, minus 10 left over */
        const val SECOND_EVENING = 65
    }
}

/**
 * The same restaurant and group as [RecipeChangeOrderHistorySystemTest], but the incident only
 * fires before evening 4, by which time the group has visited three times. The kitchen plans for
 * the last three visits (specification page 11, line 34), all of them at the changed amount.
 *
 * Quinoa runs 10 g for the first three evenings: 30 procured on evening 1 (estimate only), 40 on
 * evening 2 (30 estimated plus 20 of history, minus 10 left over) and 40 again on evening 3
 * (30 plus 40 of history, minus 30 left over). A +50% incident then makes it 15 g, so evening 4
 * needs 3 * 15 estimated plus 6 * 15 for the three visits in the history, that is 135, and buys
 * 85 on top of the 50 left in the pantry.
 *
 * Counting a fourth visit would buy 115, and planning the history at the old 10 g would buy 55.
 */
class RecipeChangeThreeVisitHistorySystemTest : RecipeChangePlanningSystemTest() {
    override val name = "RecipeChangeThreeVisitHistorySystemTest"
    override val description = "Only the last three visits are planned, and at the changed amount"
    override val food = planFile("threeVisitsFood.json")
    override val restaurants = planFile("historyRestaurants.json")
    override val scenario = planFile("threeVisitsScenario.json")
    override val maxTicks = FOUR_EVENINGS

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SECOND_EVENING, PLAN_UNIT, QUINOA))
        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, THIRD_EVENING, PLAN_UNIT, QUINOA))

        skipUntilString(InitialAndPrepTestLogs.incident(1, PLAN_RECIPE, 4))
        assertNextLine(InitialAndPrepTestLogs.prepStart(4))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, FOURTH_EVENING, PLAN_UNIT, QUINOA))
    }

    private companion object {
        /** 30 estimated plus 20 of history, minus the 10 left over */
        const val SECOND_EVENING = 40

        /** 30 estimated plus 40 of history, minus the 30 left over */
        const val THIRD_EVENING = 40

        /** 45 estimated plus 90 for three visits, minus the 50 left over */
        const val FOURTH_EVENING = 85
    }
}

/**
 * An EVENT group of 4 reserves for evening 4 and the kitchen plans its favourite dish for every
 * customer of the group (specification page 12, line 1). A +50% incident before that evening has
 * to be part of that plan as well.
 *
 * Truffle starts at 10 g. Evenings 1 to 3 only need the estimate of 2 meals for the 14 unreserved
 * seats, so 20 g are bought on evening 1 and nothing afterwards. On evening 4 the event reserves
 * the 4 seat table, leaving 10 other seats and an estimate of 1 meal, and the recipe is now 15 g:
 * 15 estimated plus 4 * 15 for the event, that is 75, minus the 20 in the pantry, so 55 are bought.
 *
 * Planning the event at the old 10 g would buy 35.
 */
class RecipeChangeEventPlanningSystemTest : RecipeChangePlanningSystemTest() {
    override val name = "RecipeChangeEventPlanningSystemTest"
    override val description = "The event group's favourite dish is planned at the changed amount"
    override val food = planFile("eventFood.json")
    override val restaurants = planFile("eventRestaurants.json")
    override val scenario = planFile("eventScenario.json")
    override val maxTicks = FOUR_EVENINGS

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.SIM_START)
        assertNextLine(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, FIRST_EVENING, PLAN_UNIT, "truffle"))

        skipUntilString(InitialAndPrepTestLogs.incident(1, PLAN_RECIPE, 4))
        assertNextLine(InitialAndPrepTestLogs.prepStart(4))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, EVENT_EVENING, PLAN_UNIT, "truffle"))
    }

    private companion object {
        /** 2 meals estimated for the 14 unreserved seats */
        const val FIRST_EVENING = 20

        /** 15 estimated plus 4 * 15 for the event group, minus the 20 left in the pantry */
        const val EVENT_EVENING = 55
    }
}
