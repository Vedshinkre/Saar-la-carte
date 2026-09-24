# Implementation Report: Skerdi Cuka

Day 1 is 10 September 2026, the day the first implementation commits were pushed.

## Individual Contributions

The feature assignment in `feature_assignments.yaml` is unchanged. What changed in practice is described under "Adjustments".

### Implementation

| Feature | Contribution | Main code |
|---|---|---|
| F02 Logging | All log output of the simulation: log levels, output handle, the formatting rules of the specification (ids sorted ascending and comma separated, `key:value` lists), one logger object per phase. | `loggers/Logger`, `InitialAndPrepLogger`, `TickStatusLogger`, `FohReceptionLogger`, `FohServiceLogger`, `KitchenLogger`, `DeliveryLogger`, `StatisticsLogger` (about 50 log functions) |
| F13 Restaurant menu | Which dishes can currently be ordered: a recipe is available if its ingredients are in the pantry and not reserved (expired packages ignored) and an eligible cook exists. | `restaurant/Countertop` |
| F21 Escorting | Escorting of finished groups by their waiter with the limit of 10 customers per tick (partial escorting), releasing the waiter's load, dismantling merged tables of CASUAL groups, escorting everyone at closing. | `restaurant/helpers/EscortingProcessor`, `Waiter.escort` |
| F30 End of evening | End of the opening time and of the evening: removing the remaining customers, dropping reservations, separating tables, resetting waiters, drivers and customers, resetting the kitchen (counters, ids, unavailability durations) for the next evening. | `Restaurant.endOfOpeningTime` / `endEvening`, `FrontOfHouse.startFohClosing` / `endFohOpeningTime`, `Kitchen.resetKitchen`, `CustomerGroup.resetForNewEvening` |
| P05 Rating | The rating of a group from its experience and rating likelihood, the restaurant's positive/negative counters, the rating logs, and the rating of groups at closing. | `restaurant/helpers/RatingProcessor`, `CustomerGroup.determineRating`, `CasualGroup.determineRating`, `enums/RatingType` |
| P04 Customer events | Shared with other members, see "Adjustments". My part: EVENT groups are removed from the front of house after their rating, and they are rated by the same processor. | `FrontOfHouse`, `RatingProcessor` |

### Testing (assigned testing features)

I am tester of F01, F03, F04, F05, F08, F09, F18, F20, F22, F25, F27, F32, F33 and P01. My tests are about 300 test methods in about 30 unit and integration test files, and 29 system tests.

| Feature | Tests written |
|---|---|
| F01 | `SimulationRunTest` (log framing and log levels through the real `main`), `SimulationIntegrationTest`, `SimulationOrchestrationTest`; system tests `SimulationLifecycleSystemTest`, `StatisticsOrderingSystemTest` |
| F03 / F04 / F05 | Recipe part of `FoodParserTest` and `FoodParserIntegrationTest` (Ansh wrote the ingredient part), `RestaurantParserBasicDishMenuTest`, `RestaurantAndCustomerParserIntegrationTest`, `CustomerParserTypeSpecificTest` |
| F08 / F09 / P01 | `SupplierTest`, `PantryTest`, `StockTest`, `IngredientPackageTest`, `KitchenPlanningTest`; system tests for supplier procurement, unavailability and pantry expiry |
| F18 | `OrderingTest` (order of customers, dishes running out, customers that find no dish, waiter at the order limit, status logs, order ids) |
| F20 | `DeliveryHandOverTest`, `DeliveryEatingTest`, `DeliveryReturnTripTest`, `DeliveryMultiOrderWaiterCapacityTest`, `DeliveryOrderingIntegrationTest`; system tests for the delivery queue, orders split across waiters, and delivery at the end of the evening |
| F22 | `RegularGroupTest` and the system tests for regular groups (failed attempts, consecutive failures, ascending id, ratings) |
| F25 | `EventBrowsingDecisionTest`, `EventBrowsingIntegrationTest`; system tests for the restaurant decision, the last three opening ticks and driver incidents |
| F27 | `WaitingForFoodTest`, `WaitingForFoodIntegrationTest`, `WaitingForDeliveryTest`; system tests for waiting, leaving, waiter load and escorting of partly served groups, and the log order of eating |
| F32 / F33 | `RecipeChangeIncidentTest`, `Recipechangehappycases`, `RecipeChangeIntegrationTest`, `PackagingChangeIntegrationTest` and part of `PackagingChangeIncidentTest` |

