# Implementation report - Skerdi Cuka (draft to paste into `implementation_report.md`)

Everything below is taken from `feature_assignments.yaml` and `git log` / `git blame`. Days are the commit dates.

## 1. Features implemented by me

| Feature | What | Where the code lies |
|---|---|---|
| F02 Logging | all log messages of the simulation | package `de.unisaarland.cs.se.selab.loggers`: `Logger`, `TickStatusLogger`, `InitialAndPrepLogger`, `FohReceptionLogger`, `FohServiceLogger`, `KitchenLogger`, `DeliveryLogger`, `StatisticsLogger` |
| F13 Restaurant menu | recipes a restaurant can currently cook (`getAvailableRecipes`) | `restaurant.Countertop` |
| F21 FOH - escorting | escorting finished or unserved customers out | `restaurant.helpers.EscortingProcessor` (created as `EscortingCoordinator`), escort actions in `actors.Waiter` |
| F30 End of evening | closing ratings, escorting everyone out, kitchen/FOH/driver reset | `restaurant.Restaurant` (end of opening time / end of evening), `Kitchen.resetKitchen`, `FrontOfHouse` reset |
| P04 Customer - events | event group escorting and rating at the end of the evening, removal of event groups after rating | `restaurant.helpers.EscortingProcessor`, `restaurant.helpers.RatingProcessor`, event handling in `restaurant.FrontOfHouse` (`customer.EventGroup` itself was mostly written by Atharva) |
| P05 Customer - rating | rating decision and rating log | `restaurant.helpers.RatingProcessor` (created as `RatingCoordinator`), `customer.CustomerGroup.experience` |

### Overlapping features (my code touched code of another member)

Only changes that fixed behaviour are listed, not formatting or merge commits.

| Feature / owner | My change | Commit / day |
|---|---|---|
| Kitchen reset, F10-F12 (Ved) | reset kitchen at the end of the evening; call `applyUnavailableDurations` in the reset; clear `orderedAtByOrderId`; runtime error during reset | `122bfec` Tue 15 Sep, `ee33929` Thu 17 Sep, `eb69350` Thu 17 Sep, `65f4c6b` Fri 18 Sep |
| Pantry, F09 (Ved) | expired packages were still counted | `1122229` Fri 18 Sep |
| Incidents, F32/F33 (Atharva) | unavailability was applied per restaurant instead of once per evening | `b550491` Sat 19 Sep |
| Drivers / delivery, F20 (Ansh) | drivers are removed at the end of the evening; driver id counter is reset | `94fce0e` Wed 16 Sep, `6b5ea26` Mon 21 Sep, `2245919` Tue 22 Sep |
| Simulation, F01 (Vlad) | reset of the order ids and customers between evenings | `9d04a1f` Thu 17 Sep, `3d4171b` Fri 18 Sep |
| Customer events (Atharva) | condition for event group escorting, handed over to Atharva | `741e2bc` Mon 14 Sep |

## 2. Testing responsibility (from `feature_assignments.yaml`) and who else tests my features

I test: F01 (impl. Vlad), F03 (Ved), F04 (Ansh), F05 (Deniz), F08 (Ved), F09 (Ved), F18 (Vlad), F20 (Ansh), F22 (Deniz), F25 (Vlad), F27 (Ansh), F32 (Atharva), F33 (Atharva), P01 (Ved).

My own features F02, F13, F21, F30, P04, P05 are tested by the members named as `testing_responsible` for them in `feature_assignments.yaml`.

## 3. Unit and integration tests I wrote (`src/test/kotlin`)

| Package | Class | Feature tested |
|---|---|---|
| `parsertests` | `FoodParserTest`, `FoodParserIntegrationTest` | F03 parse ingredients and recipes |
| `restaurantparsertests` | `RestaurantAndCustomerParserIntegrationTest`, `RestaurantParserBasicDishMenuTest` | F04 / F05 parsers together, basic dish menu |
| `customerparsertests` | `CustomerParserTypeSpecificTest` | F05 parse customers |
| `suppliertests` | `SupplierTest` | F08 supplier |
| `pantrytests` | `PantryTest` | F09 pantry |
| `stocktests` | `StockTest`, `IngredientPackageTest` | F08 / F09 stock and packages |
| `packagingchangeincidenttests` | `PackagingChangeIncidentTest`, `PackagingChangeIntegrationTest` | F32 packaging change |
| `recipechangeincidenttest` | `RecipeChangeIncidentTest` | F33 recipe change |
| `simulationtests` | `SimulationRunTest`, `SimulationOrchestrationTest`, `SimulationIntegrationTest`, `SimulationStatisticsTest` | F01 simulation managing, F07 statistics order |
| `planningtests` | `KitchenPlanningTest`, `RegularGroupTest` | F22 regular groups, planning |
| `orderingtests` | `OrderingTest` | F18 ordering |
| `customertests` | `GroupBehaviourTest` | F25 customer behaviour |
| `eventbrowsingtests` | `EventBrowsingDecisionTest`, `EventBrowsingIntegrationTest` | F25 event restaurant decision |
| `deliveryhandoverandreturntests` | `DeliveryHandOverTest`, `DeliveryEatingTest`, `DeliveryReturnTripTest`, `DeliveryMultiOrderWaiterCapacityTest`, `DeliveryOrderingIntegrationTest`, `DeliveryFixtures` | F20 delivery hand-over, eating, return |
| `deliveryservicetest` | `DriverPoolSelectionTest` | F20 driver pool |
| `waitingforfoodtests` | `WaitingForFoodTest`, `WaitingForFoodIntegrationTest`, `WaitingForDeliveryTest`, `WaitingFixtures` | F27 waiting for food |
| `servingoutcometests` | `StaggeredOrderWaitWindowTest` | F27 waiting window |
| `escortingtests` | `WaiterEscortTest` | F21 escorting |

