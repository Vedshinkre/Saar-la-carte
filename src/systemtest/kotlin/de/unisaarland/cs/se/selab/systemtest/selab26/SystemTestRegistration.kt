package de.unisaarland.cs.se.selab.systemtest.selab26

import de.unisaarland.cs.se.selab.systemtest.selab26.basicdishtests.AdaptedBasicDishAcceptedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.basicdishtests.AdaptedBasicDishOnlyAdapterAffectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.basicdishtests.AdaptedBasicDishRecipeChangeSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.basicdishtests.BasicDishMenuCompositionSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.basicdishtests.DuplicateNonBasicDishNameAcceptedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.basicdishtests.EmptyRestaurantRecipesAcceptedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.basicdishtests.SameBasicDishNameSameTypeRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.basicdishtests.SameBasicDishNameTwoTypesRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.basictests.ExampleEmptyTickOneCycleTest
import de.unisaarland.cs.se.selab.systemtest.selab26.basictests.ExamplePreparationAndServingStartTest
import de.unisaarland.cs.se.selab.systemtest.selab26.basictests.ExampleSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.customerparsertests.customerParserSystemTests
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventArrivalCasualTurnedAwaySystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventArrivalCurrentLoadIgnoresEventSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventArrivalRegularGroupsSeatedFirstSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventArrivalReservationsSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventArrivalSeatedByTwoWaitersSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventEscortingEventSeatingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventEscortingLowestLoadFirstSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventEscortingRatingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventEscortingRegularSeatingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventOrderingEventDishOverFavoriteSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventOrderingEventOneStatusSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventOrderingFavoriteDishBeforePreferencesSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventOrderingUnavailableDishNoOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventServingPriorityCasualNotServedWithEventSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventServingPriorityCasualServedNextTickSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventServingPriorityMostCookedMealsFirstSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventServingPrioritySeatingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventServingWaitEventNotServedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventServingWaitOneByOneSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventServingWaitRegularServedFirstSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventServingWaitSeatingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.fulltests.fullScenarioSystemTests
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualAdHocTableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualDeliveryEarlyDecisionTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualDeliveryTimeoutTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualMergeTrimTieBreakSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualNoDecisionNoSpaceTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualTableExclusionSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualTableNoLiftSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualTiebreakLowestIdTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CookChangeNoOrderTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CustomerBehaviourDecisionSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryCumulativeDistanceSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryOrderScenarioTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryOrderSuccessTestA
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryOutboundTripSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EatingFinishedAndEscortedSameTickTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventReservationConflictTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingCapacityCasualAfterEventTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingCapacityCasualRetryTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingCapacityExactFitTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingCapacityFailedEventRatingTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingCapacityOneTooManyTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingLoadPriorityBuildUpTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingLoadPriorityBusiestWaiterFirstTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingLoadPriorityEventNotCountedTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingLoadPriorityFirstEventStatusTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingLoadPrioritySecondEventStatusTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingLoadPrioritySecondWaiterTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingSharedCapacityFirstEventTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingSharedCapacityNoCapacityLeftTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingSharedCapacitySecondEventTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingSharedCapacityStatusTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingSpecExampleRegularBeforeEventTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingSpecExampleSeatingStatusTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingSpecExampleThreeWaitersTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingSpecExampleTickOneCasualTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventSeatingSpecExampleTickTwoCasualTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventTableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.ExactStockoutTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.LateCasualNoWaiterSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.OneCookTwoOrdersTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.PantryExpirySystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.PartialServiceSuccessTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.RecipeChangeAcrossRestaurantsTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.RecipeChangeMinimumAmountSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.RecipeChangeRoundingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.RegularBeforeCasualServingTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.RegularGradualCookServingTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.SimulationLifecycleSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.SingleOrCouple
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.StaffIdResetAcrossEveningsSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.StaffLoadBalancingFallbackSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.StaffLoadConcentrationSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.StaffMultiWaiterExhaustionSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.StatisticsOrderingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.SupplierProcurementSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.TableMergingLifecycleSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.TableReservationConflictsSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.TableReservationMergeSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.TableReservationSingleTableSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.TableTypeRestrictionsSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.UnavailabilityDurationExpirySystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.UnavailableIncidentSupplierProcurementSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.WaitstaffExhaustionTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.AllFilesValidSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.CustomerGroupUnknownRestaurantSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.DuplicateIncidentIdRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.FoodDuplicateIngredientRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.FoodDuplicateRecipeIdRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.FoodEmptyIngredientsRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.FoodInvalidPackagingVolumeRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.FoodRecipeDurationTooShortRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.FoodRecipeMissingIngredientRefRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.OverlappingUnavailabilityRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsDifferentNameSameIdRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsMultipleExecCooksRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsNoBasicDishOfItsTypeRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsNoCookExistsRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsNoDupliacteDishNameRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsNoInvalidTableSize1RejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsNoInvalidTableSizeRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsNoMultipleHeadCooksieEXECRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsNoRecipieExistsRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsNoTableExistsRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsNoWaiterExistsRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsNotUniqueTablesRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsOpeningTickEndLessThanOpeningTickStartRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsPositiveRatingsG0RejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsPositiveRatingsNeg0RejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsRecipiesNotExistRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsSameNameDifferentIdRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RestaurantsTickNotIn1to24RejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.StaffIncidentUnknownRestaurantSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.UnavailabilityDifferentIngredientsOverlapAcceptedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.ValidIncidentsAcceptedSystemTest