Bugs found by these tests in other members' code and fixed or reported: unit letter mismatch in `Stock`, unavailability durations never applied and later applied twice in `Kitchen`, expired packages counted as available in `Countertop`, a delivery driver that never became free after a given-up delivery in `DeliveryProcessor`, and wrong wait thresholds and abandoned-dish handling in `EatingProcessor` (reported to Ansh).

---

## Adjustments from the Implementation Plan

**Schedule.**

| Feature | Plan | What happened |
|---|---|---|
| F02 Logging | Day 1-2 | Done on Day 1. Corrections continued until Day 8, among them the tick-start log of spec adjustment 22. |
| F13 Menu | Day 3 | Done on Day 2. Fixed on Day 9 (expired packages counted as available; a dish shown as available while its only cook was busy). |
| F21 Escorting | Day 4 | First version on Day 5, moved into a helper class on Day 6, fixed again on Day 9. |
| F30 End of evening | Day 5 | Started on Day 6, first version on Day 7, then fixed repeatedly until Day 14. It took about three times the planned time. |
| P04 Events | Day 6 | Shared with other members, see below. |
| P05 Rating | Day 7 | Started on Day 5 together with escorting, finished on Day 7. |

**F30 was bigger than planned.** The plan treated it as one feature. It touches the kitchen (reset of counters, cook and order ids, unavailability durations), the front of house (customers, tables, waiters, drivers) and the customers (reset for the next evening). Most of my bug-fix work from Day 8 to Day 14 is in this area. Three specification adjustments changed it after the first version: adjustment 16 (customers are escorted without a waiter action at closing), 17 (logging after the closing tick) and 20 (aborted meals are no longer returned to the pantry and the kitchen is not told).

**Structure changed from the design.** The design placed escorting and rating as methods of `FrontOfHouse`. On Day 6 I moved them into `restaurant/helpers` as `EscortingProcessor` and `RatingProcessor`, next to the other processors of the front of house. `FrontOfHouse` only delegates.

**P04 (Customer - events) was shared with other members.** The specification describes events across several places: the restaurant decision, the reservation, seating, ordering, serving, escorting and the rating. Most of these are covered by other features (F25, P02, P03, P05), so it was ambiguous what P04 itself should contain. We therefore decided in the group to share the feature with members who had less contribution so far. `EventGroup` was written by Atharva, Vlad and Deniz. My part is the removal of EVENT groups from the front of house after their rating, their rating in P05, and the tests of the event restaurant decision (F25), which check adjustment 13 (tables are reserved only on the event evening). The escorting of EVENT groups (part of F21) went to Atharva on Day 5 and became part of P03.

**Testing started later than planned and was larger.** The plan asked for tests for finished features from Day 3 on. I wrote a first unit test of the simulation on Day 3 and the first system tests on Day 7; most unit and integration tests of my testing features were written on Days 8, 12 and 13. On Day 12 I replaced 33 tests that only repeated what the JSON schema already enforces with about 86 new tests. From Day 13 the system tests were written to find out why we fail the mandatory tests and were checked against the reference implementation; tests that failed there were corrected or deleted.

The work continued after the planned Day 12: Days 13 to 15 are test corrections, fixes after reference runs, and the mutant registration.

---

## Detailed Timeline

### Skerdi

