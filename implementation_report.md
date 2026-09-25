# Implementation Report

## Individual Contributions

Resubmit `feature_assignments.yaml` and make sure to update it if anything changed since the design defense. Both files should be at the root of your group's repository. Only include major contributions (no minor bugfixes or tiny code additions).

If responsibilities changed during implementation, keep the table factual for actual work and document all changes in the adjustments section below.

---

## Adjustments from the Implementation Plan

- [Describe adjustment here, e.g., which tasks took more time or were reassigned]

---

## Detailed Timeline

### Deniz Firat Sag

- **Day 1:**
  I and my teammates worked on the project skeleton.
- **Day 2:**
  I completed `F05 (Parse Customers)` with a parser for each customer group type (regular, event and casual) as well as a parser for food preferences. On **Saturday**, I completed `F14 (FOH - Evening Table Reservation)` and `F15 (FOH - Table Merging & Tables)`.
- **Day 3:**
  I wrote all the parts of `F16 (FOH - Seating)`, `F17 (FOH - Staff Management)` and `P02 (FOH - Event Seating)` that could be written independently of the progress we had made and added experience changes into the seating process as part of `F22 (Customer - Regulars)`. I also added the experience changes for event and casual groups during the seating process, with agreement from Skerdi and Vlad, since it is only 5 lines spread all over the file and 3 of those are shared between their customer group type and regular groups, i.e. I had to write them for regular groups anyway.
- **Day 4:**
  I split the welcoming part (`F16`) - that I wrote - of `FrontOfHouse` into `ArrivalProcessor` to reduce the number of functions in `FrontOfHouse` so as to make it easier and more manageable to work individually without constant merge conflicts. I also made minor fixes in the `CustomerParser` and fixed/updated the `ArrivalProcessor` in accordance with the progess we had made.
- **Day 5:**
  I added the periodic visiting functionality to `RegularGroup` as part of `F22 (Customer - Regulars)`, completing it, fixed 2 validation issues in the `CustomerParser` and refactored parts of `ArrivalProcessor` to make it more extensible and easier to reason about.
- **Day 6:**
  I fixed a minor parsing issue in the `FoodPreferenceParser`, relaxed validation by removing a validation function, added further validation to `CustomerParser`, fixed a logging and mutability bug in `ArrivalProcessor` and added order history to `RegularGroup`.
- **Day 7:**
  I spent the day fixing bugs in `ArrivalProcessor` and `RegularGroup`. On **Saturday**, I continued with fixing bugs in `ArrivalProcessor`. On **Sunday**, I fixed a minor bug in `ArrivalProcessor` and wrote unit tests for `F02 (Logging)`.
- **Day 8:**
  I made minor fixes, wrote unit, integration and system tests for `F33 (Incident - Packaging Change)` and unit and system tests for `F06 (Parse Incidents)`.
- **Day 9:**
  I wrote more unit and system tests for `F06 (Parse Incidents)`, unit, integration and system tests for `P05 (Customer - Rating)`
- **Day 10:**
  I wrote a system test. Then, I wrote unit and integration tests for `F28 (Browsing Service)`, `F13 (Restaurant - Menue)`, `F26 (Customer - Food Preferences)`, `F21 (FOH - Escorting)`, `P04 (Customer - Events)`.
- **Day 11:**
  I wrote unit and integration tests for `F23 (Customer - Casual)` and system tests for my features that I had not written system tests for with the exception of `F02 (Logging)`, for which it does not make sense to write system tests. I then wrote further system tests targeting remaining mutants, after which I went on with writing unit and integration tests for `F09 (Pantry - Buying Ingredients)`, `F10 (Cooking - Order Queue)`, `F11 (Cooking - Staff & Staff Management)`, `F18 (FOH - Ordering)`, `F19 (FOH - Serving)`.
- **Day 12:**
  I finalized my part of the implementation report.

