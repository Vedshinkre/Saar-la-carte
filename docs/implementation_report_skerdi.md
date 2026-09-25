# Implementation report - Skerdi Cuka (draft to paste into `implementation_report.md`)

Only major contributions are listed. The table shows the work I actually did; where this differs from the assignment in `feature_assignments.yaml`, it is explained in section 2.

## 1. Individual contributions

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

## 2. Adjustments from the implementation plan

- **P04 (Customer - events) was shared with other members.** The specification describes events in several places (restaurant decision, reservation, seating, ordering, serving, escorting, rating), and most of these are covered by other features (F25, P02, P03, P05). It was ambiguous what P04 itself should contain, so we decided in the group to share it with members who had less contribution so far. `EventGroup` was written by Atharva, Vlad and Deniz. My part is the rating of EVENT groups (in P05) and the tests of the event restaurant decision (F25).
- **F21: the escorting of EVENT groups was implemented by Atharva as part of P03 (event other actions),** as we decided in the group (Mon 14 Sep). While adding it to `EscortingProcessor` he also edited the REGULAR and CASUAL escorting in the same class (escorting by the assigned waiter and the log of the escorted customers), so `git blame` shows these lines under his name. They belong to my F21 work; we discussed the changes together and they are attributed to me.
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

## 4. Usage of Generative AI

**Skerdi:**
*Option 2:*
In the implementation plan I announced GitHub Copilot Chat, ChatGPT (GPT-5), Claude (Sonnet 5) and Gemini (3.1 Pro). I diverted from this: I used only Claude, in the form of Claude Code (Claude Sonnet 5), and very minor usage of Chatgpt.

Claude Code was used for debugging the implementation, for my tests. It was not used to write the implementation of my features.

- review of my unit, integration and system tests against the specification and its adjustments (redundancy, coverage, correctness);
- correcting outdated waiting-for-food and delivery tests, and writing the additional tests for the exception cases of the parsers to raise the coverage;
- debugging of the implementation: finding why tests and system tests failed on our jar or on the reference implementation, for example a delivery driver in `DeliveryProcessor` that was never freed after a given-up delivery (fixed, with a test for it);
- improving documentation of public methods.
We are aware of the potential dangers of using these tools and take full responsibility for any code, documents and other content produced during the group phase.
