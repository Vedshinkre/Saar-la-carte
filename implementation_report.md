# Implementation Report

## Individual Contributions

Paths, relative to the project root:
- **Implementation:** `src/main/kotlin/de/unisaarland/cs/se/selab`. Packages below are relative to `de.unisaarland.cs.se.selab`, and *(root)* is that package itself.
- **Unit and integration tests (u/i):** `src/test/kotlin`.
- **System tests (sys):** `src/systemtest/kotlin/de/unisaarland/cs/se/selab/systemtest/selab26`.

Each test file holds one class of the same name. For the implementation, only the classes or functions that belong to the feature are listed. Code that one feature contributes to another member's feature is listed under "overlapping features" in each member's section.

| Feature | Implementer | Implementation (package: classes / functions) | Testers | Tests (package: files) |
|---|---|---|---|---|
| F01 Simulation | Vlad | *(root)*: `Main`, `Cliinfo`, `Time`, `Constants`, `Types`; `system`: `Simulation`, `SimulationConfig`; `restaurant.Restaurant`: `simulateTick`, `simulateOpeningHoursTick`; `parsers.ParserController` (integrating the parsers; written by Atharva) | Skerdi, Ved | **Skerdi:** u/i `simulationtests`: `SimulationIntegrationTest`, `SimulationOrchestrationTest`, `SimulationRunTest`, `SimulationStatisticsTest`; sys `generaltests`: `SimulationLifecycleSystemTest`<br>**Ved:** u/i `simulationtests`: `MainIntegrationTest`, `SimulationCoverageTest` |
| F02 Logging | Skerdi | `loggers`: `Logger`, `InitialAndPrepLogger`, `TickStatusLogger`, `FohReceptionLogger`, `FohServiceLogger`, `KitchenLogger`, `DeliveryLogger`, `StatisticsLogger` | Atharva, Deniz | **Atharva:** u/i `loggertests`: `LoggerCoreTest`, `InitialAndPrepLoggerTest`, `TickStatusLoggerTest`, `StatisticsLoggerTest`; u/i `parsertests`: `ParserControllerLogTest`; sys `parserlogtests`: `ParserControllerLogSystemTest`<br>**Deniz:** u/i `loggertests`: `FohReceptionLoggerTest`, `FohServiceLoggerTest`, `KitchenLoggerTest`, `DeliveryLoggerTest` |
| F03 Parse Ingredients & Recipes | Ved | `parsers`: `FoodParser`; `food`: `Ingredient`, `Recipe` | Ansh, Skerdi | **Ansh:** u/i `parsertests`: `FoodParserTest` (ingredients), `FoodParserIntegrationTest` (one test); sys `anshtests`: `FoodParsingSystemTests`<br>**Skerdi:** u/i `parsertests`: `FoodParserTest` (recipes), `FoodParserMissingFieldsTest`, `FoodParserIntegrationTest` (schema tests) |
| F04 Parse Restaurants | Ansh | `parsers`: `RestaurantParser` (and its calls in `ParserController`) | Skerdi, Atharva | **Skerdi:** u/i `restaurantparsertests`: `RestaurantParserBasicDishMenuTest`, `RestaurantParserRejectionTest`, `RestaurantAndCustomerParserIntegrationTest`<br>**Atharva:** u/i `restaurantparsertests`: `RestaurantParserTest`, `RestaurantParserDuplicateAndExecTest`, `RestaurantParserTestDiffrentFoodConfigs` |
| F05 Parse Customers | Deniz | `parsers`: `CustomerParser`, `FoodPreferenceParser` | Vlad, Skerdi | **Vlad:** u/i `customerparsertests`: `CustomerParserSharedFieldsTest`, `FoodPreferenceParserTest`, `CustomerParserIntegrationTest` (fixtures in `customerparsertests/fixtures`); sys `customerparsertests`: `CustomerParserSystemTests`<br>**Skerdi:** u/i `customerparsertests`: `CustomerParserTypeSpecificTest`; u/i `restaurantparsertests`: `RestaurantAndCustomerParserIntegrationTest` |
| F06 Parse Incidents | Atharva | `parsers`: `IncidentParser`, `ScenarioParser` | Ved, Deniz | **Ved:** u/i `incidentparsertests`: `StaffIncidentParserTest`, `UnavailabilityIncidentParserTest`; sys `incidentparsersystemtests`: the 9 staff and unavailability rejection tests (`IncidentCookTypeExecForbiddenRejectedSystemTest` … `IncidentZeroStaffNumberRejectedSystemTest`)<br>**Deniz:** u/i `incidentparsertests`: `PackagingChangeIncidentTest`, `RecipeChangeIncidentTest`; sys `incidentparsersystemtests`: `IncidentNegativePackagingVolumeRejectedSystemTest`, `IncidentZeroPackagingVolumeRejectedSystemTest`, `IncidentZeroAdaptationRejectedSystemTest`, `IncidentNonUniqueIdsRejectedSystemTest`, `IncidentNoNameRejectedSystemTest` |
| F07 Statistics | Ansh | the counters in `restaurant.Kitchen`, `restaurant.FrontOfHouse` and `restaurant.Restaurant` (`getNumberOfCookedMeals`, `getNumberOfCustomersServed`, `getNumberOfCustomersDelivered`); the log calls in `system.Simulation.calculateStatistics` | Vlad, Atharva | **Vlad:** u/i `simulationstatisticsdeliverytests`: `StatisticsDeliveredAndRatedTest`, `StatisticsDeliveredAndRatedIntegrationTest`, `StatisticsFixtures`<br>**Atharva:** u/i `simulationstatisticsdeliverytests`: `StatisticsServedTest`, `StatisticsCookedAndDeliveredRealRestaurantTest` |
| F08 Supplier | Ved | `food`: `Supplier`, `Stock`, `IngredientPackage` | Skerdi, Atharva | **Skerdi:** u/i `stocktests`: `StockTest`, `IngredientPackageTest`; u/i `suppliertests`: `SupplierTest` (shared); sys `generaltests`: `SupplierProcurementSystemTest`, `UnavailableIncidentSupplierProcurementSystemTest`<br>**Atharva:** u/i `suppliertests`: `SupplierTest` (shared) |
| F09 Pantry - Buying Ingredients | Ved | `restaurant.Kitchen`: `planForIngredients`, `collectRecipes`, `createShoppingList`, `addRecipeToShoppingList`; `restaurant.Pantry`: `checkInventory`, `ensureQuantities`, `getPackagesForIngredient` | Skerdi, Deniz | **Skerdi:** u/i `planningtests`: `KitchenPlanningTest`<br>**Deniz:** u/i `planningtests`: `BuyingIngredientsTest`; sys `deniztests`: `PlanningSystemTest` |
| F10 Cooking - Order Queue | Ved | `restaurant.Kitchen`: `processCooking`, `rememberOrderTicks`, `cleanOrderQueue`, `getUniqueRecipes`, `sortRecipesByBasicnessAndId`, `findDishesForRecipe`; `food.Dish` | Deniz, Atharva | **Deniz:** u/i `cookingtests`: `OrderQueueTest`<br>**Atharva:** u/i `cookingtests`: `OrderQueueLifecycleTest` |
| F11 Cooking - Staff & Staff Management | Ved | `actors.Cook`; `restaurant.Kitchen`: `chooseCook`, `findLowestRankingCook`, `isCookEligible`, `getNumericalRank`, `getCooksSorted`, `getNextCookId` | Deniz, Vlad | **Deniz:** u/i `cookstafftest`: `CookStaffTest`; sys `deniztests`: `CookStaffSystemTest`<br>**Vlad:** u/i `kitchentests`: `KitchenSelectionUnitTest`, `KitchenAssignmentIntegrationTest` |
| F12 Cooking - Cooking | Ved | `restaurant.Kitchen`: `assignRecipesToCooks`, `executeCookingStep`, `recordFirstCookedTicks`, `getServableDishesNumber`, `logFinishedMeals`; `actors.CookResult` | Ansh, Atharva | **Ansh:** u/i `cookingtests`: `KitchenCookingProcessTest` (from the start of cooking on), `CookUnitTest` (part); sys `anshtests`: `CookingSystemTest`<br>**Atharva:** u/i `cookingtests`: `CookUnitTest`, `KitchenCookingProcessTest` (up to the start of cooking); sys `kitchenschedulingtests`: `BasicDishPrecedenceOverLowerIdSystemTest`, `BatchDoesNotAbsorbLaterArrivingOrderSystemTest`, `LowestRankingCookAssignedAcrossHierarchySystemTest`, `NonBasicDishesTieBreakByAscendingIdSystemTest`, `TieBreakSameCookTypeByLowestExistingIdSystemTest`, `UncookableDishIsSkippedForCookableOneSystemTest` |
| F13 Restaurant - Menu | Skerdi | `restaurant.Countertop`: `getAvailableRecipes`, `availableAmount` | Ved, Deniz | **Ved:** u/i `menuselection`: `CasualDishSelectionTest`; u/i `countertopintegrationtests`: `CountertopIntegrationTest`<br>**Deniz:** u/i `menuselection`: `RegularGroupDishSelectionTest`, `EventGroupDishSelectionTest`; sys `deniztests`: `MenuSystemTest` |
| F14 FOH - Evening Table Reservation | Deniz | `restaurant.helpers.ArrivalProcessor`: `reserveTables`; `restaurant.FrontOfHouse`: `reserveTables` | Vlad, Atharva | **Vlad:** u/i `reservationtests`: `ReservationOutcomeTest`, `ReservationCancellationTest`<br>**Atharva:** sys `generaltests`: `TableReservationConflictsSystemTest`, `TableReservationMergeSystemTest`, `TableReservationSingleTableSystemTest` |
| F15 FOH - Table Merging & Tables | Deniz | `restaurant.helpers.ArrivalProcessor`: `assignTables`, `trySingleTable`, `mergeTables`, `getSortedPreferredFreeTables`; `restaurant.Table` | Ansh, Ved | **Ansh** (merging for casual groups while seating): u/i `casualtablemergingtests`: `CasualUnreservedMergeTest`, `CasualUnreservedMergeIntegrationTest`, `CasualTableMergingFixtures`; sys `anshtests`: `CasualTablesSystemTest`<br>**Ved** (merging for reservations): u/i `mergetablestest`: `ReservationTableMergingTest`, `ReservationTableMerging2Test`; sys `generaltests`: `EventReservationConflictTest` |
| F16 FOH - Seating | Deniz | `restaurant.helpers.ArrivalProcessor`: `processArrival`, `seatRegularOrCasualGroup`, `assignWaiter`, `successfulSeating`, `rejectForNoWaiter`, `logAndResetSeatingOrderingTickStatus`; `restaurant.FrontOfHouse`: `processArrival`; `restaurant.Restaurant`: `processArrivalSeatingOrdering`, `addToCustomerQueue` | Vlad, Ansh | **Vlad** (outcomes, logs, other groups): u/i `seatingtests`: `SeatingOutcomeTest`, `SeatingIntegrationTest`; sys `generaltests`: `CasualAdHocTableMergingSystemTest`, `RegularRetrySucceedsSystemTest`<br>**Ansh** (choice of waiter, action loads): u/i `seatingtests`: `WaiterSelectionRulesTest`, `AssignWaiterEdgeCaseTest`, `SeatingAndReservationOrderTest`, `FohStaffFixtures`; sys `anshtests`: `SeatingLimitsSystemTest` |
| F17 FOH - Staff Management | Deniz | `actors.Waiter` (ids, action and tick loads); `restaurant.FrontOfHouse`: `getNextWaiterId` | Ved, Atharva | **Atharva:** u/i `fohstaffmanagement`: `WaiterIdLifecycleTest`, `WaiterEnsureIdTest`, `WaiterFirstActionTriggerTest`, `WaiterAssignmentTieBreakTest`, `WaiterWorkloadIntegrationTest`, `WaiterIdEventGroupTest`, `FohStaffFixtures`; sys `generaltests`: `StaffIdResetAcrossEveningsSystemTest`, `StaffLoadBalancingFallbackSystemTest`, `StaffLoadConcentrationSystemTest`, `StaffMultiWaiterExhaustionSystemTest`<br>**Ved:** the unit tests were written together with Atharva and committed by him; sys `generaltests`: `WaitstaffExhaustionTest` |
| F18 FOH - Ordering | Vlad | `customer.CustomerGroup`: `placeOrder`, `registerDish`, `orderingSequence`, `WaiterRota`; `food.Order`; `restaurant.Countertop`: `reserveIngredients`, `addOrder`; `restaurant.helpers.ArrivalProcessor`: `orderSuccess`, `logPlacedOrder` | Deniz, Skerdi | **Deniz:** u/i `orderingtests`: `OrderingSideEffectsTest`; sys `deniztests`: `OrderingSystemTest`<br>**Skerdi:** u/i `orderingtests`: `OrderingTest` |
| F19 FOH - Serving | Ansh | `restaurant.helpers.ServingProcessor` (serving in-house tables); `restaurant.FrontOfHouse`: `processServing`; `actors.Waiter`: `serve` | Vlad, Deniz | **Vlad:** u/i `servingoutcometests`: `ServingOutcomeTest`, `ServingIntegrationTest`; sys `generaltests`: `RegularBeforeCasualServingTest`, `RegularGradualCookServingTest`<br>**Deniz:** u/i `servingoutcometests`: `EventServingTest`; sys `deniztests`: `ServingSystemTest` |
| F20 FOH - Delivery | Ansh | `restaurant.helpers.DeliveryProcessor`; `restaurant.helpers.ServingProcessor`: `serveDeliveryGroups`, `serveToDriver`, `claimIdleDriver`, `driverHolding`; `restaurant.FrontOfHouse`: `processDelivering` | Ved, Skerdi | **Ved:** u/i `deliveryservicetest`: `DeliveryIntegrationTest`, `DriverTest`<br>**Skerdi:** u/i `deliveryhandoverandreturntests`: `DeliveryHandOverTest`, `DeliveryMultiOrderWaiterCapacityTest`, `DeliveryOrderingIntegrationTest`, `DeliveryReturnTripTest`, `DeliveryEatingTest`, `DeliveryFixtures`; u/i `deliveryservicetest`: `DriverPoolSelectionTest`; sys `generaltests`: `DeliveryDriverFlowSystemTests`, `DeliveryMultipleWaitersServeOneOrderSystemTest`, `DeliveryQueueDelayTimeoutSystemTest`, `DeliveryQueueDelayFailedReturnSystemTest` |
| F21 FOH - Escorting | Skerdi | `restaurant.helpers.EscortingProcessor`: `processEscorting`, `logEscortedBy`, `dismantleTable`; `actors.Waiter`: `escort`; `restaurant.FrontOfHouse`: `processEscorting` | Deniz, Ansh | **Deniz** (groups with an assigned waiter): u/i `escortingtests`: `EscortingAssignedWaiterTest`, `EscortingAssignedWaiterIntegrationTest`; sys `deniztests`: `PartialEscortSystemTest`; sys `regulareatingescortingtests`: `RegularEatingTakesTwoFullTicksThenEscortsSystemTest`, `RegularEatingEscortingOrderedByIdNotFileOrderSystemTest`<br>**Ansh** (event groups): u/i `escortingtests`: `EscortingEventWaitersUnitTest`, `EscortingEventWaitersIntegrationTest`; sys `anshtests`: `EscortingSystemTest` |
| F22 Customer - Regulars | Deniz | `customer.RegularGroup` | Skerdi, Atharva | **Skerdi:** u/i `planningtests`: `RegularGroupTest`; sys `reservationdecisiontests`: `RegularFailedAttemptsNotResetByInterveningSuccessSystemTest`, `RegularPartiallyServedGroupKeepsVisitingSystemTest`, `RegularReservationOrderIsAscendingIdNotFileOrderSystemTest`, `RegularUneventfulVisitRatesPositiveSystemTest`<br>**Atharva:** u/i `planningtests`: `RegularCustomersKitchenPlanningTest`; sys `reservationdecisiontests`: `RegularConsecutiveFailuresAcrossDifferentFailureTypesSystemTest`, `RegularSeatingFailureRatesNegativeSystemTest`, `RegularUnservedGroupLeavesRatesNegativeSystemTest` |
| F23 Customer - Casual | Vlad | `customer.CasualGroup`: `isVisitingThisTick`, `isVisitingTonight`, `getDeliveryOrderTick`; `customer.CustomerGroup`: `isVisitingThisTick` | Ansh, Deniz | **Ansh** (experience and rating): u/i `customertests`: `CasualRatingUnitTest`, `CasualRatingIntegrationTest`; sys `anshtests`: `CasualVisitsSystemTest`<br>**Deniz** (visit schedule, delivery timing): u/i `customertests`: `CasualVisitScheduleTest`, `CasualVisitScheduleIntegrationTest`; sys `deniztests`: `CasualDineInSystemTest` |
| F24 Customer - Casual Delivery | Ansh | `restaurant.helpers.DeliveryProcessor` (giving up on a delivery); `actors.Driver`; `customer.CasualGroup`: `wantsDelivery` | Atharva, Ved | **Ved:** u/i `casualcustomerdeliverytests`: `CasualCustomerDelivery`; sys `generaltests`: `CasualDeliveryTimeoutTest`<br>**Atharva:** worked on the test logic of `CasualCustomerDelivery` (committed by Ved) |
| F25 Customer - Restaurant Decision | Atharva | `restaurant.BrowsingService` (the decision for casual and event groups); `customer.EventGroup`: `visitingInThreeEvenings` | Skerdi, Vlad | **Skerdi** (event groups): u/i `eventbrowsingtests`: `EventBrowsingDecisionTest`, `EventBrowsingIntegrationTest`; sys `generaltests`: `CustomerBehaviourDecisionSystemTest`, `CasualBarRestaurantChoiceSystemTest`<br>**Vlad** (casual groups): u/i `casualbrowsingtests`: `CasualBrowsingDecisionTest`, `CasualBrowsingIntegrationTest`; sys `generaltests`: `CasualDeliveryEarlyDecisionTest`, `CasualTiebreakLowestIdTest` |
| F26 Customer - Food Preferences | Vlad | `customer.FoodPreference`: `decideDish`, `firstMatchingFavorite`, `mostPreferredIngredients` | Deniz, Ved | **Deniz** (regular and event customers): u/i `menuselection`: `RegularGroupDishSelectionTest`, `EventGroupDishSelectionTest`; sys `deniztests`: `PreferencesSystemTest`<br>**Ved** (casual customers): u/i `menuselection`: `CasualDishSelectionTest`<br>*Also:* Atharva, u/i `dishdecisiontests`: `DishDecisionRuleTest`, `DishDecisionPermutationTest`, `DishDecisionFixtures` |
| F27 Customer - Waiting for Food | Ansh | `restaurant.helpers.EatingProcessor`; `restaurant.FrontOfHouse`: `processEating` | Skerdi, Atharva | **Skerdi:** u/i `waitingforfoodtests`: `WaitingForFoodTest`, `WaitingForFoodIntegrationTest`, `WaitingForDeliveryTest`, `WaitingFixtures`; u/i `servingoutcometests`: `StaggeredOrderWaitWindowTest`; sys `generaltests`: `WaitingForFoodSystemTest`<br>**Atharva:** u/i `waitingforfoodtests`: `WaitingForEventFoodTest` |
| F28 Browsing Service | Atharva | `restaurant`: `BrowsingService`, `RestaurantStats`; `restaurant.FrontOfHouse`: `getAvailableSeats`, `getAvailableDrivers`, `getReservedSeats` | Ansh, Deniz | **Ansh** (event groups): u/i `browsingtests`: `EventBrowsingEligibilityUnitTest`, `EventBrowsingSeatEstimateIntegrationTest`; sys `anshtests`: `BrowsingSystemTest`<br>**Deniz** (casual groups): u/i `casualbrowsingtests`: `CasualBrowsingEligibilityTest`, `CasualBrowsingSeatEstimateIntegrationTest`; sys `deniztests`: `CasualBrowsingSystemTest`, `EventBrowsingSystemTest` |
| F29 Delivery Service | Ansh | `actors.Driver`: `driveTowardsCustomer`, `driveTowardsRestaurant`, `hasReachedDestination`, `handOverToCustomer`, `startReturnTrip`, `finishReturnTrip` | Ved, Vlad | **Ved** (the whole trip, timing, hand-over): u/i `deliveryservicetest`: `DeliveryIntegrationTest`; sys `generaltests`: `DeliveryOrderScenarioTest`, `DeliveryOrderSuccessTestA`, `DeliveryOrderSuccessTestB`<br>**Vlad** (the outbound trip): u/i `deliveryoutboundtests`: `DeliveryOutboundTripTest`, `DeliveryOutboundIntegrationTest`; sys `generaltests`: `DeliveryOutboundTripSystemTest`, `DeliveryCumulativeDistanceSystemTest` |
| F30 End of Evening | Skerdi | `restaurant.FrontOfHouse`: `startFohClosing`, `endFohOpeningTime`, `resetWaiters`, `resetDrivers`; `restaurant.helpers.EscortingProcessor`: `escortAllAtClosing`; `restaurant.Restaurant`: `endEvening`, `endOfOpeningTime`; `restaurant.Kitchen`: `resetKitchen` | Atharva, Ansh | **Atharva** (end of the opening time): u/i `eveningclosetests`: `FrontOfHouseClosingTest`, `KitchenResetTest`<br>**Ansh** (end of the evening): u/i `eveningclosetests`: `EveningToEveningIntegrationTest`, `EveningDriverBoundaryTest`, `ClosingCompositionTest`, `EveningCloseFixtures`; sys `anshtests`: `ClosingSystemTest` |
| F31 Incident - Staff Change | Vlad | `incidents`: `StaffChangeIncident`; `actors`: `RestaurantStaff` | Ved, Ansh | **Ved** and **Ansh** (written together, committed by Ved): u/i `staffchangeincidenttests`: `StaffChangeIncidentTest`<br>**Ved:** sys `generaltests`: `CookChangeNoOrderTest`<br>**Ansh:** sys `anshtests`: `StaffChangeSystemTest` |
| F32 Incident - Recipe Change | Vlad | `incidents`: `RecipeChangeIncident` | Skerdi, Ved | **Skerdi** (edge cases): u/i `recipechangeincidenttest`: `RecipeChangeIncidentTest`<br>**Ved** (usual cases, integration): u/i `recipechangeincidenttest`: `RecipeChangeIntegrationTest`, `Recipechangehappycases`; sys `generaltests`: `RecipeChangeAcrossRestaurantsTest` |
| F33 Incident - Packaging Change | Atharva | `incidents`: `PackagingChangeIncident` | Skerdi, Deniz | **Skerdi:** u/i `packagingchangeincidenttests`: `PackagingChangeIntegrationTest`<br>**Deniz:** u/i `packagingchangeincidenttests`: `PackagingChangeIncidentTest`; sys `packagingchangeincidentsystemtests`: `PackagingChangeIncidentSystemTests` |
| F34 Incident - Ingredient Unavailability | Atharva | `incidents`: `UnavailabilityIncident`; `food.Stock`: `applyUnavailableDurations` | Ved, Ansh | **Ansh** and **Ved** (written together, committed mostly by Ansh): u/i `unavailabilityincidenttests`: `UnavailabilityIncidentTest`, `UnavailabilityIncidentIntegrationTest`<br>**Ansh:** sys `anshtests`: `UnavailabilitySystemTest` |
| P01 Pantry - Best Before, Reservations | Ved | `restaurant.Pantry`: `throwExpiredIngredients`, `reserveIngredients`, `reserveSingleIngredient` | Atharva, Skerdi | **Atharva** and **Skerdi** (shared file): u/i `pantrytests`: `PantryTest`<br>**Skerdi:** sys `generaltests`: `PantryExpirySystemTest`, `PantryExpiryAlphabeticalOrderSystemTest`, `PantryAbandonedDishIngredientsNotRefundedSystemTest` |
| P02 FOH - Event Seating | Deniz | `restaurant.helpers.ArrivalProcessor`: the EVENT part of `processArrival` (seating with several waiters at the reserved, possibly merged table) | Ansh, Vlad | **Ansh** (choice of waiters, action loads): u/i `eventseatingtests`: `EventSeatingWaiterAssignmentTest`, `EventSeatingIntegrationTest`; u/i `seatingtests`: `EventWaiterCyclingTest`; sys `anshtests`: `EventSeatingSystemTest`<br>**Vlad** (outcomes, logs, other groups): u/i `eventseatingoutcometests`: `EventSeatingOutcomeTest`, `EventSeatingOutcomeIntegrationTest`, `EventSeatingFixtures` |
| P03 FOH - Event Other Actions | Atharva | `customer.EventGroup`: `placeOrder`; `restaurant.FrontOfHouse`: `recruitWaitersForEventGroup`, `recruitWaitersOrderedBy`, `filterWaitersWithFreeCapacity`, `recruitWaiterForServing`; `restaurant.helpers.EscortingProcessor`: `escortEventGroup`, `escortAssignedGroup`, `escortWith`; `actors.Waiter`: `escortEventGroups` | Ansh, Vlad | **Ansh** (choice of waiters for escorting and serving): u/i `eventactiontests`: `EventEscortingAndServingUnitTest`, `EventEscortingAndServingIntegrationTest`<br>**Vlad** (ordering): u/i `eventorderingtests`: `EventOrderingTest`, `EventOrderingIntegrationTest`, `EventOrderingFixtures` |
| P04 Customer - Events | Skerdi (shared in the group) | `customer.EventGroup` (written by Atharva, with parts by Vlad and Deniz); `restaurant.helpers.ServingProcessor`: `serveEventTable` (Skerdi) | Deniz, Ansh | **Deniz** (ordering, event favourite dish): u/i `customertests`: `EventCustomerOrderingTest`, `EventCustomerOrderingIntegrationTest`; sys `deniztests`: `EventVisitSystemTest`, `EventEscortSystemTest`<br>**Ansh** (booking, arrival, choice of the event dish): u/i `customertests`: `EventCustomerScheduleUnitTest`, `EventCustomerScheduleIntegrationTest`; sys `anshtests`: `EventsSystemTest` |
| P05 Customer - Rating | Skerdi | `restaurant.helpers.RatingProcessor`; `customer.CustomerGroup`: `determineRating`; `customer.CasualGroup`: `determineRating`; `restaurant.FrontOfHouse`: `processRatings` | Deniz, Ved | **Deniz** (casual groups): u/i `ratingtests`: `CasualGroupRatingTest`; sys `ratingsystemtests`: `CasualGroupRatingSystemTest`; sys `reservationdecisiontests`: `FailedReservationRatesAtFirstOpeningTickSystemTest`, `RegularRatedBeforeCasualDespiteLowerIdSystemTest`<br>**Ved** (regular and event groups): u/i `ratingtests`: `RegularEventGroupRatingTest` |


