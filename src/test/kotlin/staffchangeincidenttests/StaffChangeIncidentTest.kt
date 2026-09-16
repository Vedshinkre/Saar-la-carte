package staffchangeincidenttests

import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.actors.Driver
import de.unisaarland.cs.se.selab.actors.RestaurantStaff
import de.unisaarland.cs.se.selab.actors.Waiter
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.enums.StaffType
import de.unisaarland.cs.se.selab.incidents.StaffChangeIncident
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StaffChangeIncidentTest {

    //  Helpers
    private fun createDummyStaff(): RestaurantStaff {
        // Start with 2 Cooks (1 TOURNANT, 1 ROAST), 2 Waiters, 1 Driver
        val cooks = mutableListOf(Cook(CookType.TOURNANT), Cook(CookType.ROAST))
        val waiters = mutableListOf(Waiter(), Waiter())
        val drivers = mutableListOf(Driver())
        return RestaurantStaff(cooks, waiters, drivers)
    }

    private fun createIncident(
        number: Int,
        staffType: StaffType,
        cookType: CookType?,
        staff: RestaurantStaff
    ): StaffChangeIncident {
        return StaffChangeIncident(
            id = 1,
            evening = 1,
            number = number,
            staffType = staffType,
            cookType = cookType,
            restaurantStaff = staff
        )
    }

    @Test
    fun `apply - Add Cooks - Increases Cook List`() {
        val staff = createDummyStaff()
        // Add 2 TOURNANT cooks
        val incident = createIncident(2, StaffType.COOK, CookType.TOURNANT, staff)

        incident.apply()

        // Originally 2, added 2 = 4 total
        assertEquals(4, staff.cooks.size)
        // Verify the newly added cook is of the correct type
        val tournantCount = staff.cooks.count { it.type == CookType.TOURNANT }
        assertEquals(3, tournantCount)
    }

    @Test
    fun `apply - Remove Cooks - Decreases Cook List`() {
        val staff = createDummyStaff()
        // Remove 1 TOURNANT cook
        val incident = createIncident(-1, StaffType.COOK, CookType.TOURNANT, staff)

        incident.apply()

        // Originally 2, removed 1 = 1 total
        assertEquals(1, staff.cooks.size)
        // Verify that no tournant COOK IS LEFT
        val tournantCount = staff.cooks.count { it.type == CookType.TOURNANT }
        assertEquals(0, tournantCount)
    }

    @Test
    fun `apply - Add Waitstaff - Increases Waiter List`() {
        val staff = createDummyStaff()
        val incident = createIncident(3, StaffType.WAITSTAFF, null, staff)

        incident.apply()

        // Originally 2, added 3 = 5 total
        assertEquals(5, staff.waiters.size)
    }

    @Test
    fun `apply - Remove Waitstaff - Decreases Waiter List`() {
        val staff = createDummyStaff()
        val incident = createIncident(-1, StaffType.WAITSTAFF, null, staff)

        incident.apply()

        // Originally 2, removed 1 = 1 total
        assertEquals(1, staff.waiters.size)
    }

    @Test
    fun `apply - Remove More Waitstaff Than Available - Bounds at Zero`() {
        val staff = createDummyStaff()
        // Try to remove 5 waiters, but we only have 2
        val incident = createIncident(-5, StaffType.WAITSTAFF, null, staff)

        incident.apply()

        // List should not go negative, it should just safely clear
        assertEquals(0, staff.waiters.size)
    }

    @Test
    fun `apply - Add Drivers - Increases Driver List`() {
        val staff = createDummyStaff()
        val incident = createIncident(2, StaffType.DRIVER, null, staff)

        incident.apply()

        // Originally 1, added 2 = 3 total
        assertEquals(3, staff.drivers.size)
    }

    @Test
    fun `apply - Remove Drivers - Decreases Driver List`() {
        val staff = createDummyStaff()
        val incident = createIncident(-1, StaffType.DRIVER, null, staff)

        incident.apply()

        // Originally 1, removed 1 = 0 total
        assertEquals(0, staff.drivers.size)
    }
}
