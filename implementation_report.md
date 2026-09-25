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

### Bob

- ...

---

## Usage of Generative AI

**Deniz Firat Sag:**
*Option 2:*
In the implementation plan, I stated that I would not use generative AI in the implementation phase; that statement is **no longer up-to-date** as I changed my mind and **did use generative AI**. I used Claude Code (Sonnet 5, Opus 5 and Opus 5.5) and Gemini (3.1 Pro) for writing unit, integration and system tests. I did not use any generative AI for writing or debugging implementation code (code under `src/main/`).

*Option 2:*
(insert tool names properly; if applicable add links or add version numbers of the used tools)
Alice used the following tools in the implementation phase:
Tool-1 for code completion and tool Tool-2 for ... . In addition, she used Tool-3 for ...

**Bob:**
...

In case of option 2, add additional sentences in which you provide more details on which tools you used for which specific tasks and to which extent.

We are aware of the potential dangers of using these tools and take full responsibility for any code, documents and other content produced during the group phase.
