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
 * Tests for how a RECIPE incident affects the kitchen's ingredient planning (specification page 11,
 * lines 31-35 and page 12). The planning uses the *current* recipes for the meals of earlier visits,
 * not the amounts of back then (forum topic 131).
 *
 * Written on Sep 21 to narrow down failing recipe-change component tests. Ingredients are sold in
 * 1 g packages, so the procured amount is exactly the planned amount minus what is left in the
 * pantry. The first two tests were corrected in run 4, once the reference showed that a first-time
 * REGULAR's reserved seats still count towards the estimate.
 */
abstract class RecipeChangePlanningSystemTest : ExampleSystemTestExtension() {
    override val logLevel = "DEBUG"
}

/**
 * The meals of a REGULAR group's earlier visit are planned at the changed amount.
 *
 * A REGULAR group of 2 visits every evening, reserves the 2 seat table (next to a 30 seat one) and
 * orders 2 meals of the only dish. Rice starts at 10 g:
 *  - evening 1: first visit, so all 32 seats count: 4 meals, 40 g bought, 20 g eaten, 20 g left;
 *  - a +50% incident makes the recipe 15 g;
 *  - evening 2: 3 meals for the 30 other seats (45) plus the one visit in the history (2 * 15),
 *    75 g needed, so 55 g are bought.
 *
 * Planning the history at the old 10 g would buy 45 g.
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
        /** the estimate alone: 4 meals of 10 g, the group's 2 reserved seats included */
        const val FIRST_EVENING = 40

        /** 3 * 15 estimated plus 2 * 15 from the one visit in the history, minus the 20 left over */
        const val SECOND_EVENING = 55
    }
}

/**
 * The order history grows by one visit per evening, and after an incident every visit in it is
 * planned at the changed amount.
 *
 * Same restaurant and group as [RecipeChangeOrderHistorySystemTest], but the +50% incident only
 * comes before evening 4. Quinoa is 10 g on evenings 1-3 and 15 g on evening 4:
 *  - evening 2: 30 estimated + 20 for one visit - 20 left = 30 g bought;
 *  - evening 3: 30 estimated + 40 for two visits - 30 left = 40 g bought;
 *  - evening 4: 45 estimated + 90 for three visits - 50 left = 85 g bought.
 *
 * Planning the history at the old 10 g would buy 55 g on evening 4. The group has exactly three
 * earlier visits there, so the "last three visits" cap (page 11, line 34) is not exercised.
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
        /** 30 estimated plus 20 of history, minus the 20 left over */
        const val SECOND_EVENING = 30

        /** 30 estimated plus 40 of history, minus the 30 left over */
        const val THIRD_EVENING = 40

        /** 45 estimated plus 90 for three visits, minus the 50 left over */
        const val FOURTH_EVENING = 85
    }
}

/**
 * An EVENT group's meals are planned at the changed amount. The kitchen plans one meal for every
 * customer of an event (specification page 12, line 1).
 *
 * An EVENT group of 4 comes on evening 4, which is also when a +50% incident hits. Truffle starts at
 * 10 g. Evenings 1-3 need 2 meals for the 14 free seats, so 20 g are bought once. On evening 4 the
 * event reserves the 4 seat table: 1 meal for the other 10 seats plus 4 for the event, at 15 g,
 * is 75 g, and 55 g are bought on top of the 20 g left.
 *
 * Planning the event at the old 10 g would buy 35 g.
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