### Skerdi Cuka


### 1.1 Implementation

| Feature | What I implemented | Where the code lies |
|---|---|---|
| F02 Logging | All log messages of the simulation: log levels, output handle, the formatting rules of the specification (ids sorted ascending, `key:value` lists), one logger per phase | package `loggers`: `Logger`, `InitialAndPrepLogger`, `TickStatusLogger`, `FohReceptionLogger`, `FohServiceLogger`, `KitchenLogger`, `DeliveryLogger`, `StatisticsLogger` |
| F13 Restaurant menu | Which dishes can currently be ordered (ingredients in the pantry and not reserved, expired packages ignored, an eligible cook exists) | `restaurant.Countertop` |
| F21 FOH - escorting | Escorting of finished and unserved customers by their waiter (limit of 10 per tick, partial escorting, releasing the waiter's load, taking down the tables) for REGULAR and CASUAL groups; EVENT group escorting is Atharva's (P03), see section 2 | `restaurant.helpers.EscortingProcessor`, `actors.Waiter` |
| F30 End of evening | End of the opening time and of the evening: closing ratings, escorting everyone out, dropping reservations, separating tables, resetting waiters, drivers, customers and the kitchen for the next evening | `restaurant.Restaurant` (end of opening time / evening), `FrontOfHouse` (closing), `Kitchen.resetKitchen` |
| P05 Customer - rating | Rating of a group from its experience and rating likelihood, the restaurant's rating counters, rating logs, ratings at closing | `restaurant.helpers.RatingProcessor`, `customer.CustomerGroup` / `CasualGroup` (`determineRating`) |
| P04 Customer - events | Shared with other members, see section 2. I did not write `EventGroup` | - |

### 1.2 Testing

I am tester of F01, F03, F04, F05, F08, F09, F18, F20, F22, F25, F27, F32, F33 and P01 (as in `feature_assignments.yaml`). For these features I wrote about 300 unit and integration test methods and 29 system tests. The main things I tested:

- **F01 Simulation:** the whole run through the real `main`: the log framing of the specification for different `maxTicks`, evenings and ticks restarting, restaurants simulated in ascending id, the statistics lines, the incidents applied before each evening.
- **F03 / F04 / F05 Parsers:** recipes (duration bounds, unique ids, basic dishes), the basic dish menu of a restaurant, the customer group types (REGULAR opening hours, CASUAL delivery lead time, EVENT favourite dishes), missing fields and unknown values, and the order of parsing and validation.
- **F08 / F09 / P01 Supplier and pantry:** buying whole packages, the kitchen planning (last three visits of regulars, favourite dish of events, the estimate per menu dish), expiry of ingredients, the priority of open packages and the earliest best-before date, and unavailable ingredients.
- **F18 Ordering:** the order of the customers within a group, dishes running out during an order, customers who find no dish, waiters at their order limit, the ordering and seating status logs.
- **F20 Delivery:** hand-over of meals to the drivers (also split across waiters and ticks), driving and return trip, eating after delivery, deliveries that arrive too late or fail.
- **F22 Regulars:** visiting periods, the last three visits, failed attempts and stopping after two, ratings of regular groups.
- **F25 Restaurant decision:** the choice of restaurant for CASUAL and EVENT groups (ratings, seats, dietary restrictions, last opening ticks, drivers) and that events reserve their tables on the event evening.
- **F27 Waiting for food:** how long customers wait, who leaves, the waiter's load when customers leave, partly served groups, eating time and given-up deliveries.
- **F32 / F33 Incidents:** recipe amounts (rounding down, never below one, all restaurants) and packaging volume changes with the following purchases.
- **System tests (29):** they were checked against the reference implementation, and the confirmed ones are registered for the mutants.
    - Simulation and statistics (2): log framing of a run, statistics ordered by restaurant id.
    - Supplier and pantry (6): procurement, unavailable ingredients and their duration, expiry on the right evening, alphabetical order of the removed ingredients, ingredients of abandoned dishes.
    - Restaurant decision (5): the browsing rules for CASUAL and EVENT groups, no dine-in in the last three opening ticks, a driver incident lowering the available drivers, casual groups on bar tables.
    - Waiting and leaving (6): patience, waiter load released when customers leave, escorting of partly served groups, log order of leaving and eating, late arrivals without a free waiter, orders blocked by the waiter's capacity.
    - Delivery (4): a delayed order that times out, the return of a failed delivery, an order split across waiters, a delivery at the end of the evening.
    - Regular groups (6): failed reservations and their rating in the right tick and evening, consecutive failures, reservation by ascending id, partly served groups, a positive rating.

---

## Adjustments from the Implementation Plan

### Skerdi Cuka
- **P04 (Customer - events) was shared with other members.** The specification describes events in several places (restaurant decision, reservation, seating, ordering, serving, escorting, rating), and most of these are covered by other features (F25, P02, P03, P05). It was ambiguous what P04 itself should contain, so we decided in the group to share it with members who had less contribution so far. `EventGroup` was written by Atharva, Vlad and Deniz. My part is the rating of EVENT groups (in P05) and the tests of the event restaurant decision (F25).
- **F21: the escorting of EVENT groups was implemented by Atharva as part of P03 (event other actions),** as we decided in the group (Mon 14 Sep). While adding it to `EscortingProcessor` he also edited the REGULAR and CASUAL escorting in the same class (escorting by the assigned waiter and the log of the escorted customers), so `git blame` shows these lines under his name. They belong to my F21 work; we discussed the changes together and they should be attributed to me.
- **F30:** It touches the kitchen, the front of house and the customers, and was changed afterwards by the specification adjustments 16, 17 and 20. It was planned for one day (Mon 14 Sep); I started it on Tue 15 Sep and worked on it until Wed 23 Sep.
- **Structure changed from the design.** Escorting and rating were planned as methods of `FrontOfHouse`. On Tue 15 Sep I moved them into `EscortingProcessor` and `RatingProcessor`, next to the other processors of the front of house.
- **Schedule.** F13 was done on Fri 11 Sep (planned Sat 12 Sep). F21 got its first version on Mon 14 Sep (planned Sun 13 Sep). P05 was started on Mon 14 Sep together with escorting (planned Wed 16 Sep). F02 was done on Thu 10 Sep as planned and corrected until Thu 17 Sep.
- **Testing started later than planned.** The plan asked for tests from Sat 12 Sep on. My first system tests are from Wed 16 Sep, and most tests of my testing features were written between Thu 17 Sep and Wed 23 Sep. On Mon 21 Sep I replaced tests that only repeated what the JSON schema already enforces with new ones. From Mon 21 Sep the system tests were written to find out why we fail the mandatory tests and were checked against the reference implementation.
- **Tests outside my assigned testing features:** `WaiterEscortTest` (F21, which I implemented), `SimulationStatisticsTest` (F07) and `StaggeredOrderWaitWindowTest` (serving wait window, F19).
- The work continued after the planned end (Mon 21 Sep) until Thu 24 Sep with test corrections, fixes after reference runs and the mutant registration.


## Detailed Timeline

### Deniz Firat Sag

- **Day 1:**
  I and my teammates worked on the project skeleton.
- **Day 2:**
  I completed `F05 (Parse Customers)` with a parser for each customer group type (regular, event and casual) as well as
  a parser for food preferences. On **Saturday**, I completed `F14 (FOH - Evening Table Reservation)` and
  `F15 (FOH - Table Merging & Tables)`.
- **Day 3:**
  I wrote all the parts of `F16 (FOH - Seating)`, `F17 (FOH - Staff Management)` and `P02 (FOH - Event Seating)` that
  could be written independently of the progress we had made and added experience changes into the seating process as
  part of `F22 (Customer - Regulars)`.
- **Day 4:**
  I split the welcoming part (`F16`) - that I wrote - of `FrontOfHouse` into `ArrivalProcessor` to reduce the number of
  functions in `FrontOfHouse` so as to make it easier and more manageable to work individually without constant merge
  conflicts. I also made minor fixes in the `CustomerParser` and fixed/updated the `ArrivalProcessor` in accordance with
  the progess we had made.
- **Day 5:**
  I added the periodic visiting functionality to `RegularGroup` as part of `F22 (Customer - Regulars)`, completing it,
  fixed 2 validation issues in the `CustomerParser` and refactored parts of `ArrivalProcessor` to make it more
  extensible and easier to reason about.
- **Day 6:**
  I fixed a minor parsing issue in the `FoodPreferenceParser`, relaxed validation by removing a validation function,
  added further validation to `CustomerParser`, fixed a logging and mutability bug in `ArrivalProcessor` and added order
  history to `RegularGroup`.
- **Day 7:**
  I spent the day fixing bugs in `ArrivalProcessor` and `RegularGroup`. On **Saturday**, I continued with fixing bugs in
  `ArrivalProcessor`. On **Sunday**, I fixed a minor bug in `ArrivalProcessor` and wrote unit tests for `F02 (Logging)`.
- **Day 8:**
  I made minor fixes, wrote unit, integration and system tests for `F33 (Incident - Packaging Change)` and unit and
  system tests for `F06 (Parse Incidents)`.
- **Day 9:**
  I wrote more unit and system tests for `F06 (Parse Incidents)`, unit, integration and system tests for
  `P05 (Customer - Rating)`
- **Day 10:**
  I wrote a system test. Then, I wrote unit and integration tests for `F28 (Browsing Service)`,
  `F13 (Restaurant - Menue)`, `F26 (Customer - Food Preferences)`, `F21 (FOH - Escorting)`, `P04 (Customer - Events)`.
- **Day 11:**
  I wrote unit and integration tests for `F23 (Customer - Casual)` and system tests for my features that I had not
  written system tests for with the exception of `F02 (Logging)`, for which it does not make sense to write system
  tests. I then wrote further system tests aiming to catch remaining mutants, after which I went on with writing unit
  and integration tests for `F09 (Pantry - Buying Ingredients)`, `F10 (Cooking - Order Queue)`,
  `F11 (Cooking - Staff & Staff Management)`, `F18 (FOH - Ordering)`, `F19 (FOH - Serving)`.
- **Day 12:**
  I finalized my part of the implementation report.

I would like to bring to your attention that, on days, when I seemingly have little contribution, the bug fixes I were
making took a long time, since, being responsible for customer arrivals, I was interacting with many components of the
simulation and had to make sure that the invariants my teammates were expecting were being held. This meant that I had
to spend some time reading through their code to understand it and could not just work on my own code by myself. I would
like to mention here as well that I did not use any generative AI for debugging.

### Skerdi Cuka
### During the week (Mon-Fri)

- **Thu 10 Sep:** started logger classes (F02).
- **Fri 11 Sep:** finished logger; documented the logger, `getAvailableRecipes()` for the menu (F13).
- **Mon 14 Sep:** finished the first escorting version (F21), first version of the rating (P05), moved escorting and rating to the end of the tick processing, handed EVENT group escorting to Atharva, fixed the printing of lists and key-value pairs in the logger.
- **Tue 15 Sep:** start of the end of the evening (F30): closing ratings, cleanup of the front of house, `resetKitchen`; escorting and rating moved into `restaurant.helpers`.
- **Wed 16 Sep:** first version of the end of the opening time and of the evening, drivers removed after the evening, waiter counter reset; first system tests (simulation, supplier, statistics order). We spent a lot of time fixing build and detekt issues, until the project built.
- **Thu 17 Sep:** we spent a lot of time fixing build and detekt issues again, until the project built. Tick status log adapted to the specification adjustments, kitchen reset fixes; food parser unit and integration tests; supplier, pantry and customer parser tests; pantry expiry and unavailability system tests.
- **Fri 18 Sep:** end of evening fixes, customer reset for a new evening, countertop and escorting fixes, simulation integration test.
- **Mon 21 Sep:** delivery unit and integration tests (hand-over, eating, return), waiting-for-food unit, integration and system tests, packaging change and stock tests, ordering, kitchen planning, group behaviour and simulation run tests, replaced the schema-redundant tests, system tests for the failing mandatory tests.
- **Tue 22 Sep:** system tests for the customers' patience, log order of eating and delivery at the end of the evening; unit tests adjusted after the fix of aborted dishes.
- **Wed 23 Sep:** end of evening adapted to specification adjustment 20 (aborted dishes), system tests for delivery, pantry and browsing, fixes after the reference runs, registration of the confirmed system tests for the mutants.
- **Thu 24 Sep:** added tests for the exception cases of the parsers, review of my tests against the specification.

### Over the weekend

- **Sat 12 Sep:** started the escorting code, first unit test of the simulation.
- **Sun 13 Sep:** fixed typos in the log messages.
- **Sat 19 Sep:** system tests for customer behaviour and late casual groups, restaurant and customer parser integration tests, recipe change edge cases, event restaurant decision tests, registration of the system tests for the reference and the mutants.
- **Sun 20 Sep:** worked locally on testing the features I am assigned to.

### Ansh Shekhar Tiwatne

- **Day 1:**
    - worked on project skeleton with team based on our class diagram
    - worked on restaurant parser (F04)

- **Day 2:**
    - completed restaurant parser (F04)
    - started work FOH - serving (F19)
    - realized that we interpreted the partial ordering logic incorrectly in our design; corrected the logic and
      discussed all the changes needed to the Order class with Vlad (as it was his code)
    - decided to focus on serving and eating features and work on the relatively simple statistics (F07) later as it
      depended on code yet to be written by others

- **Sat:**
    - completed FOH - serving (F19)
    - started work on delivery related features (F20, F24, F29)
    - figured remainingTicks was redundant in Driver (in our design) so removed it, this was due to a recently clarified
      forum post on the driver logging distance covered per tick vs accumulated distance of the trip

- **Sun:**
    - completed delivery related features (F20, F24, F29) i.e. `processDelivery()` and the `Driver` class methods in our
      diagrams
    - completed eating related F27 i.e. `processEating()` in our diagrams

- **Day 3:**
    - completed the relatively simple simulation statistics (F07) code (now essentially feature complete!)
    - refined other features and discussed implemented features with team
    - fixed several bugs with team to get project to assemble (many detekt issues were yet to be resolved)

- **Day 4:**
    - with team split up the code for the main functions in FOH into separate files in the helpers directory as FOH was
      getting messy (especially to work in since we have to be careful with attributions and also avoid merge
      conflicts); we also had too many (private) functions in one place which is not good and caused a detekt issue,
      this change purely moved logic into helper classes so we figured it wasn't a large deviation from our design
    - dealt with some trouble regarding git attributions since we made a commit which we then a rolled back only to then
      roll back that rollback all on my laptop (since I had experience with git), which caused others' FOH code to be
      attributed to me, had to fix it by manually emptying out and pasting lines of code again since force pushes that
      could fix this were disabled
    - dedicated the entire remaining day to getting the project to build (with detekt) so we don't waste any test runs
      tomorrow morning

- **Day 5:**
    - fixed parser issues with team
    - dedicated significant time finding a fix for SameBasicDishes test (the only one we were still failing for parsing
      tests) as we believed the parsing error could be causing several component tests to fail, tried several fixes
    - fixed issues with and simplified the parser controller
    - finally started work on my tests for my assigned features (had to wait a little longer than planed for the code to
      settle in a state that it was testable)
    - worked on F03 (parse ingredients & recipes) tests with Skerdi (we will push it tomorrow)

- **Day 6: (CODE REVIEW)**
    - had code review from 11-12
    - Skerdi and I ensured we had good test coverage for F03 (parser ingredients & recipes) for `FoodParser`
    - completed F12 (cooking) tests with Atharva, for CookUnitTest he roughly wrote tests for parts before cooking
      start, I did the tests for after cooking start; for the KitchenCookingProcess (integration) test I took things to
      do within the processCooking function and prepare ingredients, he tested the rest

- **Day 7:**
    - started working on system tests, wrote a few scenarios, ran the simulations with the JSONs and went through the
      logs
    - noted several discrepancies between interaction of our code found when running these scenarios
    - spent the entire day implementing several of these fixes with the team (some queues not populated, improper event
      group ordering waiter assignment), we went a large number component tests passed that night

- **Sat:**
    - divided some test work among teammates I'm testing with
    - helped some teammates with ambiguities in their features
    - improved my big written scenarios, restaurants, and food JSONs from yesterday, ran them against our implementation
      to get logs, made changes to these according to the specification and asserted those in some `fulltests` to run on
      the reference, then caught many mutants with these and used them to pinpoint behavior of our implementation
    - pair programmed tests on F31 (staff change incident) with Ved
    - started work on my part of P02 and P03 feature tests (that I share with Vlad)

- **Sun:**
    - finished P02 and P03 feature tests
    - finished F15 (table merging & tables) tests with Ved, I did the part for unreserved table merging, he did the
      reserved
    - pair programmed tests on F34 (ingredient unavailability) with Ved (still some work left)

- **Day 8:**
    - found several small bugs with the way delivery related code was being used that was causing us to fail delivery
      component tests, worked on comments for suggested fixes, we all fixed it together and passed several additional
      tests
    - divided testing F30 with Atharva, we decided I'll do the strictly end of evening part (endEvening, resetDrivers,
      24-th tick stuff), he'll do end of opening time (startFohClosing, escortAllAtClosing, endOfOpeningTime, and
      kitchen side of it), we'll both also look over each other's tests
    - completed my part of F30 (end of evening) tests
    - the office hour was helpful for the remaining test we were failing, wrote some A/B tests based on feedback to
      check ambiguities against the reference
    - completed F34 (ingredient unavailability) with Ved, I committed the file
    - divided unit testing work with Deniz

