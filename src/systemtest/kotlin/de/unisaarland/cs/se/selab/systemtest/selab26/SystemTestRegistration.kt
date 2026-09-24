package de.unisaarland.cs.se.selab.systemtest.selab26

import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.DeliveryInLastThreeTicksIsAcceptedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.DeliveryInLastThreeTicksIsRefusedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.DeliveryLastTicksFilesAcceptedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.EatingOrderIsTwoPassesSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.OrderDishesStartInParallelSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.OrderDishesStartOnePerTickSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.PartialDriverHandoverIsSplitSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.PartiallyLoadedDriverIsBusySystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.PartiallyLoadedDriverStillAvailableSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.PatienceLeavesFiveTicksAfterOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.PatienceLeavesFourTicksAfterOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.ServedOnFourthTickIsNeutralSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.abtests.ServedOnFourthTickIsPositiveSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.anshtests.anshSimulationMutantTests
import de.unisaarland.cs.se.selab.systemtest.selab26.anshtests.anshTests
import de.unisaarland.cs.se.selab.systemtest.selab26.anshtests.anshValidationMutantTests
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
import de.unisaarland.cs.se.selab.systemtest.selab26.deniztests.denizTests
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventArrivalCasualTurnedAwaySystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventArrivalCurrentLoadIgnoresEventSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventArrivalRegularGroupsSeatedFirstSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventArrivalReservationsSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventArrivalSeatedByTwoWaitersSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventEscortingEventSeatingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventEscortingLowestLoadFirstSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventEscortingRatingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventEscortingRegularSeatingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventOrderTakersOrderingLogSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventOrderTakersOrderingStatusSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.eventtests.EventOrderTakersSetupSystemTest
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
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.BrowsingRefusesDineInInLastThreeTicksSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualAdHocTableMergingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualBarExactFitNoMergeSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualBarRestaurantChoiceSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualDeliveryEarlyDecisionTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualMergeTrimTieBreakSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualNoDecisionNoSpaceTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualTableExclusionSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualTableNoLiftSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualTableTypesNeverMixSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CasualTiebreakLowestIdTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CookChangeNoOrderTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CorrectPartialServing1
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CustomerBehaviourDecisionSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.CustomizedBasicDishPrioritySystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryCumulativeDistanceSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryEveningBoundaryNeverRatesSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryMultipleWaitersServeOneOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryOrderSuccessTestA
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryOutboundTripSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryQueueDelayFailedReturnSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DeliveryQueueDelayTimeoutSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.DriverIncidentReducesBrowsingAvailabilitySystemTest
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
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.LowestRankingCookSelectionSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.NonMergeableBarTableBrowsingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.OneCookTwoOrdersTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.PantryAbandonedDishIngredientsNotRefundedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.PantryExpiryAlphabeticalOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.PantryExpirySystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.PartialDeliveryHandoverSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.PartialLeaverEscortedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.RecipeChangeAcrossRestaurantsTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.RecipeChangeMinimumAmountSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.RecipeChangeRoundingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.RegularBeforeCasualServingTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.RegularCapacityBlockedOrderNotLostSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.RegularGradualCookServingTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.RegularRetrySucceedsSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.SimulationLifecycleSystemTest
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
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.WaiterLoadReleasedOnLeavingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.WaitingForFoodSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.WaitstaffExhaustionTest
import de.unisaarland.cs.se.selab.systemtest.selab26.generaltests.deliveryDriverFlowSystemTests
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentNegativePackagingVolumeRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentNoNameRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentNonUniqueIdsRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentZeroAdaptationRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.incidentparsersystemtests.IncidentZeroPackagingVolumeRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.kitchenschedulingtests.BasicDishPrecedenceOverLowerIdSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.kitchenschedulingtests.BatchDoesNotAbsorbLaterArrivingOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.kitchenschedulingtests.LowestRankingCookAssignedAcrossHierarchySystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.kitchenschedulingtests.NonBasicDishesTieBreakByAscendingIdSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.kitchenschedulingtests.TieBreakSameCookTypeByLowestExistingIdSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.kitchenschedulingtests.UncookableDishIsSkippedForCookableOneSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.losttest.CookIdTieBreakHighestIdB
import de.unisaarland.cs.se.selab.systemtest.selab26.losttest.CookIdTieBreakLowestIdA
import de.unisaarland.cs.se.selab.systemtest.selab26.losttest.CorrectPartialServing2
import de.unisaarland.cs.se.selab.systemtest.selab26.losttest.IdleCookReuseA
import de.unisaarland.cs.se.selab.systemtest.selab26.losttest.IdleCookReuseB
import de.unisaarland.cs.se.selab.systemtest.selab26.losttest.KitchenCookAssignmentSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.losttest.RegularFailedTest
import de.unisaarland.cs.se.selab.systemtest.selab26.losttest.SomeDeliveryTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.BothOrdersOfATickStartTogetherSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.ClosedRestaurantStillFinishesDeliverySystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.CookIdsAreReversedOnTheFirstTickSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.CookIdsFollowAssignmentOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.CookOneFinishesLastCookOneTakesNextJobSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.CookOneFinishesLastCookTwoTakesNextJobSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.CookTwoFinishesLastCookOneTakesNextJobSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.CookTwoFinishesLastCookTwoTakesNextJobSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.ExtendedPatienceLeavesInTickEightSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.ExtendedPatienceLeavesInTickFiveSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.ExtendedPatienceLeavesInTickNineSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.ExtendedPatienceLeavesInTickSevenSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.ExtendedPatienceLeavesInTickSixSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.GivenUpDeliveryGivesUpInTickEightSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.GivenUpDeliveryIsDroppedFromTheKitchenSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.GivenUpDeliveryIsStillCookedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.GivenUpDeliveryIsStillDrivenOutAndFailsSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.HandOverContinuesInTheNextTickSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.HandOverCountsInTheServingStatusSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.HandOverIsNotCountedInTheServingStatusSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.HandOverNeverCompletesSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.HandOverWaitsForTheWholeOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.IdleCookWithHigherIdTakesNextJobSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.IdleCookWithLowestIdTakesNextJobSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.MergedTableIsUsedForServingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.MergedTablesAreLoggedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.NoReservationIsLoggedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.NoReservationRatesInTheFirstTickSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.PartialServingWaitsForOneTickSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.PartialServingWaitsForTwoTicksSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.PatienceEndsFiveTicksAfterOrderingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.PatienceEndsFourTicksAfterOrderingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.RatingAfterClosingIsStillCollectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.RatingDoesNotRepeatInTheNextEveningSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.RecipeScopeAdaptedDishAddsToBasicSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.RecipeScopeAdaptedDishReplacesBasicSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.RecipeScopeFilesAcceptedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.RecipeScopeRestaurantOneBuysExactAmountSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.RecipeScopeRestaurantOneBuysWholePackagesSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.SecondOrderOfATickWaitsOneTickSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.StaffIncidentsFailedReservationRatesInTickOneSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.StaffIncidentsIncidentsLoggedBeforePreparationSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.StaffIncidentsLateCasualLeavesSilentlySystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.StaffIncidentsNoCookSeatsButCannotOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.StaffIncidentsRegularStopsAfterTwoFailuresSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.StaffIncidentsRegularTriesAgainAfterOneFailureSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.StaffIncidentsStatisticsSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.UnfinishedDeliveryArrivesInTheLastTicksSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.UnfinishedDeliveryDoesNotRateNextEveningSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.WalkedOutMealIsAbortedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.officehourprobes.WalkedOutMealIsStillCookedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.packagingchangeincidentsystemtests.PackagingChangeIncidentSystemTests
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.AllFilesValidSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.CustomerGroupUnknownRestaurantSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.DuplicateIncidentIdRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.FoodDuplicateIngredientRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.FoodDuplicateRecipeIdRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.FoodEmptyIngredientsRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.FoodInvalidPackagingVolumeRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.FoodRecipeDurationTooShortRejectedSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.parserlogtests.FoodRecipeIngredientUnknownKeyRejectedSystemTest
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
import de.unisaarland.cs.se.selab.systemtest.selab26.ratingsystemtests.CasualGroupRatingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.recipeincidenttests.RecipeChangeEventPlanningSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.recipeincidenttests.RecipeChangeIncidentOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.recipeincidenttests.RecipeChangeLaterEveningSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.recipeincidenttests.RecipeChangeOrderHistorySystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.recipeincidenttests.RecipeChangePersistsAcrossEveningsSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.recipeincidenttests.RecipeChangeScopeSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.recipeincidenttests.RecipeChangeThreeVisitHistorySystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests.CasualConsumptionCarryOverSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests.RegularPlanningFailedOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests.RegularPlanningTwoGroupsSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests.RegularPlanningVisitingPeriodSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests.RegularReservedTableIsTheExactFitSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.regularplanningtests.RegularSeatedWithoutOrderKeepsCountingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests.FailedReservationRatesInFirstTickOfEveningSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests.FailedReservationRatesInTheCorrectEveningSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests.RegularConsecutiveFailuresAcrossDifferentFailureTypesSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests.RegularFailedAttemptsNotResetByInterveningSuccessSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests.RegularPartiallyServedGroupKeepsVisitingSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests.RegularReservationOrderIsAscendingIdNotFileOrderSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests.RegularSeatingFailureRatesNegativeSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests.RegularUneventfulVisitRatesPositiveSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.reservationdecisiontests.RegularUnservedGroupLeavesRatesNegativeSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.staffchangetests.StaffChangeClampThenHireSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.staffchangetests.StaffChangeNoCookStillProcuresSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.staffchangetests.StaffChangeNoWaitstaffSystemTest
import de.unisaarland.cs.se.selab.systemtest.selab26.staffchangetests.StaffChangeOnlyNamedRestaurantSystemTest

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
        testSuite.registerTest(ExactStockoutTest())
        testSuite.registerTest(OneCookTwoOrdersTest())
        testSuite.registerTest(ExamplePreparationAndServingStartTest())
        testSuite.registerTest(ExampleEmptyTickOneCycleTest())
        testSuite.registerTest(EventReservationConflictTest())
        testSuite.registerTest(WaitstaffExhaustionTest())
        testSuite.registerTest(IdleCookReuseA())
        testSuite.registerTest(IdleCookReuseB()) // testSuite.registerTest(RegularRetrySucceedsSystemTest())
        registerSkerdiReferenceTests(testSuite)
        registeratharvaRefrenceTests(testSuite)
        fullScenarioSystemTests(false).forEach { testSuite.registerTest(it) }
        anshTests().forEach { testSuite.registerTest(it) }
        testSuite.registerTest(DeliveryOrderSuccessTestA())
        testSuite.registerTest(PartialDeliveryHandoverSystemTest())
        testSuite.registerTest(CookChangeNoOrderTest())
        testSuite.registerTest(RecipeChangeAcrossRestaurantsTest())
        testSuite.registerTest(RegularFailedTest())
        testSuite.registerTest(CorrectPartialServing1())
        registerOfficeHourProbes(testSuite)
        registerPatienceAbProbes(testSuite)
        testSuite.registerTest(SomeDeliveryTest())
        testSuite.registerTest(CookIdTieBreakHighestIdB())
        testSuite.registerTest(CookIdTieBreakLowestIdA())
        testSuite.registerTest(KitchenCookAssignmentSystemTest())
        testSuite.registerTest(CorrectPartialServing2())
        denizTests().forEach { testSuite.registerTest(it) }
    }

    /** AB tests for the two tick windows fo the serving phase */
    private fun registerPatienceAbProbes(testSuite: SELab26TestSuite) {
        testSuite.registerTest(PatienceLeavesFiveTicksAfterOrderSystemTest())
        testSuite.registerTest(
            PatienceLeavesFourTicksAfterOrderSystemTest()
        ) // confirmed by Vlad's system test but keeping for another run for redundancy
        testSuite.registerTest(ServedOnFourthTickIsPositiveSystemTest())
        testSuite.registerTest(ServedOnFourthTickIsNeutralSystemTest())
        registerOpenQuestionAbProbes(testSuite)
    }

    private fun registerOpenQuestionAbProbes(testSuite: SELab26TestSuite) {
        testSuite.registerTest(DeliveryLastTicksFilesAcceptedSystemTest())
        testSuite.registerTest(DeliveryInLastThreeTicksIsRefusedSystemTest())
        testSuite.registerTest(DeliveryInLastThreeTicksIsAcceptedSystemTest())
        testSuite.registerTest(OrderDishesStartInParallelSystemTest())
        testSuite.registerTest(OrderDishesStartOnePerTickSystemTest())
        testSuite.registerTest(PartialDriverHandoverIsSplitSystemTest())
        testSuite.registerTest(PartiallyLoadedDriverStillAvailableSystemTest())
        testSuite.registerTest(PartiallyLoadedDriverIsBusySystemTest())
    }

    /**
     * Register the tests you want to run against the validation mutants here!
     * The test only check validation, so they log messages will only possibly
     * be incorrect during the parsing/validation.
     * Everything after 'Simulation start' works correctly
     */
    fun registerSystemTestsMutantValidation(testSuite: SELab26TestSuite) {
        testSuite.registerTest(IncidentZeroPackagingVolumeRejectedSystemTest())
        testSuite.registerTest(IncidentNegativePackagingVolumeRejectedSystemTest())
        testSuite.registerTest(IncidentZeroAdaptationRejectedSystemTest())
        testSuite.registerTest(IncidentNonUniqueIdsRejectedSystemTest())
        registeratharvaMutantValidationTests(testSuite)
        testSuite.registerTest(ExampleSystemTest())
        customerParserSystemTests().forEach { testSuite.registerTest(it) }
        registerVladValidationMutantTests(testSuite)
        fullScenarioSystemTests(true).forEach { testSuite.registerTest(it) }
        testSuite.registerTest(IncidentNoNameRejectedSystemTest())
        anshValidationMutantTests().forEach { testSuite.registerTest(it) }
    }

    /**
     * The same as above, but the log message only (possibly) become incorrect
     * from the 'Simulation start' log onwards
     */
    fun registerSystemTestsMutantSimulation(testSuite: SELab26TestSuite) {
        testSuite.registerTest(PackagingChangeIncidentSystemTests())
        testSuite.registerTest(ExampleSystemTest())
        testSuite.registerTest(EventReservationConflictTest())
        testSuite.registerTest(ExactStockoutTest())
        testSuite.registerTest(OneCookTwoOrdersTest())
        registeratharvaMutantSimulationTests(testSuite)
        testSuite.registerTest(PantryExpirySystemTest())
        testSuite.registerTest(DeliveryOrderSuccessTestA())
        testSuite.registerTest(CustomerBehaviourDecisionSystemTest())
        testSuite.registerTest(UnavailabilityDurationExpirySystemTest())
        testSuite.registerTest(SimulationLifecycleSystemTest())
        testSuite.registerTest(StatisticsOrderingSystemTest())
        testSuite.registerTest(SupplierProcurementSystemTest())
        testSuite.registerTest(UnavailableIncidentSupplierProcurementSystemTest())
        testSuite.registerTest(LateCasualNoWaiterSystemTest())
        testSuite.registerTest(KitchenCookAssignmentSystemTest())
        testSuite.registerTest(CorrectPartialServing1()) // testSuite.registerTest(StaffLoadConcentrationSystemTest())
        // testSuite.registerTest(StaffMultiWaiterExhaustionSystemTest())
        testSuite.registerTest(
            StaffLoadBalancingFallbackSystemTest()
        )
        testSuite.registerTest(CookChangeNoOrderTest())
        registerVladSimulationMutantTests(testSuite)
        registerVladConfirmedProbeMutantTests(testSuite)
        registerSkerdiConfirmedMutantTests(testSuite)
        fullScenarioSystemTests(true).forEach { testSuite.registerTest(it) }
        testSuite.registerTest(CasualGroupRatingSystemTest())
        testSuite.registerTest(RegularFailedTest())
        anshSimulationMutantTests().forEach { testSuite.registerTest(it) }
        testSuite.registerTest(KitchenCookAssignmentSystemTest())
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
        testSuite.registerTest(RegularSeatingFailureRatesNegativeSystemTest())
        testSuite.registerTest(RegularUnservedGroupLeavesRatesNegativeSystemTest())
        testSuite.registerTest(RegularConsecutiveFailuresAcrossDifferentFailureTypesSystemTest())
        testSuite.registerTest(BasicDishPrecedenceOverLowerIdSystemTest())
        testSuite.registerTest(NonBasicDishesTieBreakByAscendingIdSystemTest())
        testSuite.registerTest(UncookableDishIsSkippedForCookableOneSystemTest())
        testSuite.registerTest(BatchDoesNotAbsorbLaterArrivingOrderSystemTest())
        testSuite.registerTest(LowestRankingCookAssignedAcrossHierarchySystemTest())
        testSuite.registerTest(TieBreakSameCookTypeByLowestExistingIdSystemTest())
        testSuite.registerTest(CustomizedBasicDishPrioritySystemTest())
        testSuite.registerTest(LowestRankingCookSelectionSystemTest())
        testSuite.registerTest(NonMergeableBarTableBrowsingSystemTest())
        testSuite.registerTest(EventOrderTakersSetupSystemTest())
        testSuite.registerTest(EventOrderTakersOrderingLogSystemTest())
        testSuite.registerTest(EventOrderTakersOrderingStatusSystemTest())
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
        testSuite.registerTest(CasualTableTypesNeverMixSystemTest())
        deliveryDriverFlowSystemTests().forEach { testSuite.registerTest(it) }
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

    /**
     * Probes written after the office hour, one specification statement each, so that a failure
     * against the reference implementation names the rule we read wrongly. The four patience and
     * partial serving tests come in pairs that state the two competing readings of the same rule,
     * so exactly one test of each pair can pass. Not registered for the mutants until the reference
     * run confirms them.
     */
    private fun registerOfficeHourProbes(testSuite: SELab26TestSuite) {
        testSuite.registerTest(PatienceEndsFiveTicksAfterOrderingSystemTest())
        testSuite.registerTest(PartialServingWaitsForOneTickSystemTest())
        testSuite.registerTest(ExtendedPatienceLeavesInTickFiveSystemTest())
        testSuite.registerTest(ExtendedPatienceLeavesInTickSixSystemTest())
        testSuite.registerTest(ExtendedPatienceLeavesInTickEightSystemTest())
        testSuite.registerTest(ExtendedPatienceLeavesInTickNineSystemTest())
        testSuite.registerTest(RecipeScopeRestaurantOneBuysExactAmountSystemTest())
        testSuite.registerTest(RecipeScopeAdaptedDishAddsToBasicSystemTest())
        testSuite.registerTest(WalkedOutMealIsAbortedSystemTest())
        testSuite.registerTest(GivenUpDeliveryIsDroppedFromTheKitchenSystemTest())
        testSuite.registerTest(HandOverWaitsForTheWholeOrderSystemTest())
        testSuite.registerTest(HandOverNeverCompletesSystemTest())
        testSuite.registerTest(HandOverIsNotCountedInTheServingStatusSystemTest())
        testSuite.registerTest(CookIdsAreReversedOnTheFirstTickSystemTest())
        testSuite.registerTest(SecondOrderOfATickWaitsOneTickSystemTest())
        testSuite.registerTest(IdleCookWithLowestIdTakesNextJobSystemTest())
        testSuite.registerTest(CookOneFinishesLastCookTwoTakesNextJobSystemTest())
        testSuite.registerTest(CookOneFinishesLastCookOneTakesNextJobSystemTest())
        testSuite.registerTest(CookTwoFinishesLastCookTwoTakesNextJobSystemTest())
        testSuite.registerTest(CookTwoFinishesLastCookOneTakesNextJobSystemTest())
        testSuite.registerTest(StaffIncidentsFailedReservationRatesInTickOneSystemTest())
        testSuite.registerTest(StaffIncidentsRegularTriesAgainAfterOneFailureSystemTest())
        testSuite.registerTest(StaffIncidentsRegularStopsAfterTwoFailuresSystemTest())
        testSuite.registerTest(StaffIncidentsLateCasualLeavesSilentlySystemTest())
        testSuite.registerTest(StaffIncidentsIncidentsLoggedBeforePreparationSystemTest())
        testSuite.registerTest(StaffIncidentsNoCookSeatsButCannotOrderSystemTest())
        testSuite.registerTest(StaffIncidentsStatisticsSystemTest())
        testSuite.registerTest(IdleCookWithHigherIdTakesNextJobSystemTest())
    }

    /**
     * All of Skerdi's system tests, to run against the reference implementation. The negative control
     * EatingOrderIsPerGroupSystemTest is left out on purpose: it is wrong against the reference by design.
     */
    private fun registerSkerdiReferenceTests(testSuite: SELab26TestSuite) { // simulation and statistics
        testSuite.registerTest(SimulationLifecycleSystemTest())
        testSuite.registerTest(StatisticsOrderingSystemTest()) // supplier and pantry
        testSuite.registerTest(SupplierProcurementSystemTest())
        testSuite.registerTest(UnavailableIncidentSupplierProcurementSystemTest())
        testSuite.registerTest(UnavailabilityDurationExpirySystemTest())
        testSuite.registerTest(PantryExpirySystemTest())
        testSuite.registerTest(PantryExpiryAlphabeticalOrderSystemTest())
        testSuite.registerTest(PantryAbandonedDishIngredientsNotRefundedSystemTest())
        testSuite.registerTest(CustomerBehaviourDecisionSystemTest())
        testSuite.registerTest(BrowsingRefusesDineInInLastThreeTicksSystemTest())
        testSuite.registerTest(CasualBarRestaurantChoiceSystemTest())
        testSuite.registerTest(CasualBarExactFitNoMergeSystemTest())
        testSuite.registerTest(DriverIncidentReducesBrowsingAvailabilitySystemTest())
        testSuite.registerTest(LateCasualNoWaiterSystemTest())
        testSuite.registerTest(WaitingForFoodSystemTest())
        testSuite.registerTest(WaiterLoadReleasedOnLeavingSystemTest())
        testSuite.registerTest(PartialLeaverEscortedSystemTest())
        testSuite.registerTest(EatingOrderIsTwoPassesSystemTest())
        testSuite.registerTest(RegularCapacityBlockedOrderNotLostSystemTest()) // delivery
        testSuite.registerTest(DeliveryQueueDelayTimeoutSystemTest())
        testSuite.registerTest(DeliveryQueueDelayFailedReturnSystemTest())
        testSuite.registerTest(DeliveryMultipleWaitersServeOneOrderSystemTest())
        testSuite.registerTest(DeliveryEveningBoundaryNeverRatesSystemTest())
        testSuite.registerTest(RegularFailedAttemptsNotResetByInterveningSuccessSystemTest())
        testSuite.registerTest(RegularReservationOrderIsAscendingIdNotFileOrderSystemTest())
        testSuite.registerTest(RegularUneventfulVisitRatesPositiveSystemTest())
        testSuite.registerTest(RegularPartiallyServedGroupKeepsVisitingSystemTest())
        testSuite.registerTest(FailedReservationRatesInFirstTickOfEveningSystemTest())
        testSuite.registerTest(FailedReservationRatesInTheCorrectEveningSystemTest())
    }

    private fun registerVladSimulationMutantTests(testSuite: SELab26TestSuite) {
        testSuite.registerTest(CasualAdHocTableMergingSystemTest())
        testSuite.registerTest(RegularGradualCookServingTest())
        testSuite.registerTest(RegularBeforeCasualServingTest())
        testSuite.registerTest(CasualTiebreakLowestIdTest())
        testSuite.registerTest(CasualDeliveryEarlyDecisionTest())
        testSuite.registerTest(DeliveryOutboundTripSystemTest())
        testSuite.registerTest(DeliveryCumulativeDistanceSystemTest())
        testSuite.registerTest(AdaptedBasicDishRecipeChangeSystemTest())
        testSuite.registerTest(AdaptedBasicDishOnlyAdapterAffectedSystemTest())
        testSuite.registerTest(BasicDishMenuCompositionSystemTest())
        testSuite.registerTest(RecipeChangeMinimumAmountSystemTest())
        testSuite.registerTest(RecipeChangeIncidentOrderSystemTest())
        testSuite.registerTest(RecipeChangePersistsAcrossEveningsSystemTest())
        testSuite.registerTest(RecipeChangeLaterEveningSystemTest())
        testSuite.registerTest(RecipeChangeScopeSystemTest())
        testSuite.registerTest(RecipeChangeEventPlanningSystemTest())
        testSuite.registerTest(StaffChangeClampThenHireSystemTest())
        testSuite.registerTest(StaffChangeOnlyNamedRestaurantSystemTest())
    }

    /**
     * My probes that the reference runs confirmed (results 8 and 10). Each of them names one rule, so a
     * mutant that breaks that rule fails exactly one of them. Only the winning reading of each A/B
     * pair is here; the losing ones fail against the reference by design and stay off this list.
     */
    private fun registerVladConfirmedProbeMutantTests(testSuite: SELab26TestSuite) {
        testSuite.registerTest(NoReservationIsLoggedSystemTest())
        testSuite.registerTest(NoReservationRatesInTheFirstTickSystemTest())
        testSuite.registerTest(MergedTablesAreLoggedSystemTest())
        testSuite.registerTest(MergedTableIsUsedForServingSystemTest())
        testSuite.registerTest(PatienceEndsFourTicksAfterOrderingSystemTest())
        testSuite.registerTest(PartialServingWaitsForTwoTicksSystemTest())
        testSuite.registerTest(ExtendedPatienceLeavesInTickSevenSystemTest())
        testSuite.registerTest(RecipeScopeFilesAcceptedSystemTest())
        testSuite.registerTest(RecipeScopeRestaurantOneBuysWholePackagesSystemTest())
        testSuite.registerTest(RecipeScopeAdaptedDishReplacesBasicSystemTest())
        testSuite.registerTest(WalkedOutMealIsStillCookedSystemTest())
        testSuite.registerTest(ClosedRestaurantStillFinishesDeliverySystemTest())
        testSuite.registerTest(RatingAfterClosingIsStillCollectedSystemTest())
        testSuite.registerTest(RatingDoesNotRepeatInTheNextEveningSystemTest())
        testSuite.registerTest(UnfinishedDeliveryArrivesInTheLastTicksSystemTest())
        testSuite.registerTest(UnfinishedDeliveryDoesNotRateNextEveningSystemTest())
        testSuite.registerTest(RecipeChangeOrderHistorySystemTest())
        testSuite.registerTest(RecipeChangeThreeVisitHistorySystemTest())
        testSuite.registerTest(RegularPlanningTwoGroupsSystemTest())
        testSuite.registerTest(RegularPlanningVisitingPeriodSystemTest())
        testSuite.registerTest(RegularPlanningFailedOrderSystemTest())
        testSuite.registerTest(CasualConsumptionCarryOverSystemTest())
        testSuite.registerTest(RegularReservedTableIsTheExactFitSystemTest())
        testSuite.registerTest(RegularSeatedWithoutOrderKeepsCountingSystemTest())
        testSuite.registerTest(StaffChangeNoCookStillProcuresSystemTest())
        testSuite.registerTest(StaffChangeNoWaitstaffSystemTest())
        testSuite.registerTest(RegularRetrySucceedsSystemTest())
        testSuite.registerTest(GivenUpDeliveryGivesUpInTickEightSystemTest())
        testSuite.registerTest(GivenUpDeliveryIsStillCookedSystemTest())
        testSuite.registerTest(GivenUpDeliveryIsStillDrivenOutAndFailsSystemTest())
        testSuite.registerTest(HandOverContinuesInTheNextTickSystemTest())
        testSuite.registerTest(HandOverCountsInTheServingStatusSystemTest())
        testSuite.registerTest(CookIdsFollowAssignmentOrderSystemTest())
        testSuite.registerTest(BothOrdersOfATickStartTogetherSystemTest())
        testSuite.registerTest(IdleCookWithHigherIdTakesNextJobSystemTest())
    }

    /** Skerdi's system tests that passed against the reference (results of commit 91d36d5). */
    private fun registerSkerdiConfirmedMutantTests(testSuite: SELab26TestSuite) {
        testSuite.registerTest(PantryExpiryAlphabeticalOrderSystemTest())
        testSuite.registerTest(WaitingForFoodSystemTest())
        testSuite.registerTest(WaiterLoadReleasedOnLeavingSystemTest())
        testSuite.registerTest(PartialLeaverEscortedSystemTest())
        testSuite.registerTest(EatingOrderIsTwoPassesSystemTest())
        testSuite.registerTest(DeliveryQueueDelayTimeoutSystemTest())
        testSuite.registerTest(DeliveryQueueDelayFailedReturnSystemTest())
        testSuite.registerTest(RegularCapacityBlockedOrderNotLostSystemTest())
        testSuite.registerTest(DeliveryMultipleWaitersServeOneOrderSystemTest())
        testSuite.registerTest(DeliveryEveningBoundaryNeverRatesSystemTest())
        testSuite.registerTest(CasualBarRestaurantChoiceSystemTest())
        testSuite.registerTest(CasualBarExactFitNoMergeSystemTest())
        testSuite.registerTest(RegularFailedAttemptsNotResetByInterveningSuccessSystemTest())
        testSuite.registerTest(RegularReservationOrderIsAscendingIdNotFileOrderSystemTest())
        testSuite.registerTest(RegularUneventfulVisitRatesPositiveSystemTest())
        testSuite.registerTest(RegularPartiallyServedGroupKeepsVisitingSystemTest())
    }

    private fun registerVladValidationMutantTests(testSuite: SELab26TestSuite) {
        testSuite.registerTest(FoodRecipeIngredientUnknownKeyRejectedSystemTest())
        testSuite.registerTest(SameBasicDishNameTwoTypesRejectedSystemTest())
        testSuite.registerTest(SameBasicDishNameSameTypeRejectedSystemTest())
        testSuite.registerTest(AdaptedBasicDishAcceptedSystemTest())
        testSuite.registerTest(DuplicateNonBasicDishNameAcceptedSystemTest())
        testSuite.registerTest(EmptyRestaurantRecipesAcceptedSystemTest())
    }
}