/**
 * Used for test registration
 */
object SystemTestRegistration {
    /**
     * Register your tests to run against the reference implementation!
     * This can also be used to debug our system test, or to see if we
     * understood something correctly or not (everything should work
     * the same as their reference implementation)
     */
    fun registerSystemTestsForReferenceImplementation(testSuite: SELab26TestSuite) {
        testSuite.registerTest(ExampleSystemTest())
        testSuite.registerTest(ExactStockoutTest()) //   testSuite.registerTest(ExhaustiveSimpleScenarioTest())
        testSuite.registerTest(OneCookTwoOrdersTest())
        testSuite.registerTest(ExamplePreparationAndServingStartTest())
        testSuite.registerTest(ExampleEmptyTickOneCycleTest())
        testSuite.registerTest(EventReservationConflictTest())
        testSuite.registerTest(WaitstaffExhaustionTest())
        testSuite.registerTest(CasualAdHocTableMergingSystemTest())
        testSuite.registerTest(RegularGradualCookServingTest())
        testSuite.registerTest(RegularBeforeCasualServingTest())
        testSuite.registerTest(CasualTiebreakLowestIdTest())
        testSuite.registerTest(CustomerBehaviourDecisionSystemTest())
        testSuite.registerTest(CasualDeliveryEarlyDecisionTest())
        testSuite.registerTest(DeliveryOutboundTripSystemTest())
        testSuite.registerTest(DeliveryCumulativeDistanceSystemTest())
        testSuite.registerTest(SingleOrCouple()) // testSuite.registerTest(RegularRetrySucceedsSystemTest())
        testSuite.registerTest(SimulationLifecycleSystemTest())
        testSuite.registerTest(StatisticsOrderingSystemTest())
        testSuite.registerTest(SupplierProcurementSystemTest())
        testSuite.registerTest(UnavailableIncidentSupplierProcurementSystemTest())
        testSuite.registerTest(
            PantryExpirySystemTest()
        )
        testSuite.registerTest(LateCasualNoWaiterSystemTest())
        registeratharvaRefrenceTests(testSuite)
        customerParserSystemTests().forEach { testSuite.registerTest(it) }
        fullScenarioSystemTests(false).forEach { testSuite.registerTest(it) }
        testSuite.registerTest(DeliveryOrderSuccessTestA())
        testSuite.registerTest(CasualDeliveryTimeoutTest())
        testSuite.registerTest(PartialServiceSuccessTest())
        testSuite.registerTest(CookChangeNoOrderTest())
        testSuite.registerTest(RecipeChangeAcrossRestaurantsTest())
        testSuite.registerTest(DeliveryOrderScenarioTest())
        testSuite.registerTest(DeliveryOrderScenarioTest())
        testSuite.registerTest(RecipeChangeMinimumAmountSystemTest())
        registerBasicDishTests(testSuite)
    }

