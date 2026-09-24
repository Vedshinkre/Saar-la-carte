package ratingtests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.RatingType
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Order
import de.unisaarland.cs.se.selab.food.Recipe
import de.unisaarland.cs.se.selab.loggers.Logger
import de.unisaarland.cs.se.selab.restaurant.FrontOfHouse
import de.unisaarland.cs.se.selab.restaurant.Table
import de.unisaarland.cs.se.selab.restaurant.helpers.RatingProcessor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.collections.emptyList
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**Regular Event Group Rating Test P 05
 * RegularEventGroupRatingTest validates rating processing for RegularGroup and EventGroup via RatingProcessor
 * and FrontOfHouse, verifying negative ratings triggered by insufficient table capacity during reservations,
 * seating failures due to unavailable waitstaff, food service timeouts, and unfinished orders at closing.
 */
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

    // Insufficient COMMON tables par EventGroup generates negative ratings
    @Test
    fun `EventGroup fails reservation due to insufficient tables and receives a NEGATIVE rating COMMON`() {
        val massiveEventGroup = createEventGroup(10, TableType.COMMON)
        val tinyTable = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(tinyTable))

        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        val reservationSuccess = foh.reserveTables(massiveEventGroup)

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

        assertFalse(reservationSuccess, "The reservation should fail because the table is too small")
        assertEquals(ExperienceType.NEGATIVE, massiveEventGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, massiveEventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive)
        assertEquals(6, updatedNegative)
    }

    // Insufficient SEPARATED tables par EventGroup generates negative rating
    @Test
    fun `EventGroup fails reservation due to insufficient tables and receives a NEGATIVE rating SEPARATED`() {
        val massiveEventGroup = createEventGroup(10, TableType.SEPARATED)
        val tinyTable = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(tinyTable))

        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        val reservationSuccess = foh.reserveTables(massiveEventGroup)

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

        assertFalse(reservationSuccess, "The reservation should fail because the table is too small")
        assertEquals(ExperienceType.NEGATIVE, massiveEventGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, massiveEventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive)
        assertEquals(6, updatedNegative)
    }

    // Insufficient BAR tables par EventGroup generates negative ratings
    @Test
    fun `EventGroup fails reservation due to insufficient tables and receives a NEGATIVE rating BAR`() {
        val massiveEventGroup = createEventGroup(10, TableType.BAR)
        val tinyTable = Table(id = 1, size = 2, tableType = TableType.BAR)
        val foh = setupFoh(listOf(tinyTable))

        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        val reservationSuccess = foh.reserveTables(massiveEventGroup)

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

        assertFalse(reservationSuccess, "The reservation should fail because the table is too small")
        assertEquals(ExperienceType.NEGATIVE, massiveEventGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, massiveEventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive)
        assertEquals(6, updatedNegative)
    }

    // Insufficient COMMON tables par RegularGroup geenrates negative ratings
    @Test
    fun `RegularGroup fails reservation due to insufficient tables and receives a NEGATIVE rating COMMON`() {
        val massiveEventGroup = createRegularGroup(10, TableType.COMMON)
        val tinyTable = Table(id = 1, size = 2, tableType = TableType.COMMON)
        val foh = setupFoh(listOf(tinyTable))

        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        val reservationSuccess = foh.reserveTables(massiveEventGroup)

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

        assertFalse(reservationSuccess, "The reservation should fail because the table is too small")
        assertEquals(ExperienceType.NEGATIVE, massiveEventGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, massiveEventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive)
        assertEquals(6, updatedNegative)
    }

    // Insufficient SEPARATED tables par RegularGroup generates negative ratings
    @Test
    fun `RegularGroup fails reservation due to insufficient tables and receives a NEGATIVE rating SEPARATED`() {
        val massiveEventGroup = createRegularGroup(10, TableType.SEPARATED)
        val tinyTable = Table(id = 1, size = 2, tableType = TableType.SEPARATED)
        val foh = setupFoh(listOf(tinyTable))

        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        val reservationSuccess = foh.reserveTables(massiveEventGroup)

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

        assertFalse(reservationSuccess, "The reservation should fail because the table is too small")
        assertEquals(ExperienceType.NEGATIVE, massiveEventGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, massiveEventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive)
        assertEquals(6, updatedNegative)
    }

    // Insufficient BAR tables par RegularGroup generates negative ratings
    @Test
    fun `RegularGroup fails reservation due to insufficient tables and receives a NEGATIVE rating BAR`() {
        val massiveEventGroup = createRegularGroup(10, TableType.BAR)
        val tinyTable = Table(id = 1, size = 2, tableType = TableType.BAR)
        val foh = setupFoh(listOf(tinyTable))

        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        val reservationSuccess = foh.reserveTables(massiveEventGroup)

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

        assertFalse(reservationSuccess, "The reservation should fail because the table is too small")
        assertEquals(ExperienceType.NEGATIVE, massiveEventGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, massiveEventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive)
        assertEquals(6, updatedNegative)
    }

    // lack of Waitstaff for RegularGroup fails seating and generates negative rating
    @Test
    fun `RegularGroup fails seating due to no waitstaff, gets negative experience, and increases negative ratings`() {
        val regularGroup = createRegularGroup(4, TableType.COMMON)

        val validTable = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val foh = FrontOfHouse(
            tables = listOf(validTable),
            waiters = emptyList(),
            drivers = emptyList(),
            countertop = mock()
        )

        val initialPositiveRatings = 10
        val initialNegativeRatings = 5

        val menu = emptyList<Recipe>()
        val seatingSuccess = foh.processArrival(regularGroup, menu)

        if (!seatingSuccess) {
            regularGroup.experience = ExperienceType.NEGATIVE
        }

        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = regularGroup,
            positiveRatings = initialPositiveRatings,
            negativeRatings = initialNegativeRatings,
            closing = false
        )

        assertFalse(seatingSuccess, "Seating should fail because there are no waitstaff")
        assertEquals(ExperienceType.NEGATIVE, regularGroup.experience, "Experience must update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, regularGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive, "Positive ratings should remain unchanged")
        assertEquals(6, updatedNegative, "Negative ratings should increase by 1")
    }

    // lack of Waitstaff for EventGroup fails seating and generates negative rating    @Test
    fun `EventGroup fails seating due to no waitstaff, gets negative experience, and increases negative ratings`() {
        val eventGroup = createEventGroup(4, TableType.COMMON)

        val validTable = Table(id = 1, size = 4, tableType = TableType.COMMON)
        val foh = FrontOfHouse(
            tables = listOf(validTable),
            waiters = emptyList(),
            drivers = emptyList(),
            countertop = mock()
        )

        val initialPositiveRatings = 10
        val initialNegativeRatings = 5
        val menu = emptyList<Recipe>()

        val isProcessed = foh.processArrival(eventGroup, menu)

        val (updatedPositive, updatedNegative) = foh.processRatings(
            positiveRatings = initialPositiveRatings,
            negativeRatings = initialNegativeRatings
        )

        assertTrue(isProcessed, "EventGroup should be removed from queue after failing to seat")
        assertEquals(ExperienceType.NEGATIVE, eventGroup.experience, "Experience must automatically update to NEGATIVE")
        assertEquals(RatingType.NEGATIVE, eventGroup.determineRating(), "Group must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive, "Positive ratings should remain unchanged")
        assertEquals(6, updatedNegative, "Negative ratings should increase by exactly 1")
    }

    // Negative ratings if the food is late or did not appear at all
    @Test
    fun `RegularGroup gets negative experience and rating when food arrives too late or not at all`() {
        val regularGroup = createRegularGroup(4, TableType.COMMON)
        val initialPositive = 10
        val initialNegative = 5

        regularGroup.experience = ExperienceType.NEGATIVE

        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = regularGroup,
            positiveRatings = initialPositive,
            negativeRatings = initialNegative,
            closing = false
        )

        assertEquals(ExperienceType.NEGATIVE, regularGroup.experience, "Experience must be negative due to food delay")
        assertEquals(RatingType.NEGATIVE, regularGroup.determineRating(), "Group must generate a negative rating")
        assertEquals(10, updatedPositive, "Positive ratings should remain unchanged")
        assertEquals(6, updatedNegative, "Negative ratings should increase by 1")
    }

    // negative ratings from the event at service timeout(end of evening) for event groups
    @Test
    fun `EventGroup gets negative rating due to service timeout`() {
        val eventGroup = createEventGroup(4, TableType.COMMON)
        val initialPositive = 20
        val initialNegative = 2

        eventGroup.experience = ExperienceType.NEGATIVE

        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = eventGroup,
            positiveRatings = initialPositive,
            negativeRatings = initialNegative,
            closing = false
        )

        assertEquals(ExperienceType.NEGATIVE, eventGroup.experience)
        assertEquals(RatingType.NEGATIVE, eventGroup.determineRating())
        assertEquals(20, updatedPositive)
        assertEquals(3, updatedNegative, "Negative ratings should increase due to timeout")
    }

    // negative ratings from the event at service timeout(end of evening) for regular groups
    @Test
    fun `RegularGroup at closing with no order receives NEGATIVE experience`() {
        val regularGroup = createRegularGroup(2, TableType.COMMON)
        val initialPositive = 10
        val initialNegative = 5

        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = regularGroup,
            positiveRatings = initialPositive,
            negativeRatings = initialNegative,
            closing = true
        )

        assertEquals(ExperienceType.NEGATIVE, regularGroup.experience, "negative stuff")
        assertEquals(RatingType.NEGATIVE, regularGroup.determineRating(), "Must generate a NEGATIVE rating")
        assertEquals(10, updatedPositive)
        assertEquals(6, updatedNegative)
    }

