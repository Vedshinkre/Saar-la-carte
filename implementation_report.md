# Implementation Report

## Individual Contributions

### Vlad Mihai Marciu

### 1.1 Implementation

| Package | Classes / functions | Feature |
|---|---|---|
| *(root)* | `Main` (`main`, `parseCommandLineArgs`, `setupLogging`), `Cliinfo`, `Time`, `Constants`, `Types` | F01 |
| `system` | `Simulation` (the evening / preparation / serving), `SimulationConfig` | F01 |
| `restaurant` | `Restaurant`: `simulateTick`, `simulateOpeningHoursTick`, (the per-tick flow of a restaurant) | F01 |
| `food` | `Order` | F18 |
| `restaurant` | `Countertop`: `reserveIngredients`, `addOrder` | F18 |
| `customer` | `CustomerGroup`: `placeOrder`, `registerDish`, `orderingSequence`, `WaiterRota`, `startNewVisit`, `getCustomersWhoLeft` | F18 |
| `restaurant.helpers` | `ArrivalProcessor`: `orderSuccess`, `logPlacedOrder`, the ordering part of `processArrival` and of the ordering status | F18 |
| `customer` | `CasualGroup`: `isVisitingThisTick`, `isVisitingTonight`, `getDeliveryOrderTick`; `CustomerGroup.isVisitingThisTick` | F23 |
| `customer` | `FoodPreference`: `decideDish`, `firstMatchingFavorite`, `mostPreferredIngredients` | F26 |
| `incidents` | `StaffChangeIncident`; `actors.RestaurantStaff` | F31 |
| `incidents` | `RecipeChangeIncident` | F32 |

### 1.2 Testing

| Feature (implementer) | Type | Package | Test classes |
|---|---|---|---|
| F05 (Deniz) | unit, integration | `customerparsertests` | `CustomerParserSharedFieldsTest`, `FoodPreferenceParserTest`, `CustomerParserIntegrationTest` (JSON fixtures in `customerparsertests/fixtures`) |
| F05 (Deniz) | system | `customerparsertests` | `CustomerParserSystemTests` |
| F07 (Ansh) | unit, integration | `simulationstatisticsdeliverytests` | `StatisticsDeliveredAndRatedTest`, `StatisticsDeliveredAndRatedIntegrationTest`, `StatisticsFixtures` |
| F11 (Ved) | unit, integration | `kitchentests` | `KitchenSelectionUnitTest`, `KitchenAssignmentIntegrationTest` |
| F14 (Deniz) | unit | `reservationtests` | `ReservationOutcomeTest`, `ReservationCancellationTest` |
| F16 (Deniz) | unit, integration | `seatingtests` | `SeatingOutcomeTest`, `SeatingIntegrationTest` |
| F16 (Deniz) | system | `generaltests` | `CasualAdHocTableMergingSystemTest`, `RegularRetrySucceedsSystemTest` |
| F19 (Ansh) | unit, integration | `servingoutcometests` | `ServingOutcomeTest`, `ServingIntegrationTest` |
| F19 (Ansh) | system | `generaltests` | `RegularBeforeCasualServingTest`, `RegularGradualCookServingTest` |
| F25 (Atharva) | unit, integration | `casualbrowsingtests` | `CasualBrowsingDecisionTest`, `CasualBrowsingIntegrationTest` |
| F25 (Atharva) | system | `generaltests` | `CasualDeliveryEarlyDecisionTest`, `CasualTiebreakLowestIdTest` |
| F29 (Ansh) | unit, integration | `deliveryoutboundtests` | `DeliveryOutboundTripTest`, `DeliveryOutboundIntegrationTest` |
| F29 (Ansh) | system | `generaltests` | `DeliveryOutboundTripSystemTest`, `DeliveryCumulativeDistanceSystemTest` |
| P02 (Deniz) | unit, integration | `eventseatingoutcometests` | `EventSeatingOutcomeTest`, `EventSeatingOutcomeIntegrationTest`, `EventSeatingFixtures` |
| P03 (Atharva) | unit, integration | `eventorderingtests` | `EventOrderingTest`, `EventOrderingIntegrationTest`, `EventOrderingFixtures` |

Extra system tests: `staffchangetests`, `recipeincidenttests` and `basicdishtests` for F31 and F32; `regularplanningtests` and `officehourprobes` across features.

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

### Vlad Mihai Marciu

Days are counted as working days (Monday to Friday) from Thursday, Sep 10. Work done on Saturdays and Sundays is listed separately as "over the weekend".