- **Day 9:**
    - worked on bugfixes in all the code concerning results from A/B tests
    - Vlad and I wrote some more A/B tests based on feedback from office hours
    - wrote tests for F21 escorting (my event half) also Deniz and I worked on our other shared tests

- **Day 10: (GROUP PASS)**
    - worked on bugfixes again from the new A/B test results, for the failing delivery related tests
    - completed work on my other tests with Deniz
    - worked on some system tests specifically for covering certain features

- **Day 11:**
    - finished work on all leftover system tests relating to my testing responsibilities, registered them on the
      reference to then maybe catch the leftover mutants
    - wrote a few more full system tests covering large and complex scenarios with multiple customers and restaurants,
      running through multiple evenings
    - improved documentation of my code
    - refined the implementation report

- **Day 12:**
    - refined the implementation report

- **Some notes:**
    - I have some really large full system tests with complex multiple evening scenarios (in the `fulltests` folder)
      that cover most of the implementation behavior. I wanted to keep the code for these minimal and maintainable by
      not having several calls to `assertNextLine` in the Kotlin code but rather storing them separately in `.log` files
      where every line is asserted by default and I used ellipses denote a `skipUntilString` (for places with
      ambiguity). This allows me to copy logs from our implementation run on my scenario JSONS and easily make changes
      wherever needed to then test against the reference / catch mutants. This way my actual "Kotlin" code for the full
      system tests was very minimal, so in the office hour I was given feedback to still write some larger system tests,
      so I also have 3 more full tests (with well thought large scenarios) done in the way my teammates did with several
      assert calls in the `anshtests` folder
    - I made some merges from dev to main on GitLab website which were attributed to a duplicate profile of mine. I have
      added a mailmap for this as recommended by our tutor. This only concerns merge commits, all line attributions are
      kept by my single original profile.
    - Ved and I pair programmed good incident tests together, hence one of the two tests was committed by him while the
      other by me. We didn't want spend much time finding a way to split it and thought this wouldn't be a big deal. We
      wanted to clarify it in the report for the record.

