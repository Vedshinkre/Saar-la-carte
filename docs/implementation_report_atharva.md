# Implementation report - Atharva Kore (draft to paste into `implementation_report.md`)

## 1. Features implemented by me

| Feature | Spec definition (Appendix A.2) | What I built | Where the code lies (package under `de.unisaarland.cs.se.selab`) |
|---|---|---|---|
| F06 Parse Incidents | "Parsing and validating incidents" | Parses and validates all four incident types (STAFF, RECIPE, PACKAGING, UNAVAILABLE) and their type-specific properties from spec §17/Fig. 8 (id, type, evening, plus e.g. `restaurant`/`number`/`staffType`/`cookType` for STAFF, `ingredient`/`packagingVolume` for PACKAGING, `ingredient`/`duration` for UNAVAILABLE — including the "no overlapping durations for the same ingredient" constraint) | `parsers` — `IncidentParser`, wired into `ParserController` (also shared work in `ScenarioParser`) |
| F25 Customer - Restaurant Decision | "Deciding on restaurants for causal [sic] groups and casual delivery groups, and event groups" | The restaurant-decision algorithm from the spec's "Browsing and Rating Service" section for EVENT groups: filters to restaurants open at the group's `visitingTick`/`eventEvening` with enough reserved-seat capacity, keeps only restaurants where every customer finds a dish they can eat, then picks the highest (positive − negative) rating difference, tying on lowest id — and, for EVENT groups specifically, reserves immediately once decided | `customer` — `EventGroup`; also `restaurant` — `RestaurantStats` (reporting event-group activity) |
| F28 Browsing Service | "Browsing with static and dynamic restaurant data, estimating available seats" | The shared lookup service every customer type queries: static per-restaurant data (type, opening times, delivery/event capability, recipes and their ingredients) plus the dynamic per-tick estimate of free seats per table type and free delivery drivers, decremented as each group commits so the next group isn't offered a restaurant that's already full | `restaurant` — `BrowsingService` |
| F33 Incident - Packaging Change | "Managing flexible packaging storage, performing incident packagingchange" | Applies the packaging-change incident from spec §17: changes one ingredient's packaging volume to the new positive size given in the incident (`ingredient`, `packagingVolume`) | `incidents` — `PackagingChangeIncident` |
| F34 Incident - Ingredient Unavailability | "Managing availibility [sic] at supplier, performing incident ingredientUnavailability" | Applies the unavailability incident from spec §17: makes one ingredient unpurchasable from the supplier for `duration` evenings starting with the incident's own evening; reads/writes the restaurant's `food.Stock` (held by `restaurant.Pantry`) to enforce that | `incidents` — `UnavailabilityIncident` (reads/writes `food.Stock`, held by `restaurant.Pantry`) |
| P03 FOH - Event Other Actions | "Performing other actions for event groups, like waitstaff selection and action performance" — subtitled in the spec table itself as **"FOH Event - Ordering, Escorting"** | The two ad-hoc, manager-driven waitstaff assignments the spec's Front-of-House section describes for EVENT groups specifically (distinct from REGULAR/CASUAL's single-assigned-waiter model): TAKING ORDERS immediately after an event group is seated, and ESCORTING them out afterward, in both cases the manager picking waiters ad-hoc by load rather than one waiter owning the whole group | `restaurant.helpers` — `EscortingProcessor` (event branches); `customer` — `EventGroup` (order placement); `restaurant` — `FrontOfHouse` (event handling branches) |

Also did most of the early work on the shared parsing infrastructure that every feature sits on:
`parsers.ParserController` (28 of 37 commits touching the file) and `parsers.ScenarioParser` (9 of 10), plus the
`restaurant.RestaurantStats` updates needed once `BrowsingService`/`EventGroup` started reporting event-group activity.

Across all six features combined, that's five packages: `parsers`, `customer`, `restaurant`, `restaurant.helpers`, and
`incidents` (plus reading, not writing, `food`).

