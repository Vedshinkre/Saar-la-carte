package de.unisaarland.cs.se.selab.systemtest.selab26

import de.unisaarland.cs.se.selab.systemtest.selab26.basictests.ExampleSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.customerparsertests.customerParserSystemTests
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.ExactStockoutTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.InvalidRestaurantParserTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.MyParserTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.OneCookTwoOrdersTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.ProcurementLogicTestA
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.ProcurementLogicTestB
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.SequenceTwoSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.SimulationLifecycleSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.StatisticsOrderingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.SupplierProcurementSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.UnavailableIncidentSupplierProcurementSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.WaitstaffExhaustionTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentCookTypeExecForbiddenRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentDriverWithCookTypeRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentNegativeEveningRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentStaffWithIngredientRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentUnavailabilityNegativeDurationRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentUnavailabilityProhibitedPropertyRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentUnavailabilityZeroDurationRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentWaitstaffWithCookTypeRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentZeroStaffNumberRejectedSystemTest
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
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.ValidIncidentsAcceptedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsDifferentNameSameIdTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsNoBasicDishOfItsTypeTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsNoCookExistsTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsNoDuplicateDishNameTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsNoInvalidTableSize1Test
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsNoInvalidTableSizeTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsNoMultipleHeadCooksEXECTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsNoRecipeExistsTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsNoTableExistsTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsNoWaiterExistsTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsNotUniqueTablesTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsOpeningTickEndLessThanOpeningTickStartTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsPositiveRatingsG0Test
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsPositiveRatingsNeg0Test
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsRecipesNotExistTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsSameNameDifferentIdTest
import de.unisaarland.cs.se.selab.systemtest.selab26.restaurantparsertests.RestaurantsTickNotIn1to24Test

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

        // testSuite.registerTest(EventReservationConflictTest())
        testSuite.registerTest(ExactStockoutTest())
        //  testSuite.registerTest(ExhaustiveSimpleScenarioTest())
        testSuite.registerTest(OneCookTwoOrdersTest())
        testSuite.registerTest(WaitstaffExhaustionTest())
        testSuite.registerTest(SequenceTwoSystemTest())
        testSuite.registerTest(SimulationLifecycleSystemTest())
        testSuite.registerTest(StatisticsOrderingSystemTest())
        testSuite.registerTest(SupplierProcurementSystemTest())
        testSuite.registerTest(UnavailableIncidentSupplierProcurementSystemTest())
        testSuite.registerTest(RestaurantsDifferentNameSameIdTest())
        testSuite.registerTest(RestaurantsNoBasicDishOfItsTypeTest())
        testSuite.registerTest(RestaurantsNoCookExistsTest())
        testSuite.registerTest(RestaurantsNoDuplicateDishNameTest())
        testSuite.registerTest(RestaurantsNoInvalidTableSizeTest())
        testSuite.registerTest(RestaurantsNoInvalidTableSize1Test())
        testSuite.registerTest(RestaurantsNoMultipleHeadCooksEXECTest())
        testSuite.registerTest(RestaurantsNoRecipeExistsTest())
        testSuite.registerTest(RestaurantsNoTableExistsTest())
        testSuite.registerTest(RestaurantsNotUniqueTablesTest())
        testSuite.registerTest(RestaurantsNoWaiterExistsTest())
        testSuite.registerTest(RestaurantsOpeningTickEndLessThanOpeningTickStartTest())
        testSuite.registerTest(RestaurantsPositiveRatingsG0Test())
        testSuite.registerTest(RestaurantsPositiveRatingsNeg0Test())
        testSuite.registerTest(RestaurantsRecipesNotExistTest())
        testSuite.registerTest(RestaurantsSameNameDifferentIdTest())
        testSuite.registerTest(RestaurantsTickNotIn1to24Test())

        registerParserControllerLogTests(testSuite)
        customerParserSystemTests().forEach { testSuite.registerTest(it) }

        // new
        testSuite.registerTest(InvalidRestaurantParserTest())
        testSuite.registerTest(MyParserTest())
        testSuite.registerTest(ProcurementLogicTestB())
        testSuite.registerTest(ProcurementLogicTestA())
    }

    /**
     * Register the tests you want to run against the validation mutants here!
     * The test only check validation, so they log messages will only possibly
     * be incorrect during the parsing/validation.
     * Everything after 'Simulation start' works correctly
     */
    fun registerSystemTestsMutantValidation(testSuite: SELab26TestSuite) {
        testSuite.registerTest(ExampleSystemTest())
        registerParserControllerLogTests(testSuite)
        customerParserSystemTests().forEach { testSuite.registerTest(it) }
    }

    /**
     * The same as above, but the log message only (possibly) become incorrect
     * from the 'Simulation start' log onwards
     */
    fun registerSystemTestsMutantSimulation(testSuite: SELab26TestSuite) {
        testSuite.registerTest(ExampleSystemTest())

        //  testSuite.registerTest(EventReservationConflictTest())
        //  testSuite.registerTest(ExactStockoutTest())
        //  testSuite.registerTest(ExhaustiveSimpleScenarioTest())
        testSuite.registerTest(OneCookTwoOrdersTest())
        testSuite.registerTest(WaitstaffExhaustionTest())
    }

    /**
     * System-test port of ParserControllerLogTest.kt: exercises ParserController's
     * "Initialization Info" logs end to end through the real CLI/jar pipeline.
     */
    private fun registerParserControllerLogTests(testSuite: SELab26TestSuite) {
        testSuite.registerTest(AllFilesValidSystemTest())
        testSuite.registerTest(CustomerGroupUnknownRestaurantSystemTest())
        testSuite.registerTest(StaffIncidentUnknownRestaurantSystemTest())
        testSuite.registerTest(ValidIncidentsAcceptedSystemTest())
        testSuite.registerTest(DuplicateIncidentIdRejectedSystemTest())
        testSuite.registerTest(OverlappingUnavailabilityRejectedSystemTest())

        testSuite.registerTest(RestaurantsDifferentNameSameIdRejectedSystemTest())
        testSuite.registerTest(RestaurantsSameNameDifferentIdRejectedSystemTest())
        testSuite.registerTest(RestaurantsNotUniqueTablesRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoCookExistsRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoWaiterExistsRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoTableExistsRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoRecipieExistsRejectedSystemTest())
        testSuite.registerTest(RestaurantsNoMultipleHeadCooksieEXECRejectedSystemTest())
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
        testSuite.registerTest(InvalidRestaurantParserTest())
        testSuite.registerTest(MyParserTest())
        testSuite.registerTest(IncidentWaitstaffWithCookTypeRejectedSystemTest())
        testSuite.registerTest(IncidentDriverWithCookTypeRejectedSystemTest())
        testSuite.registerTest(IncidentNegativeEveningRejectedSystemTest())
        testSuite.registerTest(IncidentStaffWithIngredientRejectedSystemTest())
        testSuite.registerTest(IncidentZeroStaffNumberRejectedSystemTest())
        testSuite.registerTest(IncidentCookTypeExecForbiddenRejectedSystemTest())
        testSuite.registerTest(IncidentUnavailabilityZeroDurationRejectedSystemTest())
        testSuite.registerTest(IncidentUnavailabilityNegativeDurationRejectedSystemTest())
        testSuite.registerTest(IncidentUnavailabilityProhibitedPropertyRejectedSystemTest())
    }
}