No test of mine carries `@Disabled` any more. Changes made for this:
- `FoodParserTest`: the disabled ingredient test passes as it is; the test for a recipe ingredient whose `unit` differs from the ingredient was removed, because it needs a schema change (`recipe.schema` accepts unknown keys) and nobody adds more code.
- `FoodParserIntegrationTest`: the restaurant fixture path pointed to a deleted file, now `src/systemtest/resources/RestaurantParserTests/Restaurants.json`; the food fixture had an ingredient amount of 100 g for a package of 20 g and is now 500 g.
- `WaitingForFoodTest`, `WaitingForFoodIntegrationTest`: the open question "exactly 4 ticks after ordering positive or neutral" is answered (neutral, "before the 4 tick expectation window is over"). The test now expects NEUTRAL at 4 ticks and POSITIVE at 3 ticks.

## 4. System tests I wrote (`src/systemtest/kotlin/.../selab26`)

| Package | Classes |
|---|---|
| `generaltests` | `SimulationLifecycleSystemTest`, `SupplierProcurementSystemTest`, `UnavailableIncidentSupplierProcurementSystemTest`, `UnavailabilityDurationExpirySystemTest`, `StatisticsOrderingSystemTest`, `PantryExpirySystemTest`, `PantryExpiryAlphabeticalOrderSystemTest`, `PantryAbandonedDishIngredientsNotRefundedSystemTest`, `CustomerBehaviourDecisionSystemTest`, `LateCasualNoWaiterSystemTest`, `WaitingForFoodSystemTest`, `WaiterLoadReleasedOnLeavingSystemTest`, `PartialLeaverEscortedSystemTest`, `RegularCapacityBlockedOrderNotLostSystemTest`, `BrowsingRefusesDineInInLastThreeTicksSystemTest`, `DriverIncidentReducesBrowsingAvailabilitySystemTest`, `CasualBarRestaurantChoiceSystemTest`, `CasualBarExactFitNoMergeSystemTest`, `CasualTableTypesNeverMixSystemTest`, `DeliveryQueueDelayFailedReturnSystemTest`, `DeliveryQueueDelayTimeoutSystemTest`, `DeliveryEveningBoundaryNeverRatesSystemTest`, `DeliveryMultipleWaitersServeOneOrderSystemTest`, `DeliveryDriverFlowSystemTests` (six delivery driver tests) |
| `reservationdecisiontests` | `FailedReservationRatesInFirstTickOfEveningSystemTest`, `FailedReservationRatesInTheCorrectEveningSystemTest`, `RegularFailedAttemptsNotResetByInterveningSuccessSystemTest`, `RegularPartiallyServedGroupKeepsVisitingSystemTest`, `RegularReservationOrderIsAscendingIdNotFileOrderSystemTest`, `RegularUneventfulVisitRatesPositiveSystemTest` |
| `abtests` | `EatingOrderIsPerGroupSystemTest`, `EatingOrderIsTwoPassesSystemTest` (probes against the reference) |

## 5. Timeline

### During the week (Mon-Fri)

- **Thu 10 Sep:** logger classes in `dev` (F02).
- **Fri 11 Sep:** `Id` type alias, `getAvailableRecipes()` for the menu (F13), documented the logger.
- **Mon 14 Sep:** finished escorting (F21), first version of the rating service (P05), moved escorting and rating to the end of the tick, event group escorting, fixed the printing of lists and key-value pairs in the logger, detekt fixes.
- **Tue 15 Sep:** start of end of evening (F30): closing ratings, FOH customer cleanup, `resetKitchen`; escorting and rating coordinators moved into `restaurant.helpers`; fixed attributions (`@author` lines) and detekt issues.
- **Wed 16 Sep:** rough finish of end of opening time / evening, drivers removed after the evening, waiter counter reset, logger fixes; first system tests (simulation, supplier, statistics order).
- **Thu 17 Sep:** tick status logger fixes from the spec adjustments, event groups removed after rating, reset of ids and kitchen fixes; food parser unit and integration tests; supplier, pantry and customer parser tests.
- **Fri 18 Sep:** end of evening fixes, customer reset, countertop changes, escorting fixes, expired packages counted in the pantry fixed, simulation test.
- **Mon 21 Sep:** delivery hand-over / eating / return unit and integration tests, waiting-for-food unit, integration and system tests, packaging change integration tests, stock tests, driver id counter reset, removed redundant tests, system tests for the failing component tests.
- **Tue 22 Sep:** corner case system tests for rating and delayed delivery, more food parser coverage, system tests for the customers' patience.
- **Wed 23 Sep:** fixes of the abortion and delivery give-up handling (`deliveryGivenUp`), unit tests adjusted; system tests for delivery, pantry, browsing, reservations and table types.
- **Thu 24 Sep:** removed `@Disabled` from my unit tests (see section 3), `@param` tags for all public logger functions, delivery driver system tests.

### Over the weekend (recorded separately, not part of the week)

- **Sat 12 Sep:** working on escorting, unit test for the simulation, reflection for a private method test, test fixes.
- **Sun 13 Sep:** logger message typos, logger test fix.
- **Sat 19 Sep:** system tests for customer behaviour and late casual groups, restaurant and customer parser integration tests, recipe change incident edge cases, restaurant decision unit and integration tests, basic dish test, unavailability applied once per evening, registration of the system tests for the reference and the mutants.
- **Sun 20 Sep:** no commits.

## 6. Documentation

All public functions of the `loggers` package (my F02 code) have KDoc with `@param` tags. The other public functions of my features already had KDoc; there are no `//` comments in place of KDoc on public functions of mine.