### Ved Rahul Shinkre

- **Day 1: September 10, 2026**
    - Implemented basic code for core food inventory domain models: `Ingredient`, `IngredientPackage`, and `Stock`.
    - Implemented the initial prototype of `FoodParser` for reading JSON configuration files.

- **Day 2: September 11, 2026**
    - Implemented entities: `Recipe`, `Dish`, `Supplier`, and `Pantry`.
    - Implemented the basic `Cook` actor class and completed the initial 65% structural skeleton of the `Kitchen`
      component (logs and few implemention logic was missing).
    - Completed the second draft implementation and validation rules for `FoodParser`.
    - Refined stock tracking logic in `Stock`.

- **Over the Weekend: September 12 – September 13, 2026**
    - Introduced the `CookResult` data transfer class to report cook activity, assigned meal totals, and completion
      metrics.
    - Cleaned up formatting, whitespace, and Detekt-compliant spacing across `Ingredient`, `IngredientPackage`,
      `Recipe`, `Stock`, `Cook`, `Supplier`, and `Dish`.
    - Cleaned and refactored `FoodParser`.
    - Structured the kitchen order queue and cook assignment placeholders.
    - Refactored `Stock` to support dynamic recipe and ingredient change incidents.
    - Updated staff role enums and hierarchies (`WAITSTAFF` and `EXEC` rank ordering).
    - Implemented unit tests for `StaffIncidentParser` and `UnavailabilityIncidentParser`.
    - Implemented logging integration across `Kitchen`, `Cook`, and `Pantry`.
    - Re-integrated and validated interactions between `Kitchen`, `Cook`, and `Pantry`.
    - Implemented initial unit tests for table merging logic.