    /**
     * Basic-dish tests: the RECIPE incident on an adapted basic dish (the shared ingredient map of
     * `Recipe.copy()`), and probes for the basic-dish validation rules of the food file.
     * not registered for the mutant runs until we know they pass against the reference.
     */
    private fun registerBasicDishTests(testSuite: SELab26TestSuite) {
        testSuite.registerTest(AdaptedBasicDishRecipeChangeSystemTest())
        testSuite.registerTest(AdaptedBasicDishOnlyAdapterAffectedSystemTest())
        testSuite.registerTest(BasicDishMenuCompositionSystemTest())
        testSuite.registerTest(SameBasicDishNameTwoTypesRejectedSystemTest())
        testSuite.registerTest(SameBasicDishNameSameTypeRejectedSystemTest())
        testSuite.registerTest(AdaptedBasicDishAcceptedSystemTest())
        testSuite.registerTest(DuplicateNonBasicDishNameAcceptedSystemTest())
        testSuite.registerTest(EmptyRestaurantRecipesAcceptedSystemTest())
    }

    /**
     * Register the tests you want to run against the validation mutants here!
     * The test only check validation, so they log messages will only possibly
     * be incorrect during the parsing/validation.
     * Everything after 'Simulation start' works correctly
     */
    fun registerSystemTestsMutantValidation(testSuite: SELab26TestSuite) {
        registeratharvaMutantValidationTests(testSuite)
        testSuite.registerTest(ExampleSystemTest())
        customerParserSystemTests().forEach { testSuite.registerTest(it) }
        fullScenarioSystemTests(true).forEach { testSuite.registerTest(it) }
        testSuite.registerTest(RecipeChangeMinimumAmountSystemTest())
        registerBasicDishTests(testSuite)
    }

    /**
     * The same as above, but the log message only (possibly) become incorrect
     * from the 'Simulation start' log onwards
     */
    fun registerSystemTestsMutantSimulation(testSuite: SELab26TestSuite) {
        testSuite.registerTest(ExampleSystemTest())
        registeratharvaMutantSimulationTests(testSuite)
        testSuite.registerTest(EventReservationConflictTest())
        testSuite.registerTest(ExactStockoutTest())
        testSuite.registerTest(OneCookTwoOrdersTest())
        testSuite.registerTest(PantryExpirySystemTest())
        testSuite.registerTest(DeliveryOrderSuccessTestA())
        testSuite.registerTest(CustomerBehaviourDecisionSystemTest())
        testSuite.registerTest(UnavailabilityDurationExpirySystemTest())
        testSuite.registerTest(SingleOrCouple()) // testSuite.registerTest(StaffLoadConcentrationSystemTest())
        // testSuite.registerTest(StaffMultiWaiterExhaustionSystemTest())
        testSuite.registerTest(
            StaffLoadBalancingFallbackSystemTest()
        )
        fullScenarioSystemTests(true).forEach { testSuite.registerTest(it) }
        testSuite.registerTest(CookChangeNoOrderTest())
    }

