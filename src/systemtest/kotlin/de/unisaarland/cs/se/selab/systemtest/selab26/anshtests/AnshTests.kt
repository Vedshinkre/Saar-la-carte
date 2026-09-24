package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.SystemTestSELab26

/** every system test in this folder */
fun anshTests(): List<SystemTestSELab26> = FoodParsingSystemTests.all() + listOf(
    CookingSystemTest(),
    CasualTablesSystemTest(),
    SeatingLimitsSystemTest(),
    EscortingSystemTest(),
    CasualVisitsSystemTest(),
    BrowsingSystemTest(),
    ClosingSystemTest(),
    StaffChangeSystemTest(),
    UnavailabilitySystemTest(),
    EventsSystemTest(),
)
