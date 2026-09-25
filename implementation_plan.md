# Implementation Plan

## Responsibilities


| Member  | Implementation responsibility | Main design area |
|---|---|---|
| Atharva | F06, P03, F28, F25, F33, F34 | Packaging, ingredient incident parsing/handling, customer restaurant decision, browsing service, and event customer other actions  |
| Ansh    | F04, F19, F20, F24, F27, F29, F07 | Restaurant configuration, serving and delivery, casual customer behaviour, and statistics |
| Deniz   | F05, F14, F15, F16, F17, F22, P02 | Customer parsing, front-of-house table reservation/merging/seating and staff management, and regular customer behavior |
| Ved     | F03, F08, F09, F10, F11, F12, P01 | Ingredient/recipe parsing, supplier, pantry, and kitchen order/staff/cooking logic |
| Vlad    | F01, F18, F23, F32, F26, F31 | Simulation flow, ordering, casual customer behavior, food preferences, staff and recipe change incidents |
| Skerdi  | F02, F13, F21, F30, P04, P05 | Logging, menu availability, escorting, end-of-evening handling, and event customer/rating behavior |


Each member owns 6-7 feature, and for F01-F07 every member is primarily responsible for at most two of them (as required). The sum of difficulty of the features is roughly equal for each person with respect to our design. We kept some closely related classes together. 

Testers for every feature are always members other than the implementer, and we spread testing duties evenly across the group (each member tests roughly 13.33 features).

### Feature Assignment

| Feature | Implementer | Testers |
|---|---|---|
| F01 | Vlad | Skerdi, Ved |
| F02 | Skerdi | Atharva, Deniz |
| F03 | Ved | Ansh, Skerdi |
| F04 | Ansh | Skerdi, Atharva |
| F05 | Deniz | Vlad, Skerdi |
| F06 | Atharva | Ved, Deniz |
| F07 | Ansh | Vlad, Atharva |
| F08 | Ved | Skerdi, Atharva |
| F09 | Ved | Skerdi, Deniz |
| F10 | Ved | Deniz, Atharva |
| F11 | Ved | Deniz, Vlad |
| F12 | Ved | Ansh, Atharva |
| F13 | Skerdi | Ved, Deniz |
| F14 | Deniz | Vlad, Atharva |
| F15 | Deniz | Ansh, Ved |
| F16 | Deniz | Vlad, Ansh |
| F17 | Deniz | Ved, Atharva |
| F18 | Vlad | Deniz, Skerdi |
| F19 | Ansh | Vlad, Deniz |
| F20 | Ansh | Ved, Skerdi |
| F21 | Skerdi | Deniz, Ansh |
| F22 | Deniz | Skerdi, Atharva |
| F23 | Vlad | Ansh, Deniz |
| F24 | Ansh | Atharva, Ved |
| F25 | Atharva | Skerdi, Vlad |
| F26 | Vlad | Deniz, Ved |
| F27 | Ansh | Skerdi, Atharva |
| F28 | Atharva | Ansh, Deniz |
| F29 | Ansh | Ved, Vlad |
| F30 | Skerdi | Atharva, Ansh |
| F31 | Vlad | Ved, Ansh |
| F32 | Vlad | Skerdi, Ved |
| F33 | Atharva | Skerdi, Deniz |
| F34 | Atharva | Ved, Ansh |
| P01 | Ved | Atharva, Skerdi |
| P02 | Deniz | Ansh, Vlad |
| P03 | Atharva | Ansh, Vlad |
| P04 | Skerdi | Deniz, Ansh |
| P05 | Skerdi | Deniz, Ved |


Though we will likely use a few stub functions until complete implementation of features, it is ideal to have a few features completed before others can be built or tested on top of them:

* **F01 (Simulation Managing / CLI) and F02 (Logging)** come first every other feature needs the CLI/main loop to run in and the logging calls to report through.
* **F03-F06 (parsing the food, restaurant, customer, and incident files)** must work before the objects they produce (ingredients, recipes, restaurants, customer groups, incidents) can be used by anything else.
* **F08-F12 (supplier, pantry, and cooking)** need to be in place before F18-F21 (ordering, serving, delivery, escorting) can be tested end-to-end, since those steps rely on meals actually being cooked.
* **F14-F17 (table reservation, merging, seating, staff management)** need to exist before F18-F21, since customers must be seated before they can order or be served.
* **F25/F28 (restaurant decision / browsing service)** need to exist before the casual and event customer behaviors that depend on them (F23, F24, P04) can be tested.


## Timeline

**Rough Milestones**
1. **Day 2:** Parsing finished; CLI and logging in place.
2. **Day 7:** All 39 features implemented.
3. **Day 9:** System tests must exist for every feature. Feedback from code review worked on.
4. **Day 12:** Final testing done, report finished, submission pushed.

Parsing (F03-F06) is finished within the first 2 days. From Day 3 through Day 9, everyone works on their remaining features while also continuously writing unit tests and system tests for whatever has already been finished. All features are implemented by Day 7 (5 work days before the Day 12 deadline), and by Day 9 the system test suite already covers every feature, so Days 10-12 are spent running, fixing, and finalizing the implementation.

**Day 1-2 (Parsing)**

* **Skerdi:** Work on F02 (Logging), needed by all other features.
* **Ansh:** Work on F04 (Parse Restaurants).
* **Deniz:** Work on F05 (Parse Customers).
* **Atharva:** Work on F06 (Parse Incidents).
* **Ved:** Work on F03 (Parse Ingredients & Recipes).
* **Vlad:** Work on F01 (Simulation Managing / CLI), needed by all other features.