- **Day 1 (10 Sept):** Wrote all logger objects (`Logger`, `InitialAndPrepLogger`, `KitchenLogger`, `DeliveryLogger`, `FohReceptionLogger`, `FohServiceLogger`, `StatisticsLogger`, `TickStatusLogger`).
- **Day 2 (11 Sept):** Documented the loggers. Wrote `Countertop.getAvailableRecipes` (F13).
- **Day 3 (12 Sept):** First unit tests of the simulation (filtering of customer groups). Started the escorting code.
- **Day 4 (13 Sept):** Fixed typos in the log messages.
- **Day 5 (14 Sept):** Finished the first escorting version. Fixed the printing of lists and key-value pairs in the logs. Started the rating (`RatingType`, `determineRating`). Handed EVENT escorting to Atharva.
- **Day 6 (15 Sept):** Moved escorting and rating into `EscortingProcessor` and `RatingProcessor`. Started the end of the evening and the kitchen reset. Added the ratings at closing.
- **Day 7 (16 Sept):** First version of the end of the opening time and of the evening; drivers are removed, waiter counters reset. First system tests: simulation lifecycle, supplier procurement, statistics ordering, unavailable ingredients.
- **Day 8 (17 Sept):** Fixed the kitchen reset (ids, unavailability durations) and the bug that EVENT groups were never removed after their rating. Adapted the tick-start log to spec adjustment 22. Fixed the unit letter in `Stock`. Wrote the recipe tests of the food parser, the first customer parser tests, `PantryTest`, `SupplierTest` and the pantry expiry system tests.
- **Day 9 (18 Sept):** Fixed `Countertop`, the customer reset for a new evening, escorting and end-of-evening bugs. Added `SimulationIntegrationTest`.
- **Day 10 (19 Sept):** Fixed unavailability durations applied once per restaurant instead of once per evening. Wrote the customer behaviour and late-casual system tests, the restaurant/customer parser integration test, the recipe-change edge cases and the event restaurant decision tests. Registered the tests for the reference and the mutants.
- **Day 11 (20 Sept):** No commits.
- **Day 12 (21 Sept):** Delivery tests (eating, hand-over, return trip, ordering), waiting-for-food system, unit and integration tests, packaging change tests, casual bar system tests, `OrderingTest`, `KitchenPlanningTest`, `StockTest`, `GroupBehaviourTest`, `SimulationRunTest`. Replaced the schema-redundant tests and removed repeated ones.
- **Day 13 (22 Sept):** Corrected wrong system tests and wrote probes for the failing mandatory tests (patience, partial leavers, waiter load). Found and reported a bug in our implementation. Delivery system tests, tests for the log order of eating, and tests for delivery at the end of the evening after the office-hours answer. Updated tests after Ansh's fix of aborted dishes.
- **Day 14 (23 Sept):** Removed the dependencies on the aborted dish status after adjustment 20. Fixed system tests that failed on the reference implementation and added browsing and pantry system tests. Fixed the driver that was never freed after a given-up delivery and re-enabled the outdated waiting-for-food tests with the confirmed patience values. Registered the confirmed system tests for the mutants.
- **Day 15 (24 Sept):** Review of my unit, integration and system tests against the specification and its adjustments; this report.

---

## Usage of Generative AI

**Skerdi:**
*Option 2:*
Skerdi used the following tools in the implementation phase (as announced in the implementation plan: GitHub Copilot Chat, ChatGPT (GPT-5), Claude (Sonnet 5) and Gemini (3.1 Pro)):

- **Claude Code (Claude Sonnet 5)**, on Days 14 and 15: to review my tests against the specification and its adjustments, to correct outdated tests, to fix the driver bug in `DeliveryProcessor`, to organize the system test registration, and to draft this report from the git history. I reviewed and ran all generated changes before committing.
We are aware of the potential dangers of using these tools and take full responsibility for any code, documents and other content produced during the group phase.
