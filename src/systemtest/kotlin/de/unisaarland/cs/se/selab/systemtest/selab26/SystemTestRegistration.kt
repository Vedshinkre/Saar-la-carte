package de.unisaarland.cs.se.selab.systemtest.selab26

import de.unisaarland.cs.se.selab.systemtest.selab26.basictests.ExampleEmptyTickOneCycleTest
import de.unisaarland.cs.se.selab.systemtest.selab26.basictests.ExamplePreparationAndServingStartTest
import de.unisaarland.cs.se.selab.systemtest.selab26.basictests.ExampleSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.customerparsertests.customerParserSystemTests
import de.unisaarland.cs.se.selab.systemtest.selab26.fulltests.fullScenarioSystemTests
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualAdHocTableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualDeliveryEarlyDecisionTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualNoDecisionNoSpaceTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualTiebreakLowestIdTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CustomerBehaviourDecisionSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryCumulativeDistanceSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryOrderSuccessTestA
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryOrderSuccessTestB
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryOutboundTripSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EatingFinishedAndEscortedSameTickTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.EventReservationConflictTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.ExactStockoutTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.LateCasualNoWaiterSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.OneCookTwoOrdersTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.PantryExpirySystemTest
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
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.RecipeDuplicateIngredientRejectedSystemTest
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
        testSuite.registerTest(UnavailabilityDifferentIngredientsOverlapAcceptedSystemTest())
        testSuite.registerTest(RecipeDuplicateIngredientRejectedSystemTest())
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
        testSuite.registerTest(DeliveryOrderSuccessTestB())
    }

    /**
     * Register the tests you want to run against the validation mutants here!
     * The test only check validation, so they log messages will only possibly
     * be incorrect during the parsing/validation.
     * Everything after 'Simulation start' works correctly
     */
    fun registerSystemTestsMutantValidation(testSuite: SELab26TestSuite) {
        testSuite.registerTest(ExampleSystemTest())
        registeratharvaMutantValidationTests(testSuite)
        customerParserSystemTests().forEach { testSuite.registerTest(it) }
        fullScenarioSystemTests(true).forEach { testSuite.registerTest(it) }
    }

    /**
     * The same as above, but the log message only (possibly) become incorrect
     * from the 'Simulation start' log onwards
     */
    fun registerSystemTestsMutantSimulation(testSuite: SELab26TestSuite) {
        testSuite.registerTest(ExampleSystemTest())

        testSuite.registerTest(EventReservationConflictTest())
        testSuite.registerTest(ExactStockoutTest())
        testSuite.registerTest(OneCookTwoOrdersTest())
        testSuite.registerTest(PantryExpirySystemTest())
        testSuite.registerTest(SingleOrCouple())
        testSuite.registerTest(CustomerBehaviourDecisionSystemTest())
        testSuite.registerTest(UnavailabilityDurationExpirySystemTest())
        testSuite.registerTest(SingleOrCouple()) // testSuite.registerTest(StaffLoadConcentrationSystemTest())
        // testSuite.registerTest(StaffMultiWaiterExhaustionSystemTest())
        testSuite.registerTest(
            StaffLoadBalancingFallbackSystemTest()
        )
        fullScenarioSystemTests(true).forEach { testSuite.registerTest(it) }
        registeratharvaMutantSimulationTests(testSuite)
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
        testSuite.registerTest(RecipeDuplicateIngredientRejectedSystemTest())

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
        testSuite.registerTest(StaffIdResetAcrossEveningsSystemTest())
        testSuite.registerTest(StaffLoadBalancingFallbackSystemTest())
        testSuite.registerTest(RecipeChangeRoundingSystemTest())
        testSuite.registerTest(EatingFinishedAndEscortedSameTickTest())
        testSuite.registerTest(CasualNoDecisionNoSpaceTest())
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
        testSuite.registerTest(RecipeDuplicateIngredientRejectedSystemTest())

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
        testSuite.registerTest(CasualNoDecisionNoSpaceTest())
    }
}