*Note on P03's scope:* Event Seating (P02, waitstaff assignment for seating event groups) is a separate feature and
was not implemented by me. Event Serving for event groups isn't named in the spec's P03 or P02
, but in practice it was implemented by me together with Ansh.

### Overlapping features (my code touched code of another member)

Only changes that fixed behaviour are listed, not formatting or merge commits. Feature names are the spec's Appendix A.2
names.

| Feature / owner                       | My change                                                                                      | Day                 |
|---------------------------------------|------------------------------------------------------------------------------------------------|---------------------|
| F09 Pantry - Buying Ingredients (Ved) | adjusted `UnavailabilityIncident`'s `food.Stock` interaction per Ved's review                  | Sat 12 Sep          |
| F30 End of Evening (Skerdi)           | fixed a bug in `Restaurant`'s end-of-evening flow at Skerdi's request                          | Tue 15 Sep          |
| F06 Parse Incidents (mine)            | `IncidentParser` needed a same-day reconciliation between two competing edits, mine and Ansh's | Wed 16 Sep          |
| F17 FOH - Staff Management            | fixed waiter/driver-availability logic while testing, not just writing tests                   | Mon 21 - Tue 22 Sep |

## 2. Adjustments from the Implementation Plan

- **The plan has a gap for me on Day 3 (Mon 14 Sep) and an incomplete entry on Day 4 (Tue 15 Sep)** — the per-day table
  lists no task for me on Day 3, and Day 4 just says "Work on." (`implementation_plan.md`, the Day 3 and Day 4 blocks).
  In practice I used that opening to pull F28, F33, F34 and P03 forward from their scheduled Day 6/Day 7 slots: the
  first Browsing Service and Unavailability Incident work landed on the very first weekend (Sun 13 Sep), and Packaging
  Change / event escorting started Mon 14 Sep — roughly 2-3 days ahead of plan. This lines up with the plan's own note
  that F28/F25 must exist before F23/F24/P04 can be tested, so front-loading them was a deliberate response to that
  dependency, not drift.