**Day 3**

* **Skerdi:** Work on F13.
* **Ansh:** Work on F19 and F07.
* **Deniz:** Work on F14.
* **Ved:** Work on F08.
* **Vlad:** Work on F18.
* **All members:** Alongside the feature work above, start writing unit tests and system tests for the Day 1-2 features (F01-F06), based on the specification's example scenarios (single restaurant, single evening, the provided JSON listings).

**Day 4**

* **Skerdi:** Work on F21.
* **Ansh:** Work on F20.
* **Deniz:** Work on F15.
* **Atharva:** Work on.
* **Ved:** Work on F09.
* **Vlad:** Work on F23.
* **All members:** Continue writing unit tests and system tests, now also covering the Day 3 features.

**Day 5**

* **Skerdi:** Work on F30.
* **Ansh:** Work on F24.
* **Deniz:** Work on F16.
* **Atharva:** Work on F25.
* **Ved:** Work on F10.
* **Vlad:** Work on F32.
* **All members:** Continue writing unit tests and system tests, now also covering the Day 4 features; testers start reviewing the features assigned to them in the Feature Assignment table.

**Day 6**

* **Skerdi:** Work on P04.
* **Ansh:** Work on F27.
* **Deniz:** Work on F17 and F22.
* **Atharva:** Work on F33 and P03.
* **Ved:** Work on F11 and F12.
* **Vlad:** Work on F26.
* **All members:** Continue writing unit tests and system tests, now also covering the Day 5 features.

**Day 7 (Implementation complete)**

* **Skerdi:** Work on P05 (last remaining assigned feature).
* **Ansh:** Work on F29 (last remaining assigned feature).
* **Deniz:** Work on P02 (last remaining assigned feature).
* **Atharva:** Work on F34, F28 (last remaining assigned feature).
* **Ved:** Work on P01 (last remaining assigned feature).
* **Vlad:** Work on F31 (last remaining assigned feature).
* **All members:** Continue writing unit tests and system tests, now also covering the Day 6 features.

**Someday after code review**

* **All members:** work on the feedback from the code review on their respective features and tests (if any).

**Day 8**

* **All members:** Finish unit tests and system tests for the last batch of features (Day 7), so every feature has coverage; extend existing system tests to multi-tick and multi-restaurant scenarios; testers check their assigned implementer's code against edge cases (table merging thresholds, event seating priority, waiter/driver id assignment, tie-breaking rules).

**Day 9**

* **All members:** Integrate the full evening flow (preparation phase and serving phase) across kitchen, front-of-house, customer, and incident logic; write additional system tests for multi-evening scenarios (regulars returning on their period, events reserved days in advance, incidents spanning several evenings, ingredient expiry); fix integration bugs found along the way; run the released mutants against the current system tests for the first time and note which ones slip through.

**Day 10**

* **All members:** Run the full system test suite built up over Days 3-9; write down all failures and decide which to fix first; fix each defect and add a test for it so the same bug doesn't come back unnoticed; add tests for the mutants that slipped through on Day 9 and re-run them.

**Day 11**

* **All members:** Check that logging and statistics (F02, F07) match the exact format and ordering required by the specification; go through the tricky edge cases once more (rounding rules, tick-duration math, ascending-id/alphabetic tie-breaks, action limits per waiter/tick); run the released mutants one more time and patch any that are still uncaught; run all unit, integration, and system tests once more to make sure nothing broke; make sure the project builds cleanly and passes `detekt` on the main branch.

**Day 12**

* **All members:** Fix any remaining bugs; run the complete system test suite one last time; finalize the implementation report, documenting each member's contributions; confirm the last commit on `main` is up to date; push and verify the submission.

Testing is continuous throughout the project. Each implementer writes unit tests for their features as soon as development begins, while the two assigned testers review each feature, add edge-case and integration tests, and check it against the specification's acceptance criteria before it is considered complete. From Day 3 through Day 9, the team also builds the system test suite for the completed features. Days 10-12 are then used to run the full suite, fix any defects, strengthen tests where necessary, and finalize the submission.

## Usage of Generative AI
**Ansh Shekhar Tiwatne:**
Option 2: Ansh will use GitHub Copilot Chat , ChatGPT (GPT-5), Claude (Sonnet 4), and Gemini (3.1 Pro) during the implementation phase. Intended uses will be documented before submission.

**Atharva Kore:**  
Option 1: Atharva will use GitHub Copilot Chat , ChatGPT (GPT-5), Claude (Sonnet 5), and Gemini (3.1 Pro) during the implementation phase. Intended uses will be documented before submission.

**Deniz Firat Sag:**  
Option 1: Deniz will not use generative AI in the implementation phase.

**Skerdi Cuka:**  
Option 2: Skerdi will use GitHub Copilot Chat , ChatGPT (GPT-5), Claude (Sonnet 5), and Gemini (3.1 Pro) during the implementation phase. Intended uses will be documented before submission.

**Ved Shinkre:**  
Option 2: Ved will use GitHub Copilot Chat , ChatGPT (GPT-5), Claude (Sonnet 5), and Gemini (3.1 Pro) during the implementation phase. Intended uses will be documented before submission.

**Vlad Mihai Marcio:**  
Option 2: Vlad will use GitHub Copilot Chat , ChatGPT (GPT-5), Claude (Sonnet 5), and Gemini (3.1 Pro) during the implementation phase. Intended uses will be documented before submission.

We are aware of the potential dangers of using these tools and will take full responsibility for any code, documents and other content produced during the group phase.
