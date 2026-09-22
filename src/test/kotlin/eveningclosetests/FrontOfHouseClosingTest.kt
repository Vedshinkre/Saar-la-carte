package eveningclosetests

import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.DishStatus
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.enums.ExperienceType
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.loggers.Logger
import eveningclosetests.EveningCloseFixtures.casualGroup
import eveningclosetests.EveningCloseFixtures.deliveringDriver
import eveningclosetests.EveningCloseFixtures.eatenOrder
import eveningclosetests.EveningCloseFixtures.eventGroup
import eveningclosetests.EveningCloseFixtures.frontOfHouse
import eveningclosetests.EveningCloseFixtures.regularGroup
import eveningclosetests.EveningCloseFixtures.returningDriver
import eveningclosetests.EveningCloseFixtures.slowMenu
import eveningclosetests.EveningCloseFixtures.table
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * F30 unit tests driving FrontOfHouse directly (no Restaurant/Kitchen), to reach
 * closing-time branches a full tick-by-tick simulation can't isolate.
 */
class FrontOfHouseClosingTest {

    @BeforeTest
    fun setUp() {
        Logger.setup(LogLevel.DEBUG)
        Logger.restaurantID = 1
        Time.evening = 1
        Time.tick = 1
    }

    @AfterTest
    fun tearDown() {
        Time.tick = 1
        Time.evening = 1
    }

    @Test
    fun `startFohClosing keeps a finished group's experience but forces negative for a mid-meal or order-less one`() {
        val foh = frontOfHouse(tables = listOf(table(1), table(2), table(3)))
        val finished = regularGroup(id = 1)
        val midMeal = regularGroup(id = 2)
        val noOrder = regularGroup(id = 3)
        listOf(finished, midMeal, noOrder).forEach {
            foh.reserveTables(it)
            foh.processArrival(it, slowMenu)
        }
        finished.currentOrder = eatenOrder()
        finished.experience = ExperienceType.POSITIVE
        noOrder.currentOrder = null

        foh.startFohClosing()

        assertEquals(ExperienceType.POSITIVE, finished.experience)
        assertEquals(ExperienceType.NEGATIVE, midMeal.experience)
        assertEquals(ExperienceType.NEGATIVE, noOrder.experience)
        listOf(finished, midMeal, noOrder).forEach { assertEquals(0, it.customersRemainingInRestaurant) }
    }

    @Test
    fun `startFohClosing also force-escorts a mid-meal EVENT group`() {
        val foh = frontOfHouse(tables = listOf(table(1)))
        val group = eventGroup(id = 1)
        foh.reserveTables(group)
        foh.processArrival(group, slowMenu)

        foh.startFohClosing()

        assertEquals(0, group.customersRemainingInRestaurant)
        assertEquals(ExperienceType.NEGATIVE, group.experience)
    }

    @Test
    fun `a dine-in CASUAL group mid-meal is force-escorted, a delivery CASUAL group is untouched`() {
        val dineIn = casualGroup(id = 1)
        val delivery = casualGroup(id = 2, deliveryDistance = 5)
        val foh = frontOfHouse(tables = listOf(table(1)))
        foh.processArrival(dineIn, slowMenu)
        foh.processArrival(delivery, slowMenu)

        foh.startFohClosing()

        assertEquals(0, dineIn.customersRemainingInRestaurant)
        assertEquals(ExperienceType.NEGATIVE, dineIn.experience)
        assertEquals(
            2,
            delivery.customersRemainingInRestaurant,
            "a delivery group never sat down, closing must not touch it"
        )
        assertEquals(ExperienceType.NEUTRAL, delivery.experience)
    }

    @Test
    fun `a reservation for a group that never arrives is still freed by endFohOpeningTime`() {
        val sharedTable = table(1)
        val foh = frontOfHouse(tables = listOf(sharedTable))
        foh.reserveTables(regularGroup(id = 1))
        assertEquals(TableStatus.RESERVED, sharedTable.status)

        foh.endFohOpeningTime()

        assertEquals(TableStatus.FREE, sharedTable.status)
    }

    @Test
    fun `resetDrivers keeps a RETURNING driver's id alive so the next evening's ids continue above it`() {
        val survivor = returningDriver(id = 8, ticksToDest = 5)
        val aborted = deliveringDriver(id = 3)
        val foh = frontOfHouse(tables = listOf(table(1)), drivers = listOf(survivor, aborted))

        foh.resetDrivers()
        assertNull(aborted.id)
        assertEquals(DriverState.IDLE, aborted.state)
        assertEquals(8, survivor.id, "a still-RETURNING driver must keep its evening id")

        val delivery = casualGroup(id = 1, deliveryDistance = 5)
        foh.processArrival(delivery, slowMenu)
        checkNotNull(delivery.currentOrder).dishes.forEach { it.status = DishStatus.COOKED }
        foh.processServing()

        assertEquals(9, aborted.id, "the counter must skip past the surviving driver's id 8, not restart at 1")
    }
}
