package ratingtests
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingType
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.RatingProcessor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import kotlin.collections.emptyList
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegularEventGroupRatingTest {

    // Helper method to create a dummy EventGroup
    private fun createEventGroup(size: Int, tableType: TableType): EventGroup {
        return EventGroup(
            id = 1,
            size = size,
            tableType = tableType,
            visitingAt = 1,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            eventEvening = 4,
            eventDishes = mapOf(RestaurantType.EUROPEAN to "Pizza")
        )
    }

    // Helper method to create a dummy RegularGroup
    private fun createRegularGroup(size: Int, tableType: TableType): RegularGroup {
        return RegularGroup(
            id = 2,
            size = size,
            tableType = tableType,
            visitingAt = 1,
            foodPreferences = emptyList(),
            visitingStart = 1,
            visitingPeriod = 2,
            restaurantId = 1
        )
    }

    private fun createProcessor(): RatingProcessor {
        return RatingProcessor(
            deliveryGroups = mutableListOf(),
            turnedAwayGroups = emptyList(),
            eventGroups = mutableListOf(),
            getInHouseGroups = { emptyList() },
            getServingPriority = { 0 },
            removeProcessedGroup = {}
        )
    }
    private fun setupFoh(tables: List<Table>): FrontOfHouse {
        return FrontOfHouse(
            tables = tables,
            waiters = emptyList(),
            drivers = emptyList(),
            countertop = mock()
        )
    }

    @BeforeEach
    fun setup() {
        // Reset time to prevent errors
        Time.tick = 1

        // Provide a default LogLevel so the logger doesn't crash
        Logger.setup(LogLevel.DEBUG)
    }
    // TABLE SIZE NOT MATCHING - REJECTED

    @Test
    fun `EventGroup fails reservation due to insufficient tables and receives a NEGATIVE rating COMMON`() {
        //  Setup: A group of 10 people, but the restaurant only has a table for 2
        val massiveEventGroup = createEventGroup(10, TableType.COMMON)
        val tinyTable = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(tinyTable))

        // Initial restaurant ratings before the group rates
        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        // The manager attempts to reserve tables during the preparation phase
        val reservationSuccess = foh.reserveTables(massiveEventGroup)

        // Integration Logic: In your simulation, if reservation fails, the group is turned away
        if (!reservationSuccess) {
            massiveEventGroup.experience = ExperienceType.NEGATIVE
        }
        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = massiveEventGroup,
            positiveRatings = initialPositiveRatings,
            negativeRatings = initialNegativeRatings,
            closing = false
        )

        // Assertions: Prove the cause and effect
        assertFalse(reservationSuccess, "The reservation should fail because the table is too small")
        assertEquals(ExperienceType.NEGATIVE, massiveEventGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, massiveEventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive) // Positive ratings should stay exactly the same
        assertEquals(6, updatedNegative) // Negative ratings should increase by exactly 1
    }

    @Test
    fun `EventGroup fails reservation due to insufficient tables and receives a NEGATIVE rating SEPARATED`() {
        //  Setup: A group of 10 people, but the restaurant only has a table for 2
        val massiveEventGroup = createEventGroup(10, TableType.SEPARATED)
        val tinyTable = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(tinyTable))

        // Initial restaurant ratings before the group rates
        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        // The manager attempts to reserve tables during the preparation phase
        val reservationSuccess = foh.reserveTables(massiveEventGroup)

        // Integration Logic: In your simulation, if reservation fails, the group is turned away
        if (!reservationSuccess) {
            massiveEventGroup.experience = ExperienceType.NEGATIVE
        }
        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = massiveEventGroup,
            positiveRatings = initialPositiveRatings,
            negativeRatings = initialNegativeRatings,
            closing = false
        )

        // Assertions: Prove the cause and effect
        assertFalse(reservationSuccess, "The reservation should fail because the table is too small")
        assertEquals(ExperienceType.NEGATIVE, massiveEventGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, massiveEventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive) // Positive ratings should stay exactly the same
        assertEquals(6, updatedNegative) // Negative ratings should increase by exactly 1
    }

    @Test
    fun `EventGroup fails reservation due to insufficient tables and receives a NEGATIVE rating BAR`() {
        //  Setup: A group of 10 people, but the restaurant only has a table for 2
        val massiveEventGroup = createEventGroup(10, TableType.BAR)
        val tinyTable = Table(id = 1, size = 2, tableType = TableType.BAR)
        val foh = setupFoh(listOf(tinyTable))

        // Initial restaurant ratings before the group rates
        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        // The manager attempts to reserve tables during the preparation phase
        val reservationSuccess = foh.reserveTables(massiveEventGroup)

        // Integration Logic: In your simulation, if reservation fails, the group is turned away
        if (!reservationSuccess) {
            massiveEventGroup.experience = ExperienceType.NEGATIVE
        }
        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = massiveEventGroup,
            positiveRatings = initialPositiveRatings,
            negativeRatings = initialNegativeRatings,
            closing = false
        )

        // Assertions: Prove the cause and effect
        assertFalse(reservationSuccess, "The reservation should fail because the table is too small")
        assertEquals(ExperienceType.NEGATIVE, massiveEventGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, massiveEventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive) // Positive ratings should stay exactly the same
        assertEquals(6, updatedNegative) // Negative ratings should increase by exactly 1
    }

    @Test
    fun `RegularGroup fails reservation due to insufficient tables and receives a NEGATIVE rating COMMON`() {
        //  Setup: A group of 10 people, but the restaurant only has a table for 2
        val massiveEventGroup = createRegularGroup(10, TableType.COMMON)
        val tinyTable = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(tinyTable))

        // Initial restaurant ratings before the group rates
        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        // The manager attempts to reserve tables during the preparation phase
        val reservationSuccess = foh.reserveTables(massiveEventGroup)

        // Integration Logic: In your simulation, if reservation fails, the group is turned away
        if (!reservationSuccess) {
            massiveEventGroup.experience = ExperienceType.NEGATIVE
        }
        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = massiveEventGroup,
            positiveRatings = initialPositiveRatings,
            negativeRatings = initialNegativeRatings,
            closing = false
        )

        // Assertions: Prove the cause and effect
        assertFalse(reservationSuccess, "The reservation should fail because the table is too small")
        assertEquals(ExperienceType.NEGATIVE, massiveEventGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, massiveEventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive) // Positive ratings should stay exactly the same
        assertEquals(6, updatedNegative) // Negative ratings should increase by exactly 1
    }

    @Test
    fun `RegularGroup fails reservation due to insufficient tables and receives a NEGATIVE rating SEPARATED`() {
        //  Setup: A group of 10 people, but the restaurant only has a table for 2
        val massiveEventGroup = createRegularGroup(10, TableType.SEPARATED)
        val tinyTable = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(tinyTable))

        // Initial restaurant ratings before the group rates
        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        // The manager attempts to reserve tables during the preparation phase
        val reservationSuccess = foh.reserveTables(massiveEventGroup)

        // Integration Logic: In your simulation, if reservation fails, the group is turned away
        if (!reservationSuccess) {
            massiveEventGroup.experience = ExperienceType.NEGATIVE
        }
        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = massiveEventGroup,
            positiveRatings = initialPositiveRatings,
            negativeRatings = initialNegativeRatings,
            closing = false
        )

        // Assertions: Prove the cause and effect
        assertFalse(reservationSuccess, "The reservation should fail because the table is too small")
        assertEquals(ExperienceType.NEGATIVE, massiveEventGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, massiveEventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive) // Positive ratings should stay exactly the same
        assertEquals(6, updatedNegative) // Negative ratings should increase by exactly 1
    }

    @Test
    fun `RegularGroup fails reservation due to insufficient tables and receives a NEGATIVE rating BAR`() {
        //  Setup: A group of 10 people, but the restaurant only has a table for 2
        val massiveEventGroup = createRegularGroup(10, TableType.BAR)
        val tinyTable = Table(id = 1, size = 2, tableType = TableType.BAR)
        val foh = setupFoh(listOf(tinyTable))

        // Initial restaurant ratings before the group rates
        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        // The manager attempts to reserve tables during the preparation phase
        val reservationSuccess = foh.reserveTables(massiveEventGroup)

        // Integration Logic: In your simulation, if reservation fails, the group is turned away
        if (!reservationSuccess) {
            massiveEventGroup.experience = ExperienceType.NEGATIVE
        }
        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = massiveEventGroup,
            positiveRatings = initialPositiveRatings,
            negativeRatings = initialNegativeRatings,
            closing = false
        )

        // Assertions: Prove the cause and effect
        assertFalse(reservationSuccess, "The reservation should fail because the table is too small")
        assertEquals(ExperienceType.NEGATIVE, massiveEventGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, massiveEventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive) // Positive ratings should stay exactly the same
        assertEquals(6, updatedNegative) // Negative ratings should increase by exactly 1
    }
