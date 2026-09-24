package de.unisaarland.cs.se.selab.systemtest.selab26.anshtests

import de.unisaarland.cs.se.selab.systemtest.selab26.SystemTestSELab26

/** every system test in this folder */
fun anshTests(): List<SystemTestSELab26> = FoodParsingSystemTests.all() + listOf(
    CookingSystemTest(),
    // known issue for why this fails on the reference, cook id assignment is wrong, we've decided not to change it
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

/** food parsing tests that pass against the reference, for the validation mutants */
fun anshValidationMutantTests(): List<SystemTestSELab26> = FoodParsingSystemTests.all()

/** simulation tests that pass against the reference */
fun anshSimulationMutantTests(): List<SystemTestSELab26> = listOf(
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
