# 🍽️ SaarLaCarte — Restaurant Simulation Engine

> **Software Engineering Lab (SeLab) · Saarland University · Group 10**

SaarLaCarte is a tick-based, discrete-event simulation of a multi-restaurant dining ecosystem. It models everything from ingredient procurement and kitchen workflows to front-of-house seating, delivery logistics, and customer ratings.

The simulation is driven by JSON configuration files and runs evening-by-evening until a configurable tick limit is reached.

---

## 📖 Table of Contents

* [Overview](#overview)
* [Architecture](#architecture)
* [Project Structure](#project-structure)
* [Getting Started](#getting-started)

  * [Prerequisites](#prerequisites)
  * [Building](#building)
  * [Running the Simulation](#running-the-simulation)
* [Configuration Files](#configuration-files)
* [Simulation Lifecycle](#simulation-lifecycle)
* [Key Concepts](#key-concepts)

  * [Customer Groups](#customer-groups)
  * [Restaurant Components](#restaurant-components)
  * [Incidents](#incidents)
* [Testing](#testing)

  * [Unit Tests](#unit-tests)
  * [System Tests](#system-tests)
  * [Code Coverage](#code-coverage)
* [Code Quality](#code-quality)
* [Tech Stack](#tech-stack)

---

## 🔎 Overview

The simulator reads three JSON configuration files:

* `food.json`
* `restaurants.json`
* `scenario.json`

It then runs a multi-evening simulation in which:

* **Restaurants** open and close according to configurable schedules.
* Each restaurant manages its own:

  * Kitchen staff
  * Front-of-house staff
  * Delivery drivers
  * Tables
  * Pantry and ingredient stock
  * Menu
* **Customer groups** arrive according to the scenario:

  * Regular customers
  * Casual customers
  * Event groups
* Customers can browse for restaurants, get seated, order dishes, eat, receive deliveries, and leave ratings.
* **Incidents** can modify staff, recipes, packaging, or ingredient availability.
* **Statistics** are collected for each restaurant throughout the simulation.

At the end of the simulation, statistics such as meals cooked, customers served, deliveries completed, and ratings given are logged per restaurant.

---

## 🏗️ Architecture

SaarLaCarte is organized into several cooperating components. The application starts in `Main`, where command-line arguments are parsed and passed to the parsing layer. The `ParserController` parses and cross-validates the food, restaurant, and scenario configuration files before creating the `SimulationConfig` used by the simulation.

### High-Level Architecture

```text
┌─────────────────────────────────────────────────────────────────────┐
│                              Main                                   │
│                                                                     │
│  CLI argument parsing                                               │
│  parseCommandLineArgs()                                             │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       ParserController                              │
│                                                                     │
│  ┌────────────────┐  ┌──────────────────┐  ┌────────────────────┐  │
│  │   FoodParser   │  │ RestaurantParser │  │   ScenarioParser   │  │
│  │                │  │                  │  │                    │  │
│  │ Ingredients    │  │ Restaurants      │  │ Customer Groups    │  │
│  │ Recipes        │  │ Tables           │  │ Incidents          │  │
│  │ Food validation │  │ Staff            │  │ Scenario validation│  │
│  └────────────────┘  └──────────────────┘  └────────────────────┘  │
│                                                                     │
│                 Cross-validation of all configuration data          │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         SimulationConfig                            │
│                                                                     │
│  • Restaurants                                                     │
│  • Customer groups                                                 │
│  • Incidents                                                       │
│  • Ingredients                                                      │
│  • Recipes                                                          │
│  • Restaurant statistics                                           │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                           Simulation                                │
│                                                                     │
│  1. Parse & validate configuration                                 │
│  2. Initialize objects                                             │
│  3. Apply incidents                                                │
│  4. Preparation phase                                              │
│  5. Serving phase                                                  │
│  6. End evening                                                    │
│  7. Calculate statistics                                           │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
       ┌────────────┐   ┌────────────┐   ┌──────────────┐
       │ Restaurant │   │  Customer  │   │   Incident   │
       │            │   │            │   │              │
       │ Kitchen    │   │ Regular    │   │ StaffChange  │
       │ FrontOfHouse│  │ Casual     │   │ RecipeChange │
       │ Pantry     │   │ Event      │   │ Packaging    │
       │ Countertop │   │            │   │ Unavailability│
       │ Staff      │   │            │   │              │
       └─────┬──────┘   └────────────┘   └──────────────┘
             │
       ┌─────┴─────────────────────────────────────┐
       │                                            │
       ▼                                            ▼
┌──────────────────┐                       ┌──────────────────┐
│      Actors      │                       │       Food       │
│                  │                       │                  │
│ Cook             │                       │ Ingredient       │
│ Waiter           │                       │ IngredientPackage│
│ Driver           │                       │ Recipe           │
│ RestaurantStaff  │                       │ Order            │
│                  │                       │ Dish             │
└──────────────────┘                       │ Pantry / Stock   │
                                           │ Supplier         │
                                           └──────────────────┘
```

### Application Flow

The main execution flow is:

```text
Command Line Arguments
          │
          ▼
        Main
          │
          ▼
  ParserController
          │
          ├── FoodParser
          ├── RestaurantParser
          └── ScenarioParser
          │
          ▼
   Cross-Validation
          │
          ▼
 SimulationConfig
          │
          ▼
      Simulation
          │
          ├── Execute Incidents
          │
          ├── Preparation Phase
          │
          ├── Serving Phase
          │      │
          │      └── Tick-by-tick processing
          │
          └── Calculate Statistics
```

The `Simulation` coordinates the evening lifecycle through methods such as `runSimulation()`, `simulateEvening()`, `executeIncidents()`, `executePreparationPhase()`, `executeServingPhase()`, and `calculateStatistics()`.

---

### 🧩 Core Components

#### Main

`Main` is the application entry point. It is responsible for processing the command-line arguments and starting the application.

```text
Main
├── main(args)
└── parseCommandLineArgs()
```

---

#### Parser Layer

The parser layer converts the JSON configuration files into domain objects and performs validation and cross-validation.

```text
ParserController
├── FoodParser
│   ├── Ingredients
│   └── Recipes
│
├── RestaurantParser
│   ├── Restaurants
│   ├── Tables
│   └── Kitchen Staff
│
└── ScenarioParser
    ├── CustomerParser
    └── IncidentParser
```

`ParserController` additionally validates relationships between restaurants, food, scenarios, customers, deliveries, and incidents.

---

#### Simulation

The `Simulation` class coordinates the complete simulation.

It maintains:

* Restaurants
* Customer groups
* Incidents
* The `BrowsingService`

Its main lifecycle consists of:

1. Executing incidents
2. Preparing restaurants for the evening
3. Running the serving phase tick-by-tick
4. Calculating final statistics

---

#### Restaurant

Each restaurant contains the components required to operate independently:

```text
Restaurant
│
├── RestaurantStats
├── FrontOfHouse
├── Kitchen
├── RestaurantStaff
│   ├── Cooks
│   ├── Waiters
│   └── Drivers
│
└── Tables
```

The restaurant is responsible for preparing itself for an evening, managing its customer queue, and simulating its behavior on each tick.

---

#### Kitchen

The kitchen coordinates cooking and ingredient planning.

```text
Kitchen
│
├── Pantry
├── Cooks
├── Order Queue
├── Ready Dishes
└── Cooking Statistics
```

Its responsibilities include ingredient planning, creating shopping lists, assigning recipes to cooks, processing cooking tick-by-tick, and tracking completed meals. The implementation also supports batch cooking by combining dishes using the same recipe for a cook.

---

#### Front of House

The `FrontOfHouse` component coordinates the customer-facing restaurant operations.

```text
FrontOfHouse
│
├── Tables
├── Waitstaff
├── Drivers
├── Countertop
├── In-house Groups
├── Delivery Groups
├── Customer → Table mappings
└── Turned-away Groups
```

Its responsibilities include:

* Table reservations
* Arrival, seating, and ordering
* Serving
* Deliveries
* Eating
* Escorting
* Ratings
* Waiter and driver assignment
* Table merging and dismantling

---

#### Countertop

The countertop provides the connection between the front of house and the kitchen.

```text
Countertop
│
├── Pantry
├── Order Queue
└── Cooks
```

It handles:

* Ingredient reservations
* Available recipe selection
* Adding orders to the kitchen workflow

---

### 👥 Actors

The restaurant staff is divided into three main actor types:

```text
RestaurantStaff
│
├── Cook
├── Waiter
└── Driver
```

#### Cook

A cook has a specific `CookType`, can be assigned recipes, and tracks cooking progress and remaining ticks.

Supported cook types include:

* `EXEC`
* `SOUS`
* `TOURNANT`
* `SAUCE`
* `FISH`
* `ROAST`
* `VEGETABLE`
* `PASTRY`

#### Waiter

Waiters track their current workload and action-specific tick loads. They are responsible for serving dishes and escorting customer groups.

#### Driver

Drivers handle delivery orders and move through different delivery states:

```text
IDLE
  │
  ▼
DELIVERING
  │
  ▼
RETURNING
  │
  ▼
WAITING
```

---

### 🍴 Food Domain

The food subsystem contains the objects used to represent ingredients, recipes, orders, dishes, and inventory.

```text
Food
│
├── Ingredient
├── IngredientPackage
├── Recipe
├── Order
├── Dish
├── Stock
├── Supplier
└── Pantry
```

A `Recipe` defines its ingredients, cooking duration, and required cook types. An `Order` contains dishes, while each `Dish` tracks its recipe, cooking status, and eating progress.

---

### 🧑‍🤝‍🧑 Customer Domain

Customer groups are represented by a common `CustomerGroup` abstraction with three concrete types:

```text
CustomerGroup
│
├── RegularGroup
├── CasualGroup
└── EventGroup
```

Each customer group contains information such as:

* Group size
* Food preferences
* Visiting tick
* Current order
* Table type
* Current restaurant experience

#### RegularGroup

Regular groups are associated with a specific restaurant and maintain an order history and reservation information.

#### CasualGroup

Casual groups can specify preferred restaurant types, visiting evenings, delivery distance, and rating likelihood.

#### EventGroup

Event groups contain event-specific restaurant preferences, a planned event evening, and favourite dishes by restaurant type.

---

### 🔎 Browsing Service

The `BrowsingService` is responsible for finding eligible restaurants for customer groups.

It uses restaurant statistics including:

* Available drivers
* Available seats
* Available event seats
* Positive and negative ratings
* Opening hours
* Menu
* Restaurant type

It also checks whether a restaurant's menu matches a customer's food preferences.

```text
CustomerGroup
      │
      ▼
BrowsingService
      │
      ├── Food preference matching
      ├── Seat availability
      ├── Driver availability
      ├── Restaurant type
      ├── Opening hours
      └── Restaurant statistics
      │
      ▼
Eligible Restaurant
```

---

### ⚠️ Incidents

Incidents represent changes to the simulation state that are applied during the simulation.

```text
Incident
│
├── StaffChangeIncident
├── RecipeChangeIncident
├── PackagingChangeIncident
└── UnavailabilityIncident
```

Each incident provides an `apply()` operation.

| Incident                  | Affected Component                |
| ------------------------- | --------------------------------- |
| `StaffChangeIncident`     | Restaurant staff                  |
| `RecipeChangeIncident`    | Recipe / ingredient configuration |
| `PackagingChangeIncident` | Ingredient packaging              |
| `UnavailabilityIncident`  | Ingredient stock availability     |

---

### 📝 Logging

Logging is separated from the simulation logic through specialized logger components.

```text
Logger
│
├── InitialAndPrepLogger
├── TickStatusLogger
├── FohReceptionLogger
├── FohServiceLogger
├── KitchenLogger
├── DeliveryLogger
└── StatisticsLogger
```

The loggers cover different stages of the simulation, including initialization, preparation, customer reception, serving, kitchen activity, deliveries, and final statistics.

---

### 🔢 Global Types and Enums

The architecture also contains shared global concepts used throughout the simulation.

Examples include:

* `LogLevel`
* `ExperienceType`
* `TableStatus`
* `DishStatus`
* `RestaurantType`
* `ActionType`
* `CookType`
* `DriverState`
* `TableType`
* `MeasurementUnit`
* `StaffType`

The global `Time` component tracks the current tick, evening, and maximum simulation ticks.

---

### 🔗 Component Relationships

At a high level, the main relationships between the components are:

```text
                         ┌─────────────┐
                         │    Main     │
                         └──────┬──────┘
                                │
                                ▼
                     ┌───────────────────┐
                     │ ParserController  │
                     └─────────┬─────────┘
                               │
             ┌─────────────────┼─────────────────┐
             ▼                 ▼                 ▼
       FoodParser       RestaurantParser   ScenarioParser
             │                 │                 │
             └─────────────────┼─────────────────┘
                               │
                               ▼
                     ┌───────────────────┐
                     │ SimulationConfig  │
                     └─────────┬─────────┘
                               │
                               ▼
                     ┌───────────────────┐
                     │    Simulation     │
                     └─────────┬─────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
        ┌───────────┐    ┌────────────┐   ┌────────────┐
        │ Restaurant│    │ Customers  │   │ Incidents  │
        └─────┬─────┘    └────────────┘   └────────────┘
              │
      ┌───────┼────────┬────────────┐
      │       │        │            │
      ▼       ▼        ▼            ▼
   Kitchen   FOH     Staff       Tables
      │       │
      │       ├── Waiters
      │       ├── Drivers
      │       └── Countertop
      │
      ├── Cooks
      └── Pantry
            │
            ├── Stock
            └── Supplier
```

This architecture keeps configuration parsing, simulation control, restaurant operations, customer behavior, food management, incidents, and logging separated into dedicated components while allowing them to interact through the simulation workflow.

---

## 📁 Project Structure

```text
src/
├── main/kotlin/de/unisaarland/cs/se/selab/
│   ├── Main.kt                    # Entry point & CLI argument parsing
│   ├── Types.kt                   # Type aliases (Tick, Evening, Id)
│   │
│   ├── actors/                    # Cook, Waiter, Driver, RestaurantStaff
│   ├── customer/                  # CustomerGroup, FoodPreference
│   ├── enums/                     # ActionType, CookType, CustomerStatus, etc.
│   ├── food/                      # Dish, Ingredient, Order, Recipe, Stock, Supplier
│   ├── incidents/                 # Staff, Recipe, Packaging, Unavailability incidents
│   ├── loggers/                   # Logger implementations and statistics
│   ├── parsers/                   # JSON parsers and ParserController
│   │
│   ├── restaurant/                # Restaurant and restaurant components
│   │   └── helpers/               # Arrival, delivery, eating, and other processors
│   │
│   └── system/                    # Simulation and SimulationConfig
│
├── main/resources/
│   └── schema/                    # JSON Schema files
│
├── test/kotlin/                   # Unit tests
│
└── systemtest/                    # System / integration tests
```

---

# 🚀 Getting Started

## Prerequisites

Make sure the following are installed:

| Requirement | Version                  |
| ----------- | ------------------------ |
| **JDK**     | 21+                      |
| **Gradle**  | 8.x *(wrapper included)* |

The project uses the Gradle wrapper, so a separate Gradle installation is normally not required.

---

## 🔨 Building

### Build the complete project

This runs the configured quality checks and tests and produces the project artifacts:

```bash
./gradlew build
```

### Build the JAR only

```bash
./gradlew jar
```

The resulting JAR is located at:

```text
build/libs/selab.jar
```

> **Note:** If your Gradle configuration explicitly copies or renames the JAR to `libs/selab.jar`, adjust the path above accordingly.

---

## ▶️ Running the Simulation

The simulation can be started using the generated JAR:

```bash
java -jar build/libs/selab.jar \
  --food <path/to/food.json> \
  --restaurants <path/to/restaurants.json> \
  --scenario <path/to/scenario.json> \
  --maxTicks <number> \
  --logLevel <DEBUG|INFO|IMPORTANT> \
  [--out <output-file>]
```

### Command-Line Arguments

| Argument        | Description                                    | Required |
| --------------- | ---------------------------------------------- | :------: |
| `--food`        | Path to the food configuration file            |     ✅    |
| `--restaurants` | Path to the restaurant configuration file      |     ✅    |
| `--scenario`    | Path to the scenario configuration file        |     ✅    |
| `--maxTicks`    | Maximum number of simulation ticks             |     ✅    |
| `--logLevel`    | Logging level: `DEBUG`, `INFO`, or `IMPORTANT` |     ✅    |
| `--out`         | Path to the output file                        |     ❌    |
| `--help`        | Displays usage information                     |     ❌    |

### Example

```bash
java -jar build/libs/selab.jar \
  --food resources/food.json \
  --restaurants resources/restaurants.json \
  --scenario resources/scenario.json \
  --maxTicks 1000 \
  --logLevel INFO
```

---

## ⚡ Quick Start

If the project is configured with example files in `gradle.properties`, the simulation can also be started with:

```bash
./gradlew serverExec
```

This uses the example configuration files defined by the Gradle project.

---

# ⚙️ Configuration Files

The simulation is driven by three JSON configuration files.

Each file is validated against its corresponding JSON Schema when the application starts.

| File               | Schema               | Contents                                                              |
| ------------------ | -------------------- | --------------------------------------------------------------------- |
| `food.json`        | `food.schema`        | Ingredients, suppliers, packaging information, and recipes            |
| `restaurants.json` | `restaurants.schema` | Restaurants, opening hours, staff, tables, menus, and initial ratings |
| `scenario.json`    | `scenario.schema`    | Customer groups, visit schedules, food preferences, and incidents     |

### `food.json`

Contains information about:

* Ingredients
* Measurement units
* Suppliers
* Ingredient packaging
* Recipes
* Recipe ingredients and quantities
* Cooking times

### `restaurants.json`

Defines each restaurant's:

* Restaurant type
* Opening hours
* Cooks
* Waiters
* Delivery drivers
* Tables
* Menu
* Initial ratings

### `scenario.json`

Defines:

* Customer groups
* Customer visit schedules
* Food preferences
* Restaurant-related incidents
* Staff changes
* Recipe changes
* Packaging changes
* Ingredient unavailability

If a configuration file is invalid or fails schema validation, the simulation logs the error and exits gracefully.

---

# 🔄 Simulation Lifecycle

Each simulation run is divided into evenings.

A simplified overview looks like this:

```text
Simulation Start
│
├── Evening 1
│   │
│   ├── Execute Incidents
│   │   └── Sorted by incident ID
│   │
│   ├── Preparation Phase
│   │   ├── Reserve tables for Event groups
│   │   ├── Reserve tables for Regular groups
│   │   ├── Plan ingredient procurement
│   │   └── Publish available seats & drivers
│   │       to BrowsingService
│   │
│   └── Serving Phase
│       │
│       ├── 24 ticks
│       │
│       └── Each tick:
│           ├── Event restaurant decisions
│           ├── Casual restaurant decisions
│           └── For each restaurant:
│               ├── Arrival
│               ├── Seating
│               ├── Ordering
│               ├── Cooking
│               ├── Serving
│               ├── Delivering
│               ├── Eating
│               ├── Escorting
│               └── Rating
│
├── Evening 2
│   └── ...
│
└── Simulation End
    │
    └── Statistics per restaurant
        ├── Meals cooked
        ├── Customers served
        ├── Customers delivered to
        └── Ratings given
```

---

# 🧑‍🤝‍🧑 Key Concepts

## Customer Groups

SaarLaCarte supports three different customer group types:

| Type        | Description                                                                         |
| ----------- | ----------------------------------------------------------------------------------- |
| **Regular** | Assigned to a specific restaurant and visits on fixed evenings                      |
| **Casual**  | Browses for an eligible restaurant for each visit and may dine in or order delivery |
| **Event**   | Books a restaurant three evenings in advance and receives dedicated event seating   |

---

## 🍳 Restaurant Components

### Kitchen

The kitchen:

* Assigns cooks to orders based on qualification hierarchies.
* Handles cooking processes tick-by-tick.
* Batches identical dishes where possible.
* Tracks cooking progress.
* Coordinates with the countertop and pantry.

### Pantry

The pantry:

* Maintains ingredient stock.
* Handles supplier deliveries.
* Tracks ingredient quantities.
* Handles ingredient reservations.
* Tracks ingredient expiry.

### Front of House (FOH)

The front-of-house subsystem coordinates:

* Customer seating
* Table merging
* Waiter assignments
* Serving
* Escorting
* Delivery dispatch

### Countertop

The countertop acts as the bridge between the kitchen and front of house.

It manages:

* Incoming orders
* Order queues
* Ingredient reservations
* Communication between FOH and kitchen

### BrowsingService

The `BrowsingService` matches customer groups with eligible restaurants based on factors such as:

* Available seating
* Available delivery drivers
* Dietary compatibility
* Restaurant availability
* Restaurant ratings

---

# ⚠️ Incidents

Incidents are scheduled changes that are executed at the beginning of an evening.

| Incident Type               | Effect                                                        |
| --------------------------- | ------------------------------------------------------------- |
| **StaffChangeIncident**     | Adds or removes cooks, waiters, or drivers                    |
| **RecipeChangeIncident**    | Changes recipe properties such as cooking time or ingredients |
| **PackagingChangeIncident** | Changes ingredient packaging volumes                          |
| **UnavailabilityIncident**  | Makes ingredients temporarily unavailable                     |

Incidents allow scenarios to model unexpected changes and disruptions during the simulation.

---

# 🧪 Testing

The project contains both unit tests and end-to-end system tests.

## Unit Tests

Run the complete unit test suite with:

```bash
./gradlew test
```

The unit tests cover major components including:

* Browsing
* Cooking
* Customer behavior
* Delivery
* Escorting
* Incidents
* Kitchen scheduling
* Ordering
* Parsing
* Ratings
* Reservations
* Seating
* Serving

There are **50+ unit test suites** covering the core functionality of the simulator.

---

## System Tests

System tests provide end-to-end validation using JSON scenario fixtures:

```bash
./gradlew systemtestExec
```

The system test suite contains **130+ scenario fixtures** located under:

```text
src/systemtest/resources/
```

These scenarios exercise the complete simulation pipeline and are validated against the SeLab system test API.

---

## 📊 Code Coverage

JaCoCo is configured to generate test coverage reports.

Run:

```bash
./gradlew jacocoTestReport
```

The generated reports can be found under:

```text
build/reports/jacoco/
```

---

# 🧹 Code Quality

The project uses **Detekt** for static analysis of the Kotlin codebase.

Detekt is configured through:

```text
config/detekt/detekt.yml
```

Static analysis is applied to:

* `main`
* `test`
* `systemtest`

source sets as part of the Gradle build.

### Suppression Detection

The project also contains a custom Gradle task called `detectSuppressions`.

This task scans the source sets for `@Suppress` annotations and fails the build if any are detected.

This ensures that static-analysis rules cannot be silently bypassed through suppression annotations.

---

# 🛠️ Tech Stack

| Technology                     | Purpose                       |
| ------------------------------ | ----------------------------- |
| **Kotlin 2.1.21**              | Programming language          |
| **Eclipse Temurin 21.0.12**    | JVM / runtime                 |
| **Gradle (Kotlin DSL)**        | Build system                  |
| **kotlinx-serialization-json** | JSON parsing                  |
| **json-sKema**                 | JSON Schema validation        |
| **kotlinx-cli**                | Command-line argument parsing |
| **SLF4J + kotlin-logging**     | Logging                       |
| **JUnit 5**                    | Unit testing                  |
| **Mockito**                    | Mocking in tests              |
| **JaCoCo**                     | Code coverage                 |
| **Detekt**                     | Static analysis               |

---

## 📌 Summary

SaarLaCarte combines restaurant management, customer behavior, kitchen workflows, inventory management, delivery logistics, and event-driven simulation into a single configurable system.

The simulation is fully driven by JSON configuration and is designed to support extensive automated testing through both unit and system-level test suites.

---

<p align="center">
  <i>Built with ☕ by Group 10 — Software Engineering Lab, Saarland University</i>
</p>
