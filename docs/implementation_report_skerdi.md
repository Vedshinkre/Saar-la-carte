# Implementation report - Skerdi Cuka (draft to paste into `implementation_report.md`)

Only major contributions are listed. The table shows the work I actually did; where this differs from the assignment in `feature_assignments.yaml`, it is explained in section 2.

## 1. Individual contributions

### 1.1 Implementation

| Feature | What I implemented | Where the code lies |
|---|---|---|
| F02 Logging | All log messages of the simulation: log levels, output handle, the formatting rules of the specification (ids sorted ascending, `key:value` lists), one logger per phase | package `loggers`: `Logger`, `InitialAndPrepLogger`, `TickStatusLogger`, `FohReceptionLogger`, `FohServiceLogger`, `KitchenLogger`, `DeliveryLogger`, `StatisticsLogger` |
| F13 Restaurant menu | Which dishes can currently be ordered (ingredients in the pantry and not reserved, expired packages ignored, an eligible cook exists) | `restaurant.Countertop` |
| F21 FOH - escorting | Escorting of finished and unserved customers by their waiter (limit of 10 per tick, partial escorting, releasing the waiter's load, taking down the tables) for REGULAR and CASUAL groups | `restaurant.helpers.EscortingProcessor`, `actors.Waiter` |
| F30 End of evening | End of the opening time and of the evening: closing ratings, escorting everyone out, dropping reservations, separating tables, resetting waiters, drivers, customers and the kitchen for the next evening | `restaurant.Restaurant` (end of opening time / evening), `FrontOfHouse` (closing), `Kitchen.resetKitchen` |
| P05 Customer - rating | Rating of a group from its experience and rating likelihood, the restaurant's rating counters, rating logs, ratings at closing | `restaurant.helpers.RatingProcessor`, `customer.CustomerGroup` / `CasualGroup` (`determineRating`) |
| P04 Customer - events | Shared with other members, see section 2. I did not write `EventGroup` | - |

### 1.2 Testing

I am tester of F01, F03, F04, F05, F08, F09, F18, F20, F22, F25, F27, F32, F33 and P01 (as in `feature_assignments.yaml`). My tests are about 300 test methods in about 30 unit and integration test files and 29 system tests.

| Feature | Main unit and integration tests | System tests |
|---|---|---|
| F01 Simulation | `SimulationRunTest` (log framing and log levels through the real `main`), `SimulationOrchestrationTest`, `SimulationIntegrationTest` | `SimulationLifecycleSystemTest`, `StatisticsOrderingSystemTest` |
| F03 / F04 / F05 Parsers | recipe tests of `FoodParserTest` and `FoodParserIntegrationTest`, `FoodParserMissingFieldsTest`, `RestaurantParserBasicDishMenuTest`, `RestaurantParserRejectionTest`, `RestaurantAndCustomerParserIntegrationTest`, `CustomerParserTypeSpecificTest` | - |
| F08 / F09 / P01 Supplier and pantry | `SupplierTest`, `PantryTest`, `StockTest`, `IngredientPackageTest`, `KitchenPlanningTest` | supplier procurement, unavailable ingredients, pantry expiry (6 tests) |
| F18 Ordering | `OrderingTest` | - |
| F20 Delivery | `DeliveryHandOverTest`, `DeliveryEatingTest`, `DeliveryReturnTripTest`, `DeliveryMultiOrderWaiterCapacityTest`, `DeliveryOrderingIntegrationTest` | delivery queue delay, orders split across waiters, delivery at the end of the evening (4 tests) |
| F22 Regulars | `RegularGroupTest` | regular reservations and ratings (6 tests) |
| F25 Restaurant decision | `EventBrowsingDecisionTest`, `EventBrowsingIntegrationTest`, `GroupBehaviourTest` | customer decision, last opening ticks, driver incidents, casual bar tables (5 tests) |
| F27 Waiting for food | `WaitingForFoodTest`, `WaitingForFoodIntegrationTest`, `WaitingForDeliveryTest` | waiting and leaving, waiter load, partly served groups, log order of eating (6 tests) |
| F32 / F33 Incidents | `RecipeChangeIncidentTest`, `RecipeChangeIntegrationTest`, `PackagingChangeIntegrationTest` | - |

## 2. Adjustments from the implementation plan

- **P04 (Customer - events) was shared with other members.** The specification describes events in several places (restaurant decision, reservation, seating, ordering, serving, escorting, rating), and most of these are covered by other features (F25, P02, P03, P05). It was ambiguous what P04 itself should contain, so we decided in the group to share it with members who had less contribution so far. `EventGroup` was written by Atharva, Vlad and Deniz. My part is the rating of EVENT groups (in P05) and the tests of the event restaurant decision (F25).
- **F21: EVENT group escorting moved to Atharva** (Mon 14 Sep) and became part of P03. My F21 covers REGULAR and CASUAL groups.
- **F30:** It touches the kitchen, the front of house and the customers, and was changed afterwards by the specification adjustments 16, 17 and 20. It was planned for one day (Mon 14 Sep); I started it on Tue 15 Sep and worked on it until Wed 23 Sep.
- **Structure changed from the design.** Escorting and rating were planned as methods of `FrontOfHouse`. On Tue 15 Sep I moved them into `EscortingProcessor` and `RatingProcessor`, next to the other processors of the front of house.
- **Schedule.** F13 was done on Fri 11 Sep (planned Sat 12 Sep). F21 got its first version on Mon 14 Sep (planned Sun 13 Sep). P05 was started on Mon 14 Sep together with escorting (planned Wed 16 Sep). F02 was done on Thu 10 Sep as planned and corrected until Thu 17 Sep.
- **Testing started later than planned.** The plan asked for tests from Sat 12 Sep on. My first system tests are from Wed 16 Sep, and most tests of my testing features were written between Thu 17 Sep and Wed 23 Sep. On Mon 21 Sep I replaced tests that only repeated what the JSON schema already enforces with new ones. From Mon 21 Sep the system tests were written to find out why we fail the mandatory tests and were checked against the reference implementation.
- **Tests outside my assigned testing features:** `WaiterEscortTest` (F21, which I implemented), `SimulationStatisticsTest` (F07) and `StaggeredOrderWaitWindowTest` (serving wait window, F19).
- The work continued after the planned end (Mon 21 Sep) until Thu 24 Sep with test corrections, fixes after reference runs and the mutant registration.

## 3. Timeline

### During the week (Mon-Fri)

- **Thu 10 Sep:** started logger classes (F02).
- **Fri 11 Sep:** finished logger; documented the logger, `getAvailableRecipes()` for the menu (F13).
- **Mon 14 Sep:** finished the first escorting version (F21), first version of the rating (P05), moved escorting and rating to the end of the tick processing, handed EVENT group escorting to Atharva, fixed the printing of lists and key-value pairs in the logger.
- **Tue 15 Sep:** start of the end of the evening (F30): closing ratings, cleanup of the front of house, `resetKitchen`; escorting and rating moved into `restaurant.helpers`.
- **Wed 16 Sep:** first version of the end of the opening time and of the evening, drivers removed after the evening, waiter counter reset; first system tests (simulation, supplier, statistics order).
- **Thu 17 Sep:** tick status log adapted to the specification adjustments, kitchen reset fixes; food parser unit and integration tests; supplier, pantry and customer parser tests; pantry expiry and unavailability system tests.
- **Fri 18 Sep:** end of evening fixes, customer reset for a new evening, countertop and escorting fixes, simulation integration test.
- **Mon 21 Sep:** delivery unit and integration tests (hand-over, eating, return), waiting-for-food unit, integration and system tests, packaging change and stock tests, ordering, kitchen planning, group behaviour and simulation run tests, replaced the schema-redundant tests, system tests for the failing mandatory tests.
- **Tue 22 Sep:** system tests for the customers' patience, log order of eating and delivery at the end of the evening; unit tests adjusted after the fix of aborted dishes.
- **Wed 23 Sep:** end of evening adapted to specification adjustment 20 (aborted dishes), system tests for delivery, pantry and browsing, fixes after the reference runs, registration of the confirmed system tests for the mutants.
- **Thu 24 Sep:** added tests for the exception cases of the parsers, review of my tests against the specification.

### Over the weekend

- **Sat 12 Sep:** started the escorting code, first unit test of the simulation.
- **Sun 13 Sep:** fixed typos in the log messages.
- **Sat 19 Sep:** system tests for customer behaviour and late casual groups, restaurant and customer parser integration tests, recipe change edge cases, event restaurant decision tests, registration of the system tests for the reference and the mutants.
- **Sun 20 Sep:** no commits.