I would like to bring to your attention that, on days, when I seemingly have little contribution, the bug fixes I were making took a long time, since, being responsible for customer arrivals, I was interacting with many components of the simulation and had to make sure that the invariants my teammates were expecting were being held. This meant that I had to spend some time reading through their code to understand it and could not just work on my own code by myself. I would like to mention here as well that I did not use any generative AI for debugging.

### Ansh

- **Day 1:**
  - worked on project skeleton with team based on our class diagram
  - worked on restaurant parser (F04)

- **Day 2:**
  - completed restaurant parser (F04)
  - started work FOH - serving (F19)
  - realized that we interpreted the partial ordering logic incorrectly in our design; corrected the logic and discussed all the changes needed to the Order class with Vlad (as it was his code)
  - decided to focus on serving and eating features and work on the relatively simple statistics (F07) later as it depended on code yet to be written by others

- **Sat:**
  - completed FOH - serving (F19)
  - started work on delivery related features (F20, F24, F29)
  - figured remainingTicks was redundant in Driver (in our design) so removed it, this was due to a recently clarified forum post on the driver logging distance covered per tick vs accumulated distance of the trip

- **Sun:**
  - completed delivery related features (F20, F24, F29) i.e. `processDelivery()` and the `Driver` class methods in our diagrams
  - completed eating related F27 i.e. `processEating()` in our diagrams

- **Day 3:**
  - completed the relatively simple simulation statistics (F07) code (now essentially feature complete!)
  - refined other features and discussed implemented features with team
  - fixed several bugs with team to get project to assemble (many detekt issues were yet to be resolved)

- **Day 4:**
  - with team split up the code for the main functions in FOH into separate files in the helpers directory as FOH was getting messy (especially to work in since we have to be careful with attributions and also avoid merge conflicts); we also had too many (private) functions in one place which is not good and caused a detekt issue, this change purely moved logic into helper classes so we figured it wasn't a large deviation from our design
  - dealt with some trouble regarding git attributions since we made a commit which we then a rolled back only to then roll back that rollback all on my laptop (since I had experience with git), which caused others' FOH code to be attributed to me, had to fix it by manually emptying out and pasting lines of code again since force pushes that could fix this were disabled
  - dedicated the entire remaining day to getting the project to build (with detekt) so we don't waste any test runs tomorrow morning

- **Day 5:**
  - fixed parser issues with team
  - dedicated significant time finding a fix for SameBasicDishes test (the only one we were still failing for parsing tests) as we believed the parsing error could be causing several component tests to fail, tried several fixes
  - fixed issues with and simplified the parser controller
  - finally started work on my tests for my assigned features (had to wait a little longer than planed for the code to settle in a state that it was testable)
  - worked on F03 (parse ingredients & recipes) tests with Skerdi (we will push it tomorrow)

- **Day 6: (CODE REVIEW)**
  - had code review from 11-12
  - Skerdi and I ensured we had good test coverage for F03 (parser ingredients & recipes) for `FoodParser`
  - completed F12 (cooking) tests with Atharva, for CookUnitTest he roughly wrote tests for parts before cooking start, I did the tests for after cooking start; for the KitchenCookingProcess (integration) test I took things to do within the processCooking function and prepare ingredients, he tested the rest

- **Day 7:**
  - started working on system tests, wrote a few scenarios, ran the simulations with the JSONs and went through the logs
  - noted several discrepancies between interaction of our code found when running these scenarios
  - spent the entire day implementing several of these fixes with the team (some queues not populated, improper event group ordering waiter assignment), we went a large number component tests passed that night

- **Sat:**
  - divided some test work among teammates I'm testing with
  - helped some teammates with ambiguities in their features
  - improved my big written scenarios, restaurants, and food JSONs from yesterday, ran them against our implementation to get logs, made changes to these according to the specification and asserted those in some `fulltests` to run on the reference, then caught many mutants with these and used them to pinpoint behavior of our implementation
  - pair programmed tests on F31 (staff change incident) with Ved
  - started work on my part of P02 and P03 feature tests (that I share with Vlad)