    /**
     * System-test port of ParserControllerLogTest.kt: exercises ParserController's
     * "Initialization Info" logs end to end through the real CLI/jar pipeline.
     */
    private fun registeratharvaRefrenceTests(testSuite: SELab26TestSuite) {
        testSuite.registerTest(AllFilesValidSystemTest())
        testSuite.registerTest(CustomerGroupUnknownRestaurantSystemTest())
        testSuite.registerTest(StaffIncidentUnknownRestaurantSystemTest())
        testSuite.registerTest(ValidIncidentsAcceptedSystemTest())
        testSuite.registerTest(DuplicateIncidentIdRejectedSystemTest())
        testSuite.registerTest(OverlappingUnavailabilityRejectedSystemTest())
        testSuite.registerTest(UnavailabilityDifferentIngredientsOverlapAcceptedSystemTest())

        testSuite.registerTest(RestaurantsDifferentNameSameIdRejectedSystemTest())
        testSuite.registerTest(RestaurantsSameNameDifferentIdRejectedSystemTest())
        testSuite.registerTest(RestaurantsNotUniqueTablesRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoCookExistsRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoWaiterExistsRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoTableExistsRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoRecipieExistsRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoMultipleHeadCooksieEXECRejectedSystemTest())
        testSuite.registerTest(RestaurantsMultipleExecCooksRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoInvalidTableSizeRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoInvalidTableSize1RejectedSystemTest())
        testSuite.registerTest(RestaurantsOpeningTickEndLessThanOpeningTickStartRejectedSystemTest())
        testSuite.registerTest(RestaurantsTickNotIn1to24RejectedSystemTest())
        testSuite.registerTest(RestaurantsPositiveRatingsG0RejectedSystemTest())
        testSuite.registerTest(RestaurantsPositiveRatingsNeg0RejectedSystemTest())
        testSuite.registerTest(RestaurantsRecipiesNotExistRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoDupliacteDishNameRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoBasicDishOfItsTypeRejectedSystemTest())
        testSuite.registerTest(FoodEmptyIngredientsRejectedSystemTest())
        testSuite.registerTest(FoodDuplicateIngredientRejectedSystemTest())
        testSuite.registerTest(FoodInvalidPackagingVolumeRejectedSystemTest())
        testSuite.registerTest(FoodRecipeDurationTooShortRejectedSystemTest())
        testSuite.registerTest(FoodRecipeMissingIngredientRefRejectedSystemTest())
        testSuite.registerTest(FoodDuplicateRecipeIdRejectedSystemTest())
        testSuite.registerTest(StaffLoadConcentrationSystemTest())
        testSuite.registerTest(StaffMultiWaiterExhaustionSystemTest())
        testSuite.registerTest(StaffIdResetAcrossEveningsSystemTest()) // doesnt pass on refrence
        testSuite.registerTest(StaffLoadBalancingFallbackSystemTest())
        testSuite.registerTest(RecipeChangeRoundingSystemTest())
        testSuite.registerTest(EatingFinishedAndEscortedSameTickTest())
        testSuite.registerTest(CasualNoDecisionNoSpaceTest()) // doesnt pass on refrence
        testSuite.registerTest(CasualMergeTrimTieBreakSystemTest())
        registerTableMergingTests(testSuite)
        registerEventTests(testSuite)
        helperregisteratharvaRefrenceTests(testSuite)
    }