- **Day 3: September 14, 2026**
    - Refactored domain classes (`Ingredient`, `IngredientPackage`, `Stock`, `Dish`, `Recipe`, `Supplier`, `Cook`,
      `Kitchen`, `Pantry`) to eliminate redundant getters and setters and enforce Kotlin property encapsulation as the
      group decided to move on from them .
    - Debugged and resolved kitchen execution and logging errors.
    - Implemented unit tests for `MenuAvailability`.
    - Refined schema parsing logic in `FoodParser` and standardized test file formats by adding log helper functions in
      `utils`.

- **Day 4: September 15, 2026**
    - Developed and registered end-to-end simulation system tests (`EventReservationConflictTest`,
      `ExhaustiveSimpleScenarioTest`, `WaitstaffExhaustionTest`, `OneCookTwoOrdersTest`).
    - Resolved test execution failures and assertions across unit and system test suites.
    - Updated `Cook` execution mechanics to support system test integration.

- **Day 5: September 16, 2026**
    - Authored system tests for staff change incidents and registered them in the test suite.
    - Implemented unit and system tests for `StaffIncidentParser` and `UnavailabilityIncidentParser`.
    - Fixed parser bugs in `FoodParser` and updated corresponding parser test cases.

- **Day 6: September 17, 2026**
    - Relaxed over-strict validation constraints in `FoodParser` and adjusted pantry stockout behaviors.
    - Implemented unit tests for casual customer delivery handling (`CasualCustomerDelivery`).
    - Created, updated, and registered multiple simulation system tests, including A/B system tests to verify ambiguous
      specification behaviors.
    - Refined inventory coordination between `Kitchen` and `Pantry`.

- **Day 7: September 18, 2026**
    - Implemented and adjusted system test variants for simulation edge cases to kill mutants and strengthen assertion
      coverage.
    - Refined state tracking and lifecycle updates across `Cook`, `Kitchen`, `Dish`, and `Pantry`.

- **Over the Weekend: September 19 – September 20, 2026**
    - Implemented integration tests for delivery workflows (`DeliveryIntegrationTest`) and CLI entry point handling
      (`MainIntegrationTest`).
    - Implemented simulation loop unit tests (`SimulationCoverageTest`).
    - Updated unit test suites for `Driver` class transitions and worked together with Ansh on
      `StaffChangeIncidenttest`.
    - Implemented integration tests for recipe adaptation incidents (`RecipeChangeIntegrationTest`).
    - Expanded unit tests for `CasualCustomerDelivery` and reservation table merging rules.
    - Authored, corrected, and registered delivery verification system tests.