- **F01 was finished over the weekend:** planned for Days 1-2, it was started on Day 1 and was finished over the first weekend. Fixes followed until the second weekend (`--help`, tick logging, the `maxTicks` range check, the output path).
- **Different feature order:** F26 (food preferences), F31 (staff change incident) and F32 (recipe change incident) were all implemented on Day 3, instead of on Days 6, 7 and 5 due to them being more isolated and easier to implement without requiring other parts of the system. F23 (casual customer behaviour) was implemented on Day 5 instead of Day 4.
- **F18 (ordering) took longer than planned:** planned for Day 3, it was started over the first weekend and worked on until Day 6. This was due to the feature being a bit more complicated than it seemed at first glance,
- **Work over the weekends:** both weekends were used. The first finished F01 and started F18. The second covered the tests for F07, P02 and P03, and the recipe change fix.
- **Work that is related to my features**
    - the restaurant's preparation phase (Day 5)
    - refusing customers in the last three opening ticks (Day 7)
    - refreshing the browsing estimates every tick, and the ingredient planning fix for regular groups (Day 8)
- **Testing of my assigned features:** started on Day 5 as planned, but ran until the second weekend (F07, P02 and P03 were tested over the weekend) instead of being finished by Day 8.
- **Days 8-10:** the plan had these days for multi-evening system tests, mutant runs and bug fixing. They went mainly into probe system tests against the reference implementation, which narrowed down where the specification was ambiguous. They also went into fixing the defects these revealed (recipe change incident, driver availability, regular ingredient planning, serving meals of customers who walked out) and moving the system tests that pass on the reference implementation to the mutant lists.
- **System tests outside of the assigned features:**
- **Attribution mistakes:** Final statistics in Simulation (logging code) because of a merge conflict in main. The attributions are accidentaly assigned to me but the code is actually authored by Ansh


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

**Thu 10 Sep** — basic parser controller and first incident-parsing pass (F06); undid a bad change to the simulation
entry point.

**Fri 11 Sep** — Parser Controller + Incident Parser beta, wired into `FoodParser`, incident parsing working end to
end; fixed detekt issues in the new parser code. First logger unit tests (F02) and first Restaurant Parser system
tests.

**Mon 14 Sep** — `recruitWaitersForEvents` ~80% done and Event Group work (F25); Packaging Change Incident started
(F33); event escorting started (P03); Browsing Service made detekt-clean (F28); a couple of minor bug fixes along the
way.

**Tue 15 Sep** — sim-config parsing fix in `ParserController`; fixed a bug in `Restaurant`'s end-of-evening flow at
Skerdi's request, not my feature (see section 1); fixed the simulation being able to start even after being told a
config file was invalid. Incident-parser cleanup, attribution/KDoc fixes, and the first parser system test.

**Wed 16 Sep** (28 commits, the busiest weekday) — Event Group work (F25), `getCurrentEventDish()`, two rounds of
Browsing Service updates (F28), Event Escorting updates (P03), `RestaurantStats` reporting from browsing decisions.
`IncidentParser` needed a same-day reconciliation between two competing edits from Ansh and me. Sequence-diagram-2
unit and system test.

**Thu 17 Sep** — Event Group place-order logic (F25); Browsing Service minors (F28). Cooking tests covering F10/F12
(`CookUnitTest`, `KitchenCookingProcessTest`, section 4); "more than 1 EXEC cook" and restaurant system tests.

**Fri 18 Sep** — FOH log corrections and fixes. Supplier unit test (F08); Staff Management system tests (F17).