    private fun helperregisteratharvaRefrenceTests(testSuite: SELab26TestSuite) {
        testSuite.registerTest(EventSeatingSpecExampleTickOneCasualTest())
        testSuite.registerTest(EventSeatingSpecExampleTickTwoCasualTest())
        testSuite.registerTest(EventSeatingSpecExampleRegularBeforeEventTest())
        testSuite.registerTest(EventSeatingSpecExampleThreeWaitersTest())
        testSuite.registerTest(EventSeatingSpecExampleSeatingStatusTest())
        testSuite.registerTest(EventSeatingLoadPriorityBuildUpTest())
        testSuite.registerTest(EventSeatingLoadPriorityBusiestWaiterFirstTest())
        testSuite.registerTest(EventSeatingLoadPriorityFirstEventStatusTest())
        testSuite.registerTest(EventSeatingLoadPrioritySecondWaiterTest())
        testSuite.registerTest(EventSeatingLoadPrioritySecondEventStatusTest())
        testSuite.registerTest(EventSeatingLoadPriorityEventNotCountedTest())
        testSuite.registerTest(EventSeatingCapacityExactFitTest())
        testSuite.registerTest(EventSeatingCapacityCasualAfterEventTest())
        testSuite.registerTest(EventSeatingCapacityCasualRetryTest())
        testSuite.registerTest(EventSeatingCapacityOneTooManyTest())
        testSuite.registerTest(EventSeatingCapacityFailedEventRatingTest())
        /**testSuite.registerTest(EventSeatingCapacityFailedKeepsTickLoadTest())
         testSuite.registerTest(EventSeatingCapacityFailedThenRetryTest())**/
        testSuite.registerTest(EventSeatingSharedCapacityFirstEventTest())
        testSuite.registerTest(EventSeatingSharedCapacitySecondEventTest())
        testSuite.registerTest(EventSeatingSharedCapacityNoCapacityLeftTest())
        testSuite.registerTest(EventSeatingSharedCapacityStatusTest())
    }

    /**
     * Front of house rules for EVENT groups (spec 2.2): seating by several waiters, ordering,
     * serving and escorting.
     */
    private fun registerEventTests(testSuite: SELab26TestSuite) {
        testSuite.registerTest(EventArrivalReservationsSystemTest())
        testSuite.registerTest(EventArrivalRegularGroupsSeatedFirstSystemTest())
        testSuite.registerTest(EventArrivalSeatedByTwoWaitersSystemTest())
        testSuite.registerTest(EventArrivalCasualTurnedAwaySystemTest())
        testSuite.registerTest(EventArrivalCurrentLoadIgnoresEventSystemTest())
        testSuite.registerTest(EventOrderingFavoriteDishBeforePreferencesSystemTest())
        testSuite.registerTest(EventOrderingEventOneStatusSystemTest())
        testSuite.registerTest(EventOrderingEventDishOverFavoriteSystemTest())
        testSuite.registerTest(EventOrderingUnavailableDishNoOrderSystemTest())
        testSuite.registerTest(EventServingWaitSeatingSystemTest())
        testSuite.registerTest(EventServingWaitRegularServedFirstSystemTest())
        testSuite.registerTest(EventServingWaitEventNotServedSystemTest())
        testSuite.registerTest(EventServingWaitOneByOneSystemTest())
        testSuite.registerTest(EventServingPrioritySeatingSystemTest())
        testSuite.registerTest(EventServingPriorityMostCookedMealsFirstSystemTest())
        testSuite.registerTest(EventServingPriorityCasualNotServedWithEventSystemTest())
        testSuite.registerTest(EventServingPriorityCasualServedNextTickSystemTest())
        testSuite.registerTest(EventEscortingRegularSeatingSystemTest())
        testSuite.registerTest(EventEscortingEventSeatingSystemTest())
        testSuite.registerTest(EventEscortingLowestLoadFirstSystemTest())
        testSuite.registerTest(EventEscortingRatingSystemTest())
    }

    /**
     * Table merging rules of the front of house (spec 2.2): reservation steps 1-6, table types,
     * ad-hoc CASUAL tables and the life cycle of merged tables.
     */
    private fun registerTableMergingTests(testSuite: SELab26TestSuite) {
        testSuite.registerTest(TableReservationSingleTableSystemTest())
        testSuite.registerTest(TableReservationMergeSystemTest())
        testSuite.registerTest(TableTypeRestrictionsSystemTest())
        testSuite.registerTest(TableReservationConflictsSystemTest())
        testSuite.registerTest(EventTableMergingSystemTest())
        testSuite.registerTest(CasualTableNoLiftSystemTest())
        testSuite.registerTest(CasualTableExclusionSystemTest())
        testSuite.registerTest(TableMergingLifecycleSystemTest())
    }

