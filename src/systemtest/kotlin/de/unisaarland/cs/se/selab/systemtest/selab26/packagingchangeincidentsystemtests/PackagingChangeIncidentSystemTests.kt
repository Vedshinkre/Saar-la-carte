package de.unisaarland.cs.se.selab.systemtest.selab26.packagingchangeincidentsystemtests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.ExampleSystemTestExtension
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.InitialAndPrepTestLogs

/**
 * Tests that a packaging change is applied correctly to procuring.
 */
class PackagingChangeIncidentSystemTests : ExampleSystemTestExtension() {
    override val name = "PackagingChangeIncidentSystemTest"
    override val description: String = "Tests that a packaging change is applied correctly to procuring."

    override val restaurants: String = "incidentpackagingprocure/restaurants.json"
    override val scenario: String = "incidentpackagingprocure/scenario.json"
    override val food: String = "incidentpackagingprocure/food.json"

    override val logLevel = "DEBUG"
    override val maxTicks: Int = 48

    override suspend fun run() {
        val chicken = "Chicken"
        val potato = "Potato"
        val gram = "g"
        skipUntilString(InitialAndPrepTestLogs.prepStart(1))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 2000, gram, chicken))

        skipUntilString(InitialAndPrepTestLogs.pantryProcured(2, 1000, gram, potato))
        assertNextLine(InitialAndPrepTestLogs.pantryRestocked(2))

        skipUntilString(InitialAndPrepTestLogs.pantryRemoved(1, 2000, gram, chicken))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(1, 2000, gram, chicken))

        skipUntilString(InitialAndPrepTestLogs.pantryRemoved(2, 1000, gram, potato))
        assertNextLine(InitialAndPrepTestLogs.pantryProcured(2, 700, gram, potato))
    }
}