// ------------------------------------------------------------------------------------------------

    @Test
    fun `RegularGroup fails seating due to no waitstaff, gets negative experience, and increases negative ratings`() {
        // A Regular group arrives, but the restaurant has ZERO waitstaff
        val regularGroup = createRegularGroup(4, TableType.COMMON)

        // We provide a valid table, but an empty list of waiters
        val validTable = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val foh = FrontOfHouse(
            tables = listOf(validTable),
            waiters = emptyList(),
            drivers = emptyList(),
            countertop = mock()
        )

        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        //  The FOH attempts to assign a waiter and seat the group
        val menu = emptyList<Recipe>()
        val seatingSuccess = foh.processArrival(regularGroup, menu)

        // In case there is no free waiter... an experience counts as negative
        if (!seatingSuccess) {
            regularGroup.experience = ExperienceType.NEGATIVE
        }

        //  Process the rating for the turned-away group
        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = regularGroup,
            positiveRatings = initialPositiveRatings,
            negativeRatings = initialNegativeRatings,
            closing = false
        )

        //  Prove the entire chain of events works
        assertFalse(seatingSuccess, "Seating should fail because there are no waitstaff")
        assertEquals(ExperienceType.NEGATIVE, regularGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, regularGroup.determineRating(), "Group must generate a NEGATIVE rating")
        // Prove the restaurant's rating counts took the hit
        assertEquals(10, updatedPositive, "Positive ratings should remain unchanged")
        assertEquals(6, updatedNegative, "Negative ratings should increase by 1")
    }

    @Test
    fun `EventGroup fails seating due to no waitstaff, gets negative experience, and increases negative ratings`() {
        // 1. Setup: An Event group arrives, but the restaurant has ZERO waitstaff
        val eventGroup = createEventGroup(4, TableType.COMMON)

        val validTable = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val foh = FrontOfHouse(
            tables = listOf(validTable),
            waiters = emptyList(), // <-- No waitstaff available!
            drivers = emptyList(),
            countertop = mock()
        )

        val initialPositiveRatings = 10
        val initialNegativeRatings = 5
        val menu = emptyList<Recipe>()

        //  The FOH attempts to assign a waiter and seat the group
        val isProcessed = foh.processArrival(eventGroup, menu)

        // At the end of the tick, the restaurant processes ratings
        // FOH already knows the group failed and left, so it will rate them automatically!
        val (updatedPositive, updatedNegative) = foh.processRatings(
            positiveRatings = initialPositiveRatings,
            negativeRatings = initialNegativeRatings
        )

        // Prove the entire chain of events works exactly per specification

        //  The group is completely processed (removed from queue)
        // because Event groups leave immediately if seating fails
        assertTrue(isProcessed, "EventGroup should be removed from queue after failing to seat")

        // The system must have automatically updated their experience to NEGATIVE
        assertEquals(ExperienceType.NEGATIVE, eventGroup.experience, "Experience must automatically update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, eventGroup.determineRating(), "Group must generate a NEGATIVE rating")

        // The restaurant's global rating counts must take the hit
        assertEquals(10, updatedPositive, "Positive ratings should remain unchanged")
        assertEquals(6, updatedNegative, "Negative ratings should increase by exactly 1")
    }

    @Test
    fun `RegularGroup gets negative experience and rating when food arrives too late or not at all`() {
        val regularGroup = createRegularGroup(4, TableType.COMMON)
        val initialPositive = 10
        val initialNegative = 5

        // Simulate the specification rule: food arrives too late or not at all
        // leading to a negative experience
        regularGroup.experience = ExperienceType.NEGATIVE

        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = regularGroup,
            positiveRatings = initialPositive,
            negativeRatings = initialNegative,
            closing = false
        )

        // Assertions
        assertEquals(ExperienceType.NEGATIVE, regularGroup.experience, "Experience must be negative due to food delay")
        assertEquals(RatingType.NEGATIVE, regularGroup.determineRating(), "Group must generate a negative rating")
        assertEquals(10, updatedPositive, "Positive ratings should remain unchanged")
        assertEquals(6, updatedNegative, "Negative ratings should increase by 1")
    }

    @Test
    fun `EventGroup gets negative rating due to delivery or service timeout`() {
        val eventGroup = createEventGroup(4, TableType.COMMON)
        val initialPositive = 20
        val initialNegative = 2

        // Simulate a timeout where the event group experiences a delay
        eventGroup.experience = ExperienceType.NEGATIVE

        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = eventGroup,
            positiveRatings = initialPositive,
            negativeRatings = initialNegative,
            closing = false
        )

        // Assertions
        assertEquals(ExperienceType.NEGATIVE, eventGroup.experience)
        assertEquals(RatingType.NEGATIVE, eventGroup.determineRating())
        assertEquals(20, updatedPositive)
        assertEquals(3, updatedNegative, "Negative ratings should increase due to timeout")
    }
}
