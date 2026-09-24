package de.unisaarland.cs.se.selab.systemtest.selab26.generaltests

import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.utils.TickStatusTestLogs

/**
 * CASUAL groups only ever get tables of their own type. Tables: 1 SEPARATED (2), 2 COMMON (2),
 * 3 SEPARATED (2).
 *
 * - group 1, COMMON, 4 seats: there is only one COMMON table, and it cannot be merged with the
 *   SEPARATED ones, so no restaurant is found
 * - group 2, SEPARATED, 4 seats: merges tables 1 and 3 and leaves the COMMON table alone
 * - group 3, COMMON, 2 seats: still finds the untouched COMMON table 2
 */
class CasualTableTypesNeverMixSystemTest : TableMergingSystemTest() {
    override val name = "CasualTableTypesNeverMixSystemTest"
    override val description = "CASUAL groups never get merged or single tables of another table type"
    override val restaurants = "tablemerging/casualtypes/restaurants.json"
    override val scenario = "tablemerging/casualtypes/scenario.json"
    override val food = "tablemerging/food.json"
    override val logLevel = "DEBUG"
    override val maxTicks = 4

    override suspend fun run() {
        skipUntilString(TickStatusTestLogs.tickStart(2, 1))
        assertNextLine(TickStatusTestLogs.restNoDecision(1))
        assertNextLine(TickStatusTestLogs.restDecision(2, 1))
        assertSeatedOnMergedTable(1, 2, listOf(1, 3), 1)

        skipUntilString(TickStatusTestLogs.tickStart(3, 1))
        assertNextLine(TickStatusTestLogs.restDecision(3, 1))
        assertSeatedOnSingleTable(1, 3, 2)
    }
}