    /*// testSuite.registerTest(AppendixScenarioTwoPreparationSystemTest())
            // testSuite.registerTest(AppendixScenarioTwoTickStartSystemTest())
            // testSuite.registerTest(AppendixScenarioTwoArrivalSystemTest())
            // testSuite.registerTest(AppendixScenarioTwoKitchenSystemTest())
            // testSuite.registerTest(AppendixScenarioTwoServiceSystemTest())
            // testSuite.registerTest(AppendixScenarioTwoStatisticsSystemTest())*/
    private fun registeratharvaMutantValidationTests(testSuite: SELab26TestSuite) {
        testSuite.registerTest(FoodDuplicateRecipeIdRejectedSystemTest())
        testSuite.registerTest(AllFilesValidSystemTest())
        testSuite.registerTest(CustomerGroupUnknownRestaurantSystemTest())
        testSuite.registerTest(StaffIncidentUnknownRestaurantSystemTest())
        testSuite.registerTest(ValidIncidentsAcceptedSystemTest())
        testSuite.registerTest(DuplicateIncidentIdRejectedSystemTest())
        testSuite.registerTest(OverlappingUnavailabilityRejectedSystemTest())
        testSuite.registerTest(UnavailabilityDifferentIngredientsOverlapAcceptedSystemTest())

        testSuite.registerTest(RestaurantsDifferentNameSameIdRejectedSystemTest())
        testSuite.registerTest(RestaurantsSameNameDifferentIdRejectedSystemTest())
        testSuite.registerTest(RestaurantsNotUniqueTablesRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoCookExistsRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoWaiterExistsRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoTableExistsRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoRecipieExistsRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoMultipleHeadCooksieEXECRejectedSystemTest())
        testSuite.registerTest(RestaurantsMultipleExecCooksRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoInvalidTableSizeRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoInvalidTableSize1RejectedSystemTest())
        testSuite.registerTest(RestaurantsOpeningTickEndLessThanOpeningTickStartRejectedSystemTest())
        testSuite.registerTest(RestaurantsTickNotIn1to24RejectedSystemTest())
        testSuite.registerTest(RestaurantsPositiveRatingsG0RejectedSystemTest())
        testSuite.registerTest(RestaurantsPositiveRatingsNeg0RejectedSystemTest())
        testSuite.registerTest(RestaurantsRecipiesNotExistRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoDupliacteDishNameRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoBasicDishOfItsTypeRejectedSystemTest())
        testSuite.registerTest(FoodEmptyIngredientsRejectedSystemTest())
        testSuite.registerTest(FoodDuplicateIngredientRejectedSystemTest())
        testSuite.registerTest(FoodInvalidPackagingVolumeRejectedSystemTest())
        testSuite.registerTest(FoodRecipeDurationTooShortRejectedSystemTest())
        testSuite.registerTest(FoodRecipeMissingIngredientRefRejectedSystemTest())
        testSuite.registerTest(FoodDuplicateRecipeIdRejectedSystemTest())
    }

    /*// testSuite.registerTest(StaffIdResetAcrossEveningsSystemTest())*/
    private fun registeratharvaMutantSimulationTests(testSuite: SELab26TestSuite) {
        testSuite.registerTest(StaffLoadConcentrationSystemTest())
        testSuite.registerTest(StaffMultiWaiterExhaustionSystemTest())
        testSuite.registerTest(StaffLoadBalancingFallbackSystemTest())
        testSuite.registerTest(RecipeChangeRoundingSystemTest())
        testSuite.registerTest(EatingFinishedAndEscortedSameTickTest())
        testSuite.registerTest(CasualMergeTrimTieBreakSystemTest())
        testSuite.registerTest(RecipeChangeRoundingSystemTest())
        testSuite.registerTest(CasualMergeTrimTieBreakSystemTest())
        registerTableMergingTests(testSuite)
        registerEventTests(testSuite)
        helperregisteratharvaRefrenceTests(testSuite)
    }
}
