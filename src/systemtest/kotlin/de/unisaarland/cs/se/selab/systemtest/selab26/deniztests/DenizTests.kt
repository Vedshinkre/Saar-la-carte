package de.unisaarland.cs.se.selab.systemtest.selab26.deniztests

import de.unisaarland.cs.se.selab.systemtest.selab26.SystemTestSELab26
import de.unisaarland.cs.se.selab.systemtest.selab26.regulareatingescortingtests.RegularEatingEscortingOrderedByIdNotFileOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.regulareatingescortingtests.RegularEatingTakesTwoFullTicksThenEscortsSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests.FailedReservationRatesAtFirstOpeningTickSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests.RegularRatedBeforeCasualDespiteLowerIdSystemTest

fun denizTestsForReference(): List<SystemTestSELab26> = listOf(
    DeliveryImpatienceCookBottleneckTest(),
    RegularEatingEscortingOrderedByIdNotFileOrderSystemTest(),
    RegularEatingTakesTwoFullTicksThenEscortsSystemTest(),
    FailedReservationRatesAtFirstOpeningTickSystemTest(),
    RegularRatedBeforeCasualDespiteLowerIdSystemTest(),
    FullSystemTest1(),
    FullSystemTest2(),
    FullSystemTest3(),
    FullSystemTest4(),
    FullSystemTest5()
)

fun denizTestsPassing(): List<SystemTestSELab26> = listOf(
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