- **Day 8: September 21, 2026**
    - Implemented unit tests for regular and event group rating evaluation (`RegularEventGroupRatingTest`).
    - Added unit test coverage for delivery logistics and created an exhaustive kitchen status debugging system test.
    - Resolved Detekt static analysis issues across test and production files.
    - Pair programmed F34 ingredient unavailibility tests with Ansh

- **Day 9: September 22, 2026**
    - Implemented unit tests for casual customer dish selection and countertop recipe availability.
    - Debugged and resolved system tests verifying driver availability logic and kitchen status outputs.
    - Enhanced `Dish` modeling and added system tests covering complex kitchen cooking edge cases.

- **Day 10: September 23, 2026**
    - Added edge-case system tests for cook order scheduling and updated driver unit tests.
    - Collaborated on team unit tests for Feature 13 (with Deniz).
    - Collaborated with team members to resolve failing tests on the shared test server.
    - Added comprehensive KDocs and inline comments across test suites covering Features 1, 6, 13, 15, 20, 24, 26, and
      29.

- **Day 11: September 24, 2026**
    - Added complete class-level KDocs, parameter documentation, and targeted inline comments across all core
      implementation files (`Ingredient`, `IngredientPackage`, `Recipe`, `Stock`, `Supplier`, `Dish`, `Cook`,
      `CookResult`, `Kitchen`) without modifying underlying code logic

### Atharva

**Thu 10 Sep**

- *Implementation:* basic parser controller; first incident-parsing pass — F06.
- *Bugs found/fixed:* undid a bad change to the simulation entry point; detekt auto-corrections applied to the
  incident parser.

**Fri 11 Sep**

- *Implementation:* Parser Controller + Incident Parser beta; wired `ParserController`'s arguments through to
  `FoodParser`; incident parser working end to end.
- *Bugs found/fixed:* detekt issues in the new parser code.
- *Unit/integration tests:* first logger tests, F02.
- *System tests:* first Restaurant Parser system tests.

**Mon 14 Sep**

- *Implementation:* `recruitWaitersForEvents` ~80% done and Event Group minors, F25; Packaging Change Incident
  started, F33; event-customer escorting started, P03; Browsing Service made detekt-clean, F28.
- *Bugs found/fixed:* a function-signature bug; a minor FOH bug.

**Tue 15 Sep**

- *Implementation:* sim-config parsing fix in `ParserController`.
- *Bugs found/fixed:* fixed a bug in `Restaurant`'s end-of-evening flow at Skerdi's request, not my feature (see
  section 1); the simulation could still start after being told a config file was invalid, because the entry point
  wasn't actually checking that flag.
- *Unit/integration tests:* incident-parser cleanup and detekt-driven fixes across the suite; attribution/KDoc fixes
  on tests.
- *System tests:* first parser system test covering the `wasInvalidFile` fix.

**Wed 16 Sep** (28 commits, the busiest weekday)

- *Implementation:* Event Group minors x4, F25; `getCurrentEventDish()` added; two separate rounds of Browsing
  Service updates, F28; Event Escorting update x2, P03; `RestaurantStats` updated to report from browsing decisions.
- *Bugs found/fixed:* `IncidentParser` had two competing edits from Ansh and me on the same file the same day,
  needing a same-day reconciliation rather than one clean pass ("according to ansh", then "according to me").
- *Unit/integration tests:* tests written specifically to debug the parser.
- *System tests:* sequence-diagram-2 unit and system test.

**Thu 17 Sep**

- *Implementation:* Event Group place-order logic, F25; Browsing Service minors, F28.
- *Bugs found/fixed:* a failing test fixed.
- *Unit/integration tests:* cooking tests covering F10/F12 — this is where `CookUnitTest` and
  `KitchenCookingProcessTest` (F12, section 4) picked up most of their content.
- *System tests:* "more than 1 EXEC cook" scenario, F12 testing; restaurant tests; Seq2 system test.

**Fri 18 Sep**

- *Bugs found/fixed:* FOH log corrections x2; a one-line FOH fix; further FOH changes.
- *Unit/integration tests:* Supplier unit test, F08 testing.
- *System tests:* Seq 2/3 tests; Staff Management system tests, F17 testing.

**Mon 21 Sep**

- *Bugs found/fixed:* introduced and fixed the same regression the same day — a morning change accidentally weakened
  the browsing service's check for whether a restaurant actually has room for a walk-in dine-in group, so it could
  point a group at a restaurant that doesn't have enough seats for them. I caught it myself that evening and put the
  proper capacity check back, and in the same fix also made sure an event group's restaurant-availability check looks
  at the correct evening, not just the time of day. Separately, fixed a spot where looking up an event's favourite
  dish could crash the simulation if that lookup ever came back empty, and removed a leftover branch of dead,
  incorrect logic alongside it. This also closed the real gap between spec and implementation that
  `EventOrderingSpecTest` had been documenting since it was written — event groups were no longer capped correctly at
  a waiter's per-tick order limit ("Event Ordering is now fixed", see section 2).
