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

/** Staff Change Incident Test f 31
 * StaffChangeIncidentTest validates StaffChangeIncident, verifying the complete staff group (Cook, Waiter, Driver)
 * correctly across positive and negative counts, cook adjustments filter strictly by CookType,
 * over-removal safely bounds at zero, and missing cook types abort without any effects.
 */
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

    //  applying a staff change adds the specified number of TOURNANT cooks to the staff list
    @Test
    fun `apply - Add Cooks - Increases Cook List-TOURNANT`() {
        val staff = createDummyStaff()

        val incident = createIncident(2, StaffType.COOK, CookType.TOURNANT, staff)

        incident.apply()

        // Originally 2, added 2 = 4 total
        assertEquals(4, staff.cooks.size)
        // Verify the newly added cook is of the correct type
        val tournantCount = staff.cooks.count { it.type == CookType.TOURNANT }
        assertEquals(3, tournantCount)
    }

    //  applying a staff change adds the specified number of SOUS cooks to the staff list
    @Test
    fun `apply - Add Cooks - Increases Cook List-SOUS`() {
        val staff = createDummyStaff()

        val incident = createIncident(3, StaffType.COOK, CookType.SOUS, staff)

        incident.apply()

        // Originally 2, added 3 = 5 total
        assertEquals(5, staff.cooks.size)
        // Verify the newly added cook is of the correct type
        val tournantCount = staff.cooks.count { it.type == CookType.SOUS }
        assertEquals(3, tournantCount)
    }

    //  applying a staff change adds the specified number of EXEC cooks to the staff list
    @Test
    fun `apply - Add Cooks - Increases Cook List-EXEC`() {
        val staff = createDummyStaff()

        val incident = createIncident(1, StaffType.COOK, CookType.EXEC, staff)

        incident.apply()

        // Originally 2, added 1 = 3 total
        assertEquals(3, staff.cooks.size)
        // Verify the newly added cook is of the correct type
        val tournantCount = staff.cooks.count { it.type == CookType.EXEC }
        assertEquals(1, tournantCount)
    }

    //  applying a staff change adds the specified number of SAUCE cooks to the staff list
    @Test
    fun `apply - Add Cooks - Increases Cook List-SAUCE`() {
        val staff = createDummyStaff()

        val incident = createIncident(4, StaffType.COOK, CookType.SAUCE, staff)

        incident.apply()

        // Originally 2, added 4 = 6 total
        assertEquals(6, staff.cooks.size)
        // Verify the newly added cook is of the correct type
        val tournantCount = staff.cooks.count { it.type == CookType.SAUCE }
        assertEquals(4, tournantCount)
    }

    //  applying a staff change adds the specified number of FISH cooks to the staff list
    @Test
    fun `apply - Add Cooks - Increases Cook List-FISH`() {
        val staff = createDummyStaff()

        val incident = createIncident(2, StaffType.COOK, CookType.FISH, staff)

        incident.apply()

        // Originally 2, added 2 = 4 total
        assertEquals(4, staff.cooks.size)
        // Verify the newly added cook is of the correct type
        val tournantCount = staff.cooks.count { it.type == CookType.FISH }
        assertEquals(2, tournantCount)
    }

    // applying a staff change adds the specified number of ROAST cooks to the staff list
    @Test
    fun `apply - Add Cooks - Increases Cook List-ROAST`() {
        val staff = createDummyStaff()

        val incident = createIncident(1, StaffType.COOK, CookType.ROAST, staff)

        incident.apply()

        // Originally 2, added =1 = 2 total
        assertEquals(3, staff.cooks.size)
        // Verify the newly added cook is of the correct type
        val tournantCount = staff.cooks.count { it.type == CookType.ROAST }
        assertEquals(2, tournantCount)
    }

    //  applying a staff change adds the specified number of VEGETABLE cooks to the staff list
    @Test
    fun `apply - Add Cooks - Increases Cook List-VEETABLE`() {
        val staff = createDummyStaff()

        val incident = createIncident(1, StaffType.COOK, CookType.VEGETABLE, staff)

        incident.apply()

        // Originally 2, added =1 = 2 total
        assertEquals(3, staff.cooks.size)
        // Verify the newly added cook is of the correct type
        val tournantCount = staff.cooks.count { it.type == CookType.VEGETABLE }
        assertEquals(1, tournantCount)
    }

    // applying a staff change adds the specified number of PASTRY cooks to the staff list
    @Test
    fun `apply - Add Cooks - Increases Cook List-PASTRY`() {
        val staff = createDummyStaff()

        val incident = createIncident(1, StaffType.COOK, CookType.PASTRY, staff)

        incident.apply()

        // Originally 2, added =1 = 2 total
        assertEquals(3, staff.cooks.size)
        // Verify the newly added cook is of the correct type
        val tournantCount = staff.cooks.count { it.type == CookType.PASTRY }
        assertEquals(1, tournantCount)
    }

    //  applying a negative count staff change removes the specified number of TOURNANT cooks
    @Test
    fun `apply - Remove Cooks - Decreases Cook List- TOURNANT`() {
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

    // applying a negative count staff change removes the specified number of SOUS cooks
    @Test
    fun `apply - Remove Cooks - Decreases Cook List- SOUS`() {
        val staff = createDummyStaff()
        staff.cooks.addLast(Cook(CookType.SOUS))
        staff.cooks.addLast(Cook(CookType.SOUS))

        // Remove 1 Sous cook
        val incident = createIncident(-1, StaffType.COOK, CookType.SOUS, staff)

        incident.apply()

        // Originally 4, removed 1 = 3 total
        assertEquals(3, staff.cooks.size)
        // Verify that no tournant COOK IS LEFT
        val tournantCount = staff.cooks.count { it.type == CookType.SOUS }
        assertEquals(1, tournantCount)
    }

    //  applying a negative count staff change removes the specified number of SAUCE cooks
    @Test
    fun `apply - Remove Cooks - Decreases Cook List- SAUCE`() {
        val staff = createDummyStaff()
        staff.cooks.addLast(Cook(CookType.SAUCE))
        staff.cooks.addLast(Cook(CookType.SAUCE))

        // Remove 1 SAUCE cook
        val incident = createIncident(-1, StaffType.COOK, CookType.SAUCE, staff)

        incident.apply()

        // Originally 4, removed 1 = 3 total
        assertEquals(3, staff.cooks.size)
        // Verify that no tournant COOK IS LEFT
        val tournantCount = staff.cooks.count { it.type == CookType.SAUCE }
        assertEquals(1, tournantCount)
    }

    //  applying a negative count staff change removes the specified number of FISH cooks
    @Test
    fun `apply - Remove Cooks - Decreases Cook List- FISH`() {
        val staff = createDummyStaff()
        staff.cooks.addLast(Cook(CookType.FISH))
        staff.cooks.addLast(Cook(CookType.FISH))

        // Remove 1 FISH cook
        val incident = createIncident(-1, StaffType.COOK, CookType.FISH, staff)

        incident.apply()

        // Originally 4, removed 1 = 3 total
        assertEquals(3, staff.cooks.size)
        // Verify that no tournant COOK IS LEFT
        val tournantCount = staff.cooks.count { it.type == CookType.FISH }
        assertEquals(1, tournantCount)
    }

    // applying a negative count staff change removes the specified number of VEGETABLE cooks
    @Test
    fun `apply - Remove Cooks - Decreases Cook List- VEGETABLE`() {
        val staff = createDummyStaff()
        staff.cooks.addLast(Cook(CookType.VEGETABLE))
        staff.cooks.addLast(Cook(CookType.VEGETABLE))

        // Remove 1 VEGETABLE cook
        val incident = createIncident(-1, StaffType.COOK, CookType.VEGETABLE, staff)

        incident.apply()

        // Originally 4, removed 1 = 3 total
        assertEquals(3, staff.cooks.size)
        // Verify that no tournant COOK IS LEFT
        val tournantCount = staff.cooks.count { it.type == CookType.VEGETABLE }
        assertEquals(1, tournantCount)
    }

    // applying a negative count staff change removes the specified number of PASTRY cooks
    @Test
    fun `apply - Remove Cooks - Decreases Cook List- PASTRY`() {
        val staff = createDummyStaff()
        staff.cooks.addLast(Cook(CookType.PASTRY))
        staff.cooks.addLast(Cook(CookType.PASTRY))

        // Remove 1 PASTRY cook
        val incident = createIncident(-1, StaffType.COOK, CookType.PASTRY, staff)

        incident.apply()

        // Originally 4, removed 1 = 3 total
        assertEquals(3, staff.cooks.size)
        // Verify that no tournant COOK IS LEFT
        val tournantCount = staff.cooks.count { it.type == CookType.PASTRY }
        assertEquals(1, tournantCount)
    }

    //  applying a positive staff change incident correctly increases the waiter staff list
    @Test
    fun `apply - Add Waitstaff - Increases Waiter List`() {
        val staff = createDummyStaff()
        val incident = createIncident(3, StaffType.WAITSTAFF, null, staff)

        incident.apply()

        // Originally 2, added 3 = 5 total
        assertEquals(5, staff.waiters.size)
    }

    //  applying a negative staff change incident correctly decreases the waiter staff list
    @Test
    fun `apply - Remove Waitstaff - Decreases Waiter List`() {
        val staff = createDummyStaff()
        val incident = createIncident(-1, StaffType.WAITSTAFF, null, staff)

        incident.apply()

        // Originally 2, removed 1 = 1 total
        assertEquals(1, staff.waiters.size)
    }

    //  removing more waiters than currently available clamps the waiter list safely to zero
    @Test
    fun `apply - Remove More Waitstaff Than Available - Bounds at Zero`() {
        val staff = createDummyStaff()
        // Try to remove 5 waiters, but we only have 2
        val incident = createIncident(-5, StaffType.WAITSTAFF, null, staff)

        incident.apply()

        // List should not go negative, it should just safely clear
        assertEquals(0, staff.waiters.size)
    }

    //  applying a positive staff change incident correctly increases the driver staff list
    @Test
    fun `apply - Add Drivers - Increases Driver List`() {
        val staff = createDummyStaff()
        val incident = createIncident(2, StaffType.DRIVER, null, staff)

        incident.apply()

        // Originally 1, added 2 = 3 total
        assertEquals(3, staff.drivers.size)
    }

    //  applying a negative staff change incident correctly decreases the driver staff list
    @Test
    fun `apply - Remove Drivers - Decreases Driver List`() {
        val staff = createDummyStaff()
        val incident = createIncident(-1, StaffType.DRIVER, null, staff)

        incident.apply()

        // Originally 1, removed 1 = 0 total
        assertEquals(0, staff.drivers.size)
    }

    //  a cook change incident returns early without modifying staff when cookType is null
    @Test
    fun `apply- Cook Change with null cookType -Returns Early`() {
        val staff = createDummyStaff()

        // Pass null for cookType
        val incident = createIncident(1, StaffType.COOK, null, staff)
        incident.apply()

        // Size should remain completely unchanged
        assertEquals(2, staff.cooks.size)
    }

    //  cook removal only removes cooks of matching cookType and leaves other cook types intact
    @Test
    fun `apply - Remove Cooks skip non-matching cook types`() {
        val staff = createDummyStaff() // Current list: [TOURNANT, ROAST]

        // Ask to remove a ROAST cook.
        val incident = createIncident(-1, StaffType.COOK, CookType.ROAST, staff)
        incident.apply()

        // Verifies the ROAST was removed and the TOURNANT was safely skipped
        assertEquals(1, staff.cooks.size)
        assertEquals(CookType.TOURNANT, staff.cooks.first().type)
    }

    //  removing more cooks of a specific type than available safely removes only existing ones
    @Test
    fun `apply - Remove Cooks- count exceeds available cooks`() {
        val staff = createDummyStaff() // Current list: [TOURNANT, ROAST]

        // Try to remove 5 TOURNANT cooks, even though only 1 exists.
        val incident = createIncident(-5, StaffType.COOK, CookType.TOURNANT, staff)
        incident.apply()

        // Verifies it removed the 1 available TOURNANT and stopped safely
        assertEquals(1, staff.cooks.size)
        assertEquals(CookType.ROAST, staff.cooks.first().type)
    }
}