**Mon 21 Sep** — introduced and fixed the same regression the same day: a morning change weakened the browsing
service's check for whether a restaurant has room for a walk-in dine-in group, letting it point a group at a
restaurant without enough seats; caught and reverted it that evening, also fixing the event-restaurant open check to
look at the right evening. Separately fixed a crash risk in the event favourite-dish lookup, and closed the real gap
`EventOrderingSpecTest` had been documenting since it was written — event orders are now capped correctly at a
waiter's per-tick limit ("Event Ordering is now fixed", see section 2). `CountertopAvailabilityTest` disabled rather
than fixed on the spot. A wider code review of the browsing service and event ordering that day found more than the
one regression above: two issues are **still open** as of this report (the free-seat count doesn't subtract dine-in
customers already decided but not yet seated; the event-seat estimate only looks at tables free *tonight*, not prior
reservations), and one has since been **fixed** (event orders weren't appearing in the ordering log at all).

**Tue 22 Sep** — `EventOrderingSpecTest` fixed for real; available-drivers logic corrected. Browsing Service
delivery-side updates (F28). Explicit F22 unit tests; F10 order-queue and F30 end-of-evening tests
(`OrderQueueLifecycleTest`/`eveningclosetests`). FOH Staff Management + Statistics system test (F17/F07); the
`kitchenschedulingtests` suite. Also confirmed (not a bug) that the browsing step's looser bar-table seat check is
safely caught by the later seating step, and wrote a test documenting that.

**Wed 23 Sep** — added, then same-day reverted, a wrong tick calculation for when an event group's kitchen prep
needed to finish by. Fixed the driver-availability count to exclude drivers still returning from a delivery.
System-test clean-up.

**Thu 24 Sep** — removed stale commented-out system tests from the Sep 19-21 hardening push. KDoc/comments across
implementation files, no logic changes.

**Fri 25 Sep** — found and fixed a bug in a shared test helper the same day: it collapsed event and casual serving
priority into one tier instead of ranking regular/event/casual separately, unnoticed until an event group was run
through it for the first time. Added coverage for event ordering (`EventOrderingSpecTest`), FOH waiter-id/event-group
(`WaiterIdEventGroupTest`), restaurant-parser edge cases, plus new F07 (`StatisticsCookedAndDeliveredRealRestaurantTest`)
and F27 (`WaitingForEventFoodTest`) tests.

### Over the weekend (recorded separately, not part of the week)

**Sat 12 Sep** — re-added and adjusted `ParserController`; fixed `ScenarioParser`; adjusted `UnavailabilityIncident`'s
stock interaction per Ved's review (F09/F34 overlap, see section 1). Restaurant-parser testing.

**Sun 13 Sep** — Browsing Service (F28) first complete pass; Unavailability Incident (F34) made available — both
roughly 4-5 days ahead of the plan's Day 6/7 slots (see section 2).

**Sat 19 Sep** — an escorting-log bug fix and further event-escorting work (P03); pantry test updates (P01); fixed a
`skipUntilString` misuse in my own test; browsing/escorting edge-case tests; commented out/disabled a couple of
failing tests to keep the build green (the pattern flagged in section 2).

**Sun 20 Sep** (23 commits, the single heaviest day of the project) — more Browsing Service updates (F28), FOH
recruit-waiters fixes, Incident Parser minors; recipe-change-incident and several edge-case tests; Event Group tests;
registered the reference/mutant test suites (see section 6), disabling one restaurant-parser test that didn't match
the reference implementation's behaviour.

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

**Ansh Shekhar Tiwatne:** Option 2

- used **NotebookLM** (Gemini 3.1 Pro I think) where I uploaded the specification and adjustments so I can easily use it
  for quickly querying any doubts I had (saved me a lot of time of scrolling through pages)
- used **Claude Code** (mostly Sonnet 5, sometimes Opus 5) 1. to build good scenarios for tests (given specifications
  e.g. x restaurants of y type with z customers or given particular cases) 2. to help refine and better document code I
  wrote (wanted to get my logic to work as simply with few lines and following Kotlin best practices wherever possible)
    3. to quickly write some AB tests after office hours 4. to help debug: did most debugging manually to avoid AI false
       positive, but used some aid for tricker bugs
- no AI was used for writing the implementation report

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

**Atharva Kore:**

*Option 2* (the implementation plan lists this as "Option 1" for me, which appears to be a labelling slip carried over
from the plan template — Deniz's genuine "no AI" declaration is the real Option 1; the content of my entry describes
tool usage, so it is Option 2 in substance):

In practice I only used **Claude (Sonnet 5)** during the implementation phase

- **System tests:** I worked out the scenarios and edge cases myself, against the specification — what should happen,
  and why — then made up the concrete scenarios and had the AI generate the JSON fixture files (food/restaurants/
  scenario configs) matching them. I wrote the actual system test code (the assertions and log-line sequencing)
  myself.
- **Unit tests:** I identified the edge cases and worked out the expected behaviour myself, including my own
  calculations and the pre- and post-conditions for each case, then had the AI turn that specification into the
  actual test code — the test logic and expected values were mine, not generated independently.
- **Log debugging:** simulation runs produce far more log output than is practical to read line by line by hand, so I
  had the AI walk through the logs and summarize them for me, to find where the actual behaviour diverged from what I
  expected.
- **Code documentation:** for the KDoc comments added across my implementation files, I gave the AI my own
  explanation of what each piece of code does and why — the constraints, the intent, anything non-obvious from just
  reading it — and had it turn that into properly formatted KDoc; it wasn't left to infer the documentation from the
  code by itself.
- **This implementation report:** I had the AI cross-reference my own `git log` history against
  `implementation_plan.md` and `feature_assignments.yaml` — verifying every claim (which feature, which day, which
  test, which bug) against the actual commits and source rather than accepting a guess — and present the result as
  this formally structured report.

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
- **System tests**
    - some large full tests in `fulltests`
    - system tests for testing responsibilities in `anshtests`
    - some full tests (`EnormousTest`, `MeticulousTest`, `ThoroughTest`) also in `anshtests`
    - AB Tests in `abtests`