- *Unit/integration tests, system tests:* `CountertopAvailabilityTest` disabled to keep the build green rather than
  fixed on the spot (see section 2's note on this pattern).
- *Bugs found by code review, beyond what a commit diff shows:* a wider audit of the browsing service and event
  ordering turned up more than the one seat-check regression above, with mixed fix status (checked against the
  current source as of this report, not just the day they were found):
    - The restaurant's free-seat count doesn't subtract customers who have already decided to dine in but haven't been
      seated yet, so it can briefly overcount how many seats are actually free later in the same tick — **still open**,
      unconfirmed against the reference implementation.
    - The seat count used for event reservations only looks at what's free *tonight*, so a table reserved for a regular
      customer tonight can make those seats invisible to an event group trying to book several evenings ahead — **still
      open**, and only a plausible reading, since the spec doesn't spell out the intended behaviour.
    - When an event group places an order, it's assigned to whichever waiter happens to have spare capacity rather than
      to the specific waiter who actually seated that customer, which is what the forum's ruling on this spec ambiguity
      requires — **still open**.
    - Event groups' orders weren't showing up in the ordering log at all, and a "nothing to order" line was printed
      twice instead of once — **fixed** at some point after this was found.
    - Event groups could fall back to a generic customer-ordering path that doesn't know about their favourite dish,
      which was the actual reason two spec-derived tests were disabled at the time.

**Tue 22 Sep**

- *Bugs found/fixed:* `EventOrderingSpecTest` fixed for real after Monday's change; available-drivers logic
  corrected, following up on Monday's fix.
- *Implementation:* Browsing Service delivery-side updates, F28.
- *Unit/integration tests:* explicit F22 unit tests x2; explicit F10 order-queue and F30 end-of-evening
  unit/integration tests, `OrderQueueLifecycleTest`/`eveningclosetests`.
- *System tests:* FOH Staff Management + Statistics test, F17/F07 testing; cooking system tests, the
  `kitchenschedulingtests` suite. Also checked a specific case for bar-style tables: the browsing step's free-seat
  count pools every bar table's size together rather than checking any single table's capacity, even though bar
  tables can never be combined — that's not a bug, since the later seating step still correctly turns the group away
  at the door if no single table actually fits them; wrote a test confirming that two-step behaviour (browsing
  accepts, seating correctly rejects) rather than treating the browsing step's looser check as something to fix.
- Cleanup: removed "appendix" tests judged not useful.

**Wed 23 Sep**

- *Bugs found/fixed:* added a calculation meant to estimate when an event group's kitchen prep needed to finish by,
  found it computed the wrong tick, and removed it the same day (see section 2); fixed the driver-availability count
  so it only counts drivers that are actually free right now, not ones still on their way back from a delivery.
- *Unit/integration tests, system tests:* system-test clean-up; two tests flagged as needing correction rather than
  fixed on the spot.

**Thu 24 Sep**

- Cleanup: removed the now-stale commented-out system tests from the Sep 19-21 hardening push.
- Documentation: KDoc/comments added across my implementation files, no logic changes.

**Fri 25 Sep**

- *Bugs found/fixed:* found and fixed the same day while writing the new F27 test — one of the shared test helpers
  only distinguished "regular customers" from "everyone else" for serving order, instead of ranking regular, event,
  and casual customers separately the way the real app does; nothing had run an event group through that helper
  before, so it had gone unnoticed. Fixed it to match the real three-way ranking.
- *Unit/integration tests:* added coverage for event ordering (`EventOrderingSpecTest`), FOH waiter-id/event-group
  (`WaiterIdEventGroupTest`), and restaurant-parser edge cases, plus two brand-new tests — a F07 statistics test
  (`StatisticsCookedAndDeliveredRealRestaurantTest`) and a F27 waiting-for-event-food test (`WaitingForEventFoodTest`).
- Documentation: comments across my tests and system tests.

### Over the weekend (recorded separately, not part of the week)

**Sat 12 Sep**

- *Implementation:* re-added and adjusted `ParserController`; fixed `ScenarioParser`.
- *Bugs found/fixed:* adjusted `UnavailabilityIncident`'s `food.Stock` interaction per Ved's review of the stock
  behaviour, F09/F34 overlap (see section 1).
- *Unit/integration tests:* restaurant-parser testing.

**Sun 13 Sep**

- *Implementation:* Browsing Service (F28) first complete pass; Unavailability Incident (F34) made available — both
  roughly 4-5 days ahead of the plan's Day 6/7 slots (see section 2).
- *Bugs found/fixed:* minor browsing-service fixes the same weekend.

**Sat 19 Sep**

- *Bugs found/fixed:* an escorting-log bug, P03.
- *Implementation:* further event-escorting work, P03.
- *Unit/integration tests:* pantry test updates, P01 testing; fixed a `skipUntilString` misuse in my own test.
- *System tests:* browsing/escorting edge-case tests.
- Cleanup: commented out currently-failing tests to keep the suite green; disabled a failing incident test — both
  examples of the pattern flagged in section 2.

**Sun 20 Sep** (23 commits, the single heaviest day of the project)

- *Implementation:* two more rounds of Browsing Service updates, F28; FOH recruit-waiters fixes x2; Incident Parser
  minors x2.
- *Unit/integration tests:* recipe-change-incident tests; several "Edge Case Test" commits.
- *System tests:* Event Group tests x2; registered the reference/mutant test suites (see section 6), while disabling
  one restaurant-parser test that didn't match the reference implementation's behaviour rather than chasing it
  further that day.

### Vlad Mihai Marciu

#### Week 1

- **Day 1 (Thu, Sep 10):** Started F01 as planned: the time and type classes, `Simulation`, `SimulationConfig` and `CliInfo`, and a first version of `Main` parsing the command-line arguments.
- **Day 2 (Fri, Sep 11):** Continued working on F01 and helped the rest of the team with parsing

#### Over the weekend (Sep 12-13)

- **Saturday:** Finished the first version of F01: completed `Main` and implemented `Simulation`. Started F18: implemented the `Order` class and the countertop (adding orders and reserving ingredients).
- **Sunday:** Looked over teammates implementation progress and code.

#### Week 2

- **Day 3 (Mon, Sep 14):** Continued F18 as planned: `placeOrder` following the sequence diagram. Implemented F26 (dish choice from food preferences), F31 (staff change incident) and F32 (recipe change incident). Added stub functions across the restaurant, front-of-house and customer classes and fixed `detekt` issues to try to make the project build.
- **Day 4 (Tue, Sep 15):** Connected ordering to the arrival stage, so orders reach both the customer and the kitchen's order queue. More `detekt` clean-up.
- **Day 5 (Wed, Sep 16):**
  - F18: ordering and no-ordering logs, delivery orders, `placeOrder` returns when nobody in the group could order.
  - F23: casual visiting behaviour (visiting tonight / this tick, computing the ordering tick of a delivery).
  - Integration: implemented the preparation phase of a restaurant (event and regular table reservations with their logs, free-seat count, up to ingredient planning) and the event restaurant-decision log.
  - Started testing with F05: JSON fixtures, and unit and integration tests for food preferences and customer groups.
  - Helped the rest of the team with detekt clean-up and making the project build
- **Day 6 (Thu, Sep 17):**
  - Testing F05: customer-parser system tests and further unit and integration tests.
  - Integration fixes:
    - customer clean-up that could never run;
    - duplicated no-ordering, no-reservation and pantry logs;
    - regular groups being sent to every restaurant;
    - `--help` in `Main`;
    - sorting customer lists by id;
    - event groups deciding in the first tick.
  - F23: fixed the visiting-tick check. F01: the tick log now uses elapsed ticks.
  - F18: ordering status log, and ordering logs for all customer types; `placeOrder` extended for event groups.
  - As tester of F11 and F14: kitchen unit tests, cooking integration tests, and reservation tests.
  - Managed to make the project build together with the rest of the team
- **Day 7 (Fri, Sep 18):**
  - As tester of F16, F19, F25 and F29:
    - F16: unit, integration and system tests for seating.
    - F19: tests for serving non-event groups.
    - F25: tests for the casual restaurant decision.
    - F29: tests for outgoing deliveries.
  - Fixes: order ids reset per run, stock unavailability refreshed, customers reset before each evening, range check for `maxTicks`, and refusing customers in the last three opening ticks.

#### Over the weekend (Sep 19-20)

- **Saturday:**
  - As tester of P02 and P03: unit and integration tests for event seating and event ordering.
  - Removed or disabled (in case they were not mine) tests  made obsolete by the JSON schemas update.
  - F01: fixed the output-path check.
- **Sunday:**
  - As tester of F07: unit and integration tests for statistics (delivery ratings, end of evening).
  - F32: fixed the recipe change being applied twice to recipes sharing an ingredient, with system tests for it and for basic dishes under recipe changes.
  - Moved the system tests that pass on the reference implementation to the mutant lists.

#### Week 3

- **Day 8 (Mon, Sep 21):**
  - F32: further system tests for the failing recipe-change component tests.
  - F31: system tests for the staff change incident.
  - Fixed available drivers not being refreshed every tick.
  - Ingredient planning for regular groups without an order history: probed the reference implementation with system tests, then fixed it according to the result.
  - Refactored ordering so event ordering can reuse it.
  - Went through the disabled tests and documented why they fail.
  - Wrote probe tests from the office-hour feedback.
- **Day 9 (Tue, Sep 22):**
  - New probe tests after the reference run (extended patience, recipe scope, partial serving).
  - Serving now ignores meals of customers who walked out.
  - System tests for ratings around the end of the evening.
- **Day 10 (Wed, Sep 23):**
  - Registered probe tests for deliveries given up and for hand-overs to drivers split across ticks.
  - Added probes for choosing between two free cooks of the same type.
  - Analysed disabled tests that no longer matched our implementation.
  - Fixed a few deprecated unit tests (mine) so they check the current behaviour.
  - Updated KDoc documentation for all my classes and functions.
- **Day 11 (Thu, Sep 24):**
  - Started working on my part of the implementation report
  - Updated descriptions for my system tests
  - Started doing clean-up in my parts of the implementation
  - Slight changes to unit/integration tests to improve their quality
- **Day 12 (Fri, Sep 25):**
  - Continued with clean-up of my parts of the project(removed comments, unused functions, etc.)
  - Finished my part of the implementation report

### Bob

---

## Usage of Generative AI

**Deniz Firat Sag:**
*Option 2:*
In the implementation plan, I stated that I would not use generative AI in the implementation phase; that statement is
**no longer up-to-date** as I changed my mind and **did use generative AI**. I used Claude Code (Sonnet 5, Opus 5 and
Opus 5.5) and Gemini (3.1 Pro) for writing unit, integration and system tests. I did not use any generative AI for
writing or debugging implementation code (code under `src/main/`).

*Option 2:*
(insert tool names properly; if applicable add links or add version numbers of the used tools)
Alice used the following tools in the implementation phase:
Tool-1 for code completion and tool Tool-2 for ... . In addition, she used Tool-3 for ...

**Ansh:** Option 2
- used **NotebookLM** (Gemini 3.1 Pro I think) where I uploaded the specification and adjustments so I can easily use it for quickly querying any doubts I had (saved me a lot of time of scrolling through pages)
- used **Claude Code** (mostly Sonnet 5, sometimes Opus 5) 1. to build good scenarios for tests (given specifications e.g. x restaurants of y type with z customers or given particular cases) 2. to help refine and better document code I wrote (wanted to get my logic to work as simply with few lines and following Kotlin best practices wherever possible) 3. to quickly write some AB tests after office hours 4. to help debug: did most debugging manually to avoid AI false positive, but used some aid for tricker bugs

**Ved Rahul Shinkre:** Option 2
I used **Gemini** and **ChatGPT** during the implementation phase. These tools were used strictly for debugging test
failure traces, resolving Detekt static analysis issues, improving the current and adding new unit tests , generating
compliant KDoc comments and suggesting descriptive test names. Generative AI was **not** used to design the system,
write core implmentation code, or generate any kind of business logic.
We are aware of the potential dangers of using these tools and take full responsibility for any code, documents, and
other content produced during the group phase.

**Vlad Mihai Marciu:**
*Option 2:*
I used Claude Code with the models Claude Sonnet 5, Claude Opus 5 and Claude Opus 5.5 in the implementation phase. This is different from the implementation plan, which instead listed GitHub Copilot Chat, ChatGPT (GPT-5), Claude (Sonnet 5) and Gemini (3.1 Pro). I did not end up using those tools

Claude Code was used mainly for formatting, testing (generating fixtures and skeletons) and building scenarios for small system tests that compare our behaviour where the specification was ambiguous. All generated code and text was reviewed and adjusted by me before being committed.


**Skerdi:**
*Option 2:*
In the implementation plan I announced GitHub Copilot Chat, ChatGPT (GPT-5), Claude (Sonnet 5) and Gemini (3.1 Pro). I diverted from this: I used only Claude, in the form of Claude Code (Claude Sonnet 5), and very minor usage of Chatgpt.

Claude Code was used for debugging the implementation, for my tests. It was not used to write the implementation of my features.

- review of my unit, integration and system tests against the specification and its adjustments (redundancy, coverage, correctness);
- correcting outdated waiting-for-food and delivery tests, and writing the additional tests for the exception cases of the parsers to raise the coverage;
- debugging of the implementation: finding why tests and system tests failed on our jar or on the reference implementation, for example a delivery driver in `DeliveryProcessor` that was never freed after a given-up delivery (fixed, with a test for it);
- improving documentation of public methods.


In case of option 2, add additional sentences in which you provide more details on which tools you used for which
specific tasks and to which extent.

We are aware of the potential dangers of using these tools and take full responsibility for any code, documents and
other content produced during the group phase.

---

## Feature Implementation and Placement

### Ved Rahul Shinkre

#### Feature Implementations (Core Domain)

* **F03 (Parse Ingredients & Recipes):** Implemented schema parsing, type checking, validation rules, and error
  propagation for food configuration files Food Parser.
* **F08 (Supplier):** Implemented supplier inventory interactions, ingredient procurement calculation, and packaging
  units in `Supplier`, `Stock`, `Ingredient`, and `IngredientPackage`.
* **F09 / P01 (Pantry Inventory & Expiry):** Implemented ingredient storage, best-before tracking, package disposal, and
  restocking in `Pantry`.
* **F10, F11, F12 (Cooking — Queue, Staff Management, Cooking):** Implemented cook hierarchy selection (lowest-ranking
  eligible cook priority, tie-breaking by lowest ID), order queueing, dish batching across multiple orders, tick-by-tick
  preparation, and `CookResult` reporting in `Kitchen`, `Cook`, `CookResult`, `Dish`, and `Recipe`(some of my code got
  credieted to Vlad and we were not able to credit it back to me in Kitchen (where we log the kitchen status during each
  tick)).
  So implemented All of the  `Cook`,`Supplier`, `Ingredient`, and `IngredientPackage`,`CookResult`, `Dish`, `Recipe`,
  and most of the `Kitchen`(with few implementation from Skerdi and Ansh) and `Stock` (where Atharva has a function.)
  classes.

### Ansh Shekhar Tiwatne

- **FO4: parse restaurants (parsing and validating restaurants)**
    - all of `RestaurantParser`, function calls in `ParserController`
- **F07: statistics (performing simulation statistics)**
    - small logger calls in `Simulation`, setting and incrementing statistics variables in `Kitchen`, `Restaurant`,
      `FrontOfHouse`
    - some 7 lines in the `calculateStatistics()` function in `Simulation` somehow went to Vlad (probably after
      resolving a conflict), but we just let it be
- **F19: FOH - serving (serving meals from kitchen)**
    - most of `ServingProcessor`, high level serving calls in `FrontOfHouse`, serving related functions in `Waiter`
    - the `serveEventTable()` function was written by Skerdi as P04 (which includes event customer serving) belongs to
      him
    - the waiter recruiting functions for serving event groups were written by Atharva since P04 needs him to do
      waitstaff selection for all actions for event groups
- **F20: FOH - delivery (bringing meals from kitchen to drivers, managing drivers, restaurant status of drivers)**
    - all of `DeliveryProcessor`, all of `Driver` (shared logic with my F24 and F29)
- **F24: customer - casual delivery (performing casual customer delivery behavior, e.g. restaurant decision, accepting
  food, rating)**
    - all of `DeliveryProcessor`, all of `Driver` (shared logic with my F20 and F29)
    - there was overlap for the "restaurant decision" with F25 which also has "deciding restaurant for casual delivery
      groups", this part was done by Atharva as he had all browsing service logic anyway
    - there was overlap for the "rating" with Skerdi's P05, so we decided that setting delivery related experience will
      be handled by me (similarly for other people's features wherever experience needs to be updated), and then Skerdi
      will stick to logic to calculate the rating based on the set experience
- **F27: customer - waiting for food (waiting for food, eating, giving up)**
    - all of `EatingProcessor`, high level eating calls in `FrontOfHouse`
    - code for the "giving up" part is in other parts of my own features (serving/delivery) based on wherever the "give
      up" is most sensible to be checked/handled e.g. `processGiveUps` in the delivery code
- **F29: delivery service (receiving meals from kitchen, transporting them, giving them to customers, returning)**
    - all of `Driver` (shared logic with my F20 and F24)
    - note that we don't actually have a delivery service class in our design

## Unit test and System Test Placement

### Ved Rahul Shinkre

* **F01 (Simulation):** Authored `MainIntegrationTest.kt` and `SimulationCoverageTest.kt` in package `simulationtests`
  (collaborated on execution flow with Skerdi, where he covered more of the simulation , while i foused more on the main
  and covering remainder of simulation).
* **F06 (Parse Incidents):** Authored `StaffIncidentParserTest.kt` and `UnavailabilityIncidentParserTest.kt` in package
  `incidentparsertests` (collaborated with Deniz, who covered the remaining two incident parsers (4 incidents equally
  divded in two)).
* **F13 (Restaurant - Menue):** Authored `CasualDishSelectionTest.kt` and `CountertopIntegrationTest.kt` (along with
  `Decidedishforcasuals.kt`) in package `menuselection` and `CountertopIntegrationTest`respectively (collaborated on
  menu availability logic with Deniz, where he worked on dish selection by regulars and event individuals and i focused
  on in house and delivery casuals).
* **F15 (FOH - Table Merging & Tables):** Authored `ReservationTableMergingTest.kt`and
  `ReservationTableMerging2Test.kt` in package `mergetablestest` (collaborated on table merging and reservation
  validation with Ansh, where he looked after the tble merging logic of the casuals, and i foucsed on the event and
  regular customers).
* **F17 (FOH - Staff Management):** (Atharva tested majority of this feature as we found it very difficult to divide
  this feature in two and helped him diagnose any missing coverage and missing testing logic.)
* **F20 (FOH - Delivery):** Authored `DeliveryIntegrationTest.kt` and `DriverTest.kt` in package `deliveryservicetest`
  (collaborated on waitstaff to driver handoff logic with Skerdi, where he worked mostly on the serving to the drivers
  logic, and i wokred on the driver class and the delivering logic itself).
* **F24 (Customer - Casual Delivery):** Authored `CasualCustomerDelivery.kt` in package `casualcustomerdeliverytests` (I
  covered most of the testing for this feature as just like feature F17 it was very difficult to divide this feature in
  two for testing, but Atharva had contributions in testing logic).
* **F26 (Customer - Food Preferences):** Authored `CasualDishSelectionTest.kt` in package `menuselection` (collaborated
  on customer food preference with Deniz).
* **F29 (Delivery Service):** Authored `DeliveryIntegrationTest.kt` in package `deliveryservicetest` (collaborated on
  with Vlad, where i focused on end-to-end driver transit, route timing, and handover).
* **F31 (Incident - Staff Change):** Authored `StaffChangeIncidentTest.kt` in package `staffchangeincidenttests`
  (collaborated on with Ansh, we both worked on this and F34 feature together but felt that dividng the testing code was
  difficult, so i upladed the incident staff change tests).
* **F32 (Incident - Recipe Change):** Authored `RecipeChangeIntegrationTest.kt` and `Recipechangehappycases.kt` in
  package `recipechangeincidenttset` (collaborated with Skerdi, where i took the happy cases and the integration tests
  and Skerdi also implemented integgration tests and the edge cases).
* **F34 (Incident - Ingredient Unavailability):** Just like F31, this was discussed and tested with Ansh but due to
  difficulties in dividing the code it was uploaded by Ansh completely.
* **P05 (Customer - Rating):** Authored `RegularEventGroupRatingTest.kt` in package `ratingtests` (collaborated on
  regular/event rating scoring with Deniz, while he focused on testing the casual customer ratings).

Authored, registered, and maintained 45 system tests and 8 shared logging/formatting utilities:

* **`generaltests` (22 System Tests):** `CasualDeliveryTimeoutTest`, `CookChangeNoOrderTest`, `CorrectPartialServing1`,
  `DeliveryOrderScenarioTest`, `DeliveryOrderSuccessTestA`, `DeliveryOrderSuccessTestB`, `EventReservationConflictTest`,
  `ExactStockoutTest`, `ExhaustiveSimpleScenarioTest`(had to remove due the errors), `InvalidRestaurantParserTest`,
  `MyParserTest`, `OneCookTwoOrdersTest`, `PartialServiceSuccessTest`, `PartialServiceTimeoutTest`,
  `ProcurementLogicTestA`, `ProcurementLogicTestB`, `RecipeChangeAcrossRestaurantsTest`, `RestaurantClosingTest`,
  `SingleOrCouple`, `WaitstaffExhaustionTest`, `ZeroProcurementTestA`, `ZeroProcurementTestB`.
* **`incidentparsersystemtests` (9 System Tests):** `IncidentCookTypeExecForbiddenRejectedSystemTest`,
  `IncidentDriverWithCookTypeRejectedSystemTest`, `IncidentNegativeEveningRejectedSystemTest`,
  `IncidentStaffWithIngredientRejectedSystemTest`, `IncidentUnavailabilityNegativeDurationRejectedSystemTest`,
  `IncidentUnavailabilityProhibitedPropertyRejectedSystemTest`, `IncidentUnavailabilityZeroDurationRejectedSystemTest`,
  `IncidentWaitstaffWithCookTypeRejectedSystemTest`, `IncidentZeroStaffNumberRejectedSystemTest`.
* **`losttest` (14 System Tests):** `CanvisitA`, `CanvisitB`, `CookIdTieBreakHighestIdB`, `CookIdTieBreakLowestIdA`,
  `CorrectPartialServing2`, `DeliveryBasicDishPriorityA`, `DeliveryBasicDishPriorityB`, `DeliveryBrowsingTickResetA`,
  `DeliveryBrowsingTickResetB`, `IdleCookReuseA`, `IdleCookReuseB`, `KitchenCookAssignmentSystemTest`,
  `RegularFailedTest`, `SomeDeliveryTest.
* **`utils` (8 Test Support Modules):** `DeliveryTestLogs`, `FohArrivalTestLogs`, `FohServiceTestLogs`,
  `InitialAndPrepTestLogs`, `KitchenTestLogs`, `StatisticsTestLogs`, `TestLogFormatter`, `TickStatusTestLogs`.

### Ansh Shekhar Tiwatne

- **F03 with Skerdi: parse ingredients & recipes**
    - unit tests: I covered ingredient and recipe-ingredient interaction, Skerdi covered other recipe behavior
    - integration tests: I covered one larger test for checking if a recipe was parsed with correct ingredients, Skerdi
      covered two integration tests for recipe schema validation
- **F12 with Atharva: cooking**
    - unit tests: I covered parts after cooking start, Atharva covered parts roughly upto it
    - integration tests: I covered process cooking and ingredient planning parts, Atharva did the rest
- **F15 with Ved: table merging & tables**
    - Ved did the part with merging tables for reservations in advance (in the prep phase)
    - I did the part with merging tables for casuals (without reservation) during the serving phase
- **F16 with Vlad: FOH seating**
    - I tested waitstaff selection for seating and checking action loads
    - Vlad tested the outcomes, logs and interaction with other groups
- **F21 with Deniz: FOH escorting**
    - I tested event related escorting
    - Deniz tested casual/regular (assigned waiter) escorting
- **F23 with Deniz: customer - casual**
    - I tested casual experience setting and rating outcomes
    - Deniz tested casual group visit schedule, delivery timing
- **F28 with Deniz: browsing service**
    - I tested event group related logic
    - Deniz tested casual customer related logic
- **F30 with Atharva:**
    - I did the strictly end of evening part (endEvening, resetDrivers, 24-th tick stuff)
    - he did end of opening time (startFohClosing, escortAllAtClosing, endOfOpeningTime, and kitchen side of it)
    - we both also look over each other's tests and work together
- **F31 with Ved: incident - staff change**
    - Ved and I discussed and made tests together, we decided Ved could commit it
- **F34 with Ved: incident - ingredient unavailability**
    - Ved and I discussed and made tests together, we decided I could commit it
- **P02 with Vlad: FOH - event seating**
    - I tested waitstaff selection for seating and checking action loads
    - Vlad tested the outcomes, logs and interaction with other groups
    - It's similar testing responsibility division to F16 that we also worked on together
- **P03 with Vlad: FOH - event other actions**
    - I tested the escorting and serving waitstaff selection
    - Vlad tested the ordering
- **P04 with Deniz: customer - events**
    - I tested booking, arrival, event dish choice
    - Deniz tested ordering and the event favorite dish
