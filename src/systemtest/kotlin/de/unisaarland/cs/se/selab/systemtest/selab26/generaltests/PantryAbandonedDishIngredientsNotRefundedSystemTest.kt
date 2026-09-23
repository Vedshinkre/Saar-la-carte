package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Pantry probe (spec adjustment #20): a customer that leaves gives the kitchen no signal, so the
 * ingredients reserved for their meal are eaten by the staff and never return to the pantry. The
 * REGULAR group orders Plain Rice and Slow Paella every evening but leaves before the paella is
 * served. Evening 1 buys 1000 g Rice and 50 g Saffron; the paella reserves all 50 g Saffron and 10 g
 * Rice. Evening 2 must therefore buy the 50 g Saffron again, while the rice (990 g left) is enough.
 * A refund of the abandoned reservation would show up as a missing Saffron purchase.
 */
class PantryAbandonedDishIngredientsNotRefundedSystemTest : ExampleSystemTestExtension() {
    override val name = "PantryAbandonedDishIngredientsNotRefundedSystemTest"
    override val description = "Ingredients reserved for a dish nobody eats are not returned to the pantry"
    override val restaurants = "pantryabandonedreservationjson/restaurants.json"
    override val food = "pantryabandonedreservationjson/food.json"
    override val scenario = "pantryabandonedreservationjson/scenario.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 3 * 24

    override suspend fun run() {
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, RICE_PACKAGE, UNIT, RICE))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SAFFRON_PACKAGE, UNIT, SAFFRON))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        // saffron was fully reserved by the abandoned paella, rice still covers the need
        skipUntilString(InitialAndPrepTestLogs.prepStart(2))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SAFFRON_PACKAGE, UNIT, SAFFRON))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))

        skipUntilString(InitialAndPrepTestLogs.prepStart(3))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, SAFFRON_PACKAGE, UNIT, SAFFRON))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(1))
    }

    private companion object {
        const val UNIT = "g"
        const val RICE = "Rice"
        const val SAFFRON = "Saffron"
        const val RICE_PACKAGE = 1000
        const val SAFFRON_PACKAGE = 50
    }
}