// negative ratings for closing of evening and order is  incomplete
    @Test
    fun `EventGroup at closing with unfinished order receives NEGATIVE experience`() {
        val eventGroup = mock<EventGroup>()
        val initialPositive = 10
        val initialNegative = 5

        val mockOrder = mock<Order>()

        whenever(mockOrder.areAllDishesEaten()).thenReturn(false)
        whenever(eventGroup.currentOrder).thenReturn(mockOrder)
        whenever(eventGroup.determineRating()).thenReturn(RatingType.NEGATIVE)

        val processor = createProcessor()
        val (updatedPositive, updatedNegative) = processor.rate(
            group = eventGroup,
            positiveRatings = initialPositive,
            negativeRatings = initialNegative,
            closing = true
        )

        verify(eventGroup).experience = ExperienceType.NEGATIVE
        assertEquals(10, updatedPositive)
        assertEquals(6, updatedNegative)
    }

    // food was finished eating at the end of the evening and we register positive rating
    @Test
    fun `RegularGroup at closing with completely eaten order avoids NEGATIVE experience penalty`() {
        val regularGroup = mock<RegularGroup>()
        val mockOrder = mock<Order>()

        whenever(mockOrder.areAllDishesEaten()).thenReturn(true)
        whenever(regularGroup.currentOrder).thenReturn(mockOrder)
        whenever(regularGroup.determineRating()).thenReturn(RatingType.POSITIVE)

        val processor = createProcessor()
        processor.rate(
            group = regularGroup,
            positiveRatings = 10,
            negativeRatings = 5,
            closing = true
        )

        verify(regularGroup, org.mockito.kotlin.never()).experience = ExperienceType.NEGATIVE
    }
}