- **Sun:**
  - finished P02 and P03 feature tests
  - finished F15 (table merging & tables) tests with Ved, I did the part for unreserved table merging, he did the reserved
  - pair programmed tests on F34 (ingredient unavailability) with Ved (still some work left)

- **Day 8:**
  - found several small bugs with the way delivery related code was being used that was causing us to fail delivery component tests, worked on comments for suggested fixes, we all fixed it together and passed several additional tests
  - divided testing F30 with Atharva, we decided I'll do the strictly end of evening part (endEvening, resetDrivers, 24-th tick stuff), he'll do end of opening time (startFohClosing, escortAllAtClosing, endOfOpeningTime, and kitchen side of it), we'll both also look over each other's tests
  - completed my part of F30 (end of evening) tests
  - the office hour was helpful for the remaining test we were failing, wrote some A/B tests based on feedback to check ambiguities against the reference
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
  - finished work on all leftover system tests relating to my testing responsibilities, registered them on the reference to then maybe catch the leftover mutants
  - wrote a few more full system tests covering large and complex scenarios with multiple customers and restaurants, running through multiple evenings
  - improved documentation of my code
  - refined the implementation report

- **Day 12:**
  - refined the implementation report

- **Some notes:**
  - Early during the implementation when we decided split some `FrontOfHouse` helpers into separate files, we rolled back a commit and then again decided to rollback that rollback on my laptop (as I was experienced with git) but unexpectedly all the code involved in the second rollback was reattributed to me. Since force pushes weren't allowed we had to manually empty out files and have the author of the code paste back them to fix the line attributions. This may have affected some of my git statistics e.g. inflated the number of lines added / removed
  - I have some really large full system tests with complex multiple evening scenarios (in the `fulltests` folder) that cover most of the implementation behavior. I wanted to keep the code for these minimal and maintainable by not having several calls to `assertNextLine` in the Kotlin code but rather storing them separately in `.log` files where every line is asserted by default and I used ellipses denote a `skipUntilString` (for places with ambiguity). This allows me to copy logs from our implementation run on my scenario JSONS and easily make changes wherever needed to then test against the reference / catch mutants. This way my actual "Kotlin" code for the full system tests was very minimal, so in the office hour I was given feedback to still write some larger system tests, so I also have 3 more full tests (with well thought large scenarios) done in the way my teammates did with several assert calls in the `anshtests` folder
  - I made some merges from dev to main on GitLab website which were attributed to a duplicate profile of mine. I have added a mailmap for this as recommended by our tutor. This only concerns merge commits, all line attributions are kept by my single original profile.
  - Ved and I pair programmed good incident tests together, hence one of the two tests was committed by him while the other by me. We didn't want spend much time finding a way to split it and thought this wouldn't be a big deal. We wanted to clarify it in the report for the record.

### Bob

---

## Usage of Generative AI

**Deniz Firat Sag:**
*Option 2:*
In the implementation plan, I stated that I would not use generative AI in the implementation phase; that statement is **no longer up-to-date** as I changed my mind and **did use generative AI**. I used Claude Code (Sonnet 5, Opus 5 and Opus 5.5) and Gemini (3.1 Pro) for writing unit, integration and system tests. I did not use any generative AI for writing or debugging implementation code (code under `src/main/`).

*Option 2:*
(insert tool names properly; if applicable add links or add version numbers of the used tools)
Alice used the following tools in the implementation phase:
Tool-1 for code completion and tool Tool-2 for ... . In addition, she used Tool-3 for ...

**Ansh:**
DOTO

**Bob:**
...

In case of option 2, add additional sentences in which you provide more details on which tools you used for which specific tasks and to which extent.

We are aware of the potential dangers of using these tools and take full responsibility for any code, documents and other content produced during the group phase.
