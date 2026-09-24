package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.SystemTestSELab26

fun denizTests(): List<SystemTestSELab26> = listOf(
    PlanningSystemTest(),
    CookStaffSystemTest(),
    MenuSystemTest(),
    OrderingSystemTest(),
    PreferencesSystemTest(),
    ServingSystemTest(),
    EventEscortSystemTest(),
    PartialEscortSystemTest(),
    CasualDineInSystemTest(),
    CasualBrowsingSystemTest(),
    EventBrowsingSystemTest(),
    EventVisitSystemTest(),
)