- **F17 (FOH - Staff Management, implemented by Deniz) and F24 (Customer - Casual Delivery, implemented by Ansh) were
  planned jointly with Ved, then split by authorship. Ved and I designed the test plan and edge cases for both features
  together, I committed F17's (`fohstaffmanagement/`, section 4) and Ved committed F24's
  (`casualcustomerdeliverytests/`) alone.
  Ved's own report describes the same arrangement from his side ("we balanced test ownership across features ...
  I took primary responsibility for authoring and committing the test suite for F17 ... while Ved took primary
  responsibility for authoring and committing F24, integrating veds scenario input and test logic"). On F17 this
  also went beyond a pure testing role — `feature_assignments.yaml` lists me only as a tester there, but several of my
  commits fixed staff/driver-availability logic directly, not just tests (Ved's report again: "Atharva tested majority
  of this feature ... and helped him diagnose any missing coverage").
- **A handful of cross-feature fixes were informal, not routed through the plan's designated owner:** a
  stock-interaction fix in `UnavailabilityIncident` made at Ved's direction (Sat 12 Sep), and an end-of-evening fix in
  `Restaurant` made at Skerdi's direction (Tue 15 Sep) — both quick unblocks agreed on in passing rather than through a
  scheduled handoff.
- **`IncidentParser.kt`, my sole-owned F06 file, needed a same-day merge of two competing edits with Ansh** (Wed 16 Sep)
  instead of one person owning the file cleanly end-to-end as the plan assumes.
- **One same-day implement-then-revert:** added a calculation meant to estimate when an event group's kitchen prep
  needed to finish by, and removed it a few commits later the same day after realising it computed the wrong tick (Wed
  23 Sep).
- **The two hardening pushes produced disabled/removed tests rather than strictly "fix and add a test" as the plan's Day
  10 calls for.** Several tests were commented out or disabled to keep the build green: "commented out tests which were
  not passing", "disabled incident test", "disabled test in restaurant Parser" (twice), "`CountertopAvailabilityTest.kt`
  is disabled", "detekt disabled few tests to build", "removed appendix tests as not usefull". Most of this was cleaned
  up on "removed commented System Tests" (Thu 24 Sep), but coverage temporarily regressed rather than growing
  monotonically.
- **One of my own tests was deliberately written ahead of the implementation and, for a few days, documented a real
  spec/implementation gap.** Event groups are supposed to have their orders capped at each waiter's per-tick limit,
  same as everyone else, but the implementation was still taking the whole group's order in one go at the time —
  `EventOrderingSpecTest.kt`'s class doc still records this, in its own words: its "Tick load cap" cases "are expected
  to fail today, because `EventGroup.placeOrder` takes the orders of the whole group in one go instead of stopping at
  the waiters' limit." The gap was real when the test was written and was fixed a few days later ("Event Ordering is
  now fixed" Mon 21 Sep, "fixed Event Ordering Spec test" Tue 22 Sep). All 20 cases in the file pass as of this report
  (verified by running the class directly), so the doc-comment itself is now stale and should be trimmed before
  submission.
- **Because of that slip, Day 8 (Mon 21 Sep) wasn't spent the way the plan prescribes.** The plan's Day 8 instruction is
  for testers to "check their assigned implementer's code against edge cases" — i.e. shift onto F02/F04/F07/F08/F10/
  F12/F14/F17/F22/F24/F27/F30/P01. Instead, every one of that day's 8 real commits is still fixing my own F25/P03
  event-ordering and available-driver bugs, because the Day 7 "all features implemented" milestone hadn't actually
  landed for that part of my own work yet. I only moved onto assigned-feature testing (F17, F07, F14, F22) starting Tue
  22 Sep.

## 3. Testing responsibility (from `feature_assignments.yaml`)

I test (spec Appendix A.2 names in parentheses): F02 Logging (Skerdi), F04 Parse Restaurants (Ansh), F07 Statistics
(Ansh), F08 Supplier (Ved), F10 Cooking - Order Queue (Ved), F12 Cooking - Cooking (Ved), F14 FOH - Evening Table
Reservation (Deniz), F17 FOH - Staff Management (Deniz), F22 Customer - Regulars (Deniz), F24 Customer - Casual Delivery
(Ansh), F27 Customer - Waiting for Food (Ansh), F30 End of Evening (Skerdi), P01 Pantry - Best Before, Reservations
(Ved).

Three of these ended up lighter-touch than the assignment implies, for reasons worth recording:

- **F14 FOH - Evening Table Reservation:** by the time I got to it, most of the unit-level coverage already existed —
  `reservationtests/` has no commits from me at all; Vlad (Wed 17 Sep, "tests for reservation") and Ansh (Thu 18 Sep)
  had already written the unit tests. So I only added system-level coverage in `generaltests/`:
  `TableReservationSingleTableSystemTest`, `TableReservationMergeSystemTest`, `TableReservationConflictsSystemTest`
  (spec 2.2 table-reservation steps 2/3/5 and the merge/conflict cases) and `TableMergingLifecycleSystemTest` (the
  merge-step lifecycle, steps 4-6). Per the mutation-test run described in section 6 (38/40 mutants found),
  `TableMergingLifecycleSystemTest` is confirmed to have killed 3 mutants by itself — Denkmalschutz, Grindset and
  KingOfTheHill — the strongest single-test result of anything I wrote; the other three are registered mutant probes
  too but aren't credited as the catcher for any mutant in that run.
- **F22 Customer - Regulars:** I deliberately tested this last. Browsing Service (F28) and event ordering/escorting
  (P03) had real, live bugs at the time (see section 2, and the Browsing Service/`EventGroup.placeOrder` issues) that
  felt higher-risk to leave uncovered than a feature with two other assigned testers. My own F22 unit tests didn't land
  until Tue 22 Sep, by which point Skerdi had already added coverage in `planningtests/` a day earlier (Mon 21 Sep,
  "replaced schema redundant tests with new ones, and system tests for finding failing component tests"), and Deniz (the
  implementer) added more afterward (Thu 24 Sep) — the overlap with Skerdi's testing responsibility meant
  deprioritizing it didn't actually leave a coverage gap.
- **F24 Customer - Casual Delivery:** per the F17/F24 pairing with Ved described in section 2, we planned this
  feature's tests and edge cases together, but Ved committed and owns the actual suite — `casualcustomerdeliverytests/`
  has zero commits from me, all four are his (Wed 17 Sep, Sun 20 Sep, Tue 23 Sep) — the mirror image of my sole
  authorship of F17's suite.

## 4. Unit and integration tests (`src/test/kotlin`)

Each row was checked against the file's own doc-comment, not just its directory, which corrected two attributions from
an earlier draft (marked below) and dropped one file that doesn't belong here at all.

| Package                             | Class                                                                                                                                                                                        | Feature tested                                                                                                                                                                                                                                                                            |
|-------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `loggertests`                       | `InitialAndPrepLoggerTest`, `LoggerCoreTest`, `StatisticsLoggerTest`, `TickStatusLoggerTest` (co-authored)                                                                                   | F02 Logging                                                                                                                                                                                                                                                                               |
| `restaurantparsertests`             | `RestaurantParserTest`, `RestaurantParserDuplicateAndExecTest`, `RestaurantParserTestDiffrentFoodConfigs`                                                                                    | F04 Parse Restaurants                                                                                                                                                                                                                                                                     |
| `parsertests`                       | `ParserControllerLogTest`                                                                                                                                                                    | F02 Logging, via `ParserController`'s parse-phase log lines (corrected — not F01/F03; see note below)                                                                                                                                                                                     |
| `suppliertests`                     | `SupplierTest`                                                                                                                                                                               | F08 Supplier (`Supplier.procure` boundary/off-by-one cases)                                                                                                                                                                                                                               |
| `pantrytests`                       | `PantryTest` (co-authored)                                                                                                                                                                   | P01 Pantry - Best Before, Reservations                                                                                                                                                                                                                                                    |
| `cookingtests`                      | `OrderQueueLifecycleTest`                                                                                                                                                                    | F10 Cooking - Order Queue (its own doc-comment says "Tests for F10")                                                                                                                                                                                                                      |
| `cookingtests`                      | `CookUnitTest`, `KitchenCookingProcessTest` (both co-authored, mostly with Ansh)                                                                                                             | F12 Cooking - Cooking (both files' own doc-comments say "(F12)") — same folder as the F10 row above, different feature; a fourth file in that folder, `OrderQueueTest.kt`, is Deniz's alone (Thu 24 Sep) and isn't claimed here                                                           |
| `planningtests`                     | `RegularCustomersKitchenPlanningTest`                                                                                                                                                        | F22 Customer - Regulars, kitchen-planning pooling for `Restaurant.prepareForEvening` (corrected — its doc-comment says F22 only, not F12)                                                                                                                                                 |
| `fohstaffmanagement`                | `FohStaffFixtures`, `WaiterAssignmentTieBreakTest`, `WaiterEnsureIdTest`, `WaiterFirstActionTriggerTest`, `WaiterIdEventGroupTest`, `WaiterIdLifecycleTest`, `WaiterWorkloadIntegrationTest` | F17 FOH - Staff Management                                                                                                                                                                                                                                                                |
| `eveningclosetests`                 | `FrontOfHouseClosingTest`, `KitchenResetTest`, `ClosingCompositionTest` and `EveningCloseFixtures` (co-authored)                                                                             | F30 End of Evening                                                                                                                                                                                                                                                                        |
| `eventorderingtests`                | `EventOrderingSpecTest`                                                                                                                                                                      | F25/P03 event ordering (own feature; spec-derived, see section 2)                                                                                                                                                                                                                         |
| `dishdecisiontests`                 | `DishDecisionFixtures`, `DishDecisionPermutationTest`, `DishDecisionRuleTest`                                                                                                                | F26 Customer - Food Preferences, the dish-decision rule in `FoodPreference.decideDish` (corrected — not F12/F13; this is Vlad's feature, tested because my own `EventGroup`/P03 ordering code depends on its event-priority branch, not because it's a formal testing assignment of mine) |
| `waitingforfoodtests`               | `WaitingForEventFoodTest`, `WaitingFixtures` (co-authored)                                                                                                                                   | F27 Customer - Waiting for Food (its own doc-comment: "F27 unit tests for `EatingProcessor`, covering EVENT groups")                                                                                                                                                                      |
| `simulationstatisticsdeliverytests` | `StatisticsCookedAndDeliveredRealRestaurantTest`                                                                                                                                             | F07 Statistics (its own doc-comment says "F07")                                                                                                                                                                                                                                           |

## 5. System tests I wrote (`src/systemtest/kotlin/.../selab26`)

| Package                  | Classes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `eventtests`             | `EventArrivalSeatingSystemTest`, `EventEscortingSystemTest`, `EventOrderTakersSystemTest`, `EventOrderingSystemTest`, `EventServingCapacityWaitSystemTest`, `EventServingWaiterPrioritySystemTest`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `kitchenschedulingtests` | `BasicDishPrecedenceOverLowerIdSystemTest`, `BatchDoesNotAbsorbLaterArrivingOrderSystemTest`, `LowestRankingCookAssignedAcrossHierarchySystemTest`, `NonBasicDishesTieBreakByAscendingIdSystemTest`, `TieBreakSameCookTypeByLowestExistingIdSystemTest`, `UncookableDishIsSkippedForCookableOneSystemTest`                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `parserlogtests`         | `ParserControllerLogSystemTest`, `UnavailabilityDifferentIngredientsOverlapAcceptedSystemTest`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `generaltests`           | `CasualMergeTrimTieBreakSystemTest`, `CasualNoDecisionNoSpaceTest`, `CasualTableExclusionSystemTest`, `CasualTableNoLiftSystemTest`, `EatingFinishedAndEscortedSameTickTest`, `EventSeatingCapacityBoundaryTests`, `EventSeatingLoadPriorityTests`, `EventSeatingSharedCapacityTests`, `EventSeatingSpecExampleTests`, `EventTableMergingSystemTest`, `KitchenAndBrowsingEdgeCasesSystemTest`, `RecipeChangeRoundingSystemTest`, `StaffIdResetAcrossEveningsSystemTest`, `StaffLoadBalancingFallbackSystemTest`, `StaffLoadConcentrationSystemTest`, `StaffMultiWaiterExhaustionSystemTest`, `TableMergingLifecycleSystemTest`, `TableReservationConflictsSystemTest`, `TableReservationMergeSystemTest`, `TableReservationSingleTableSystemTest`, `TableTypeRestrictionsSystemTest` |

## 6. My system tests registered against the released mutants

86 classes across my two mutant-registration functions in `SystemTestRegistration.kt` (30 validation, 56 simulation) —
all but one (`CasualTableTypesNeverMixSystemTest`, not mine) confirmed by git blame.

**Confirmed kills** (an external mutation-testing run, 38/40 mutants found) — 12 mutants across 10 of my tests:

| Test                                                   | Mutant(s)                              |
|--------------------------------------------------------|----------------------------------------|
| `TableMergingLifecycleSystemTest`                      | Denkmalschutz, Grindset, KingOfTheHill |
| `FoodDuplicateIngredientRejectedSystemTest`            | Discounter                             |
| `OverlappingUnavailabilityRejectedSystemTest`          | FoodScarcity                           |
| `EventEscortingRatingSystemTest`                       | Backlash                               |
| `RecipeChangeRoundingSystemTest`                       | ChickenAndRice                         |
| `EventArrivalCasualTurnedAwaySystemTest`               | DinnerForOne                           |
| `EventOrderingFavoriteDishBeforePreferencesSystemTest` | Extrawurst                             |
| `EventServingWaitEventNotServedSystemTest`             | Kachow                                 |
| `EventEscortingLowestLoadFirstSystemTest`              | ShortStaffed                           |
| `StaffLoadConcentrationSystemTest`                     | Whatever                               |

