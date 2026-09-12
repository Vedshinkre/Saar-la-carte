package simulationtests

import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.customer.FoodPreference
import de.unisaarland.cs.se.selab.customer.RegularGroup
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.system.Simulation
import de.unisaarland.cs.se.selab.system.SimulationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class FilteringRegularGroupTest {
    private val simulation = Simulation(SimulationConfig())

    @Test
    fun `filtering returns regular groups in their original order`() {
        val firstRegularGroup = regularGroup(id = 1)
        val casualGroup = CasualGroup(
            id = 2,
            size = 2,
            tableType = TableType.COMMON,
            visitingAt = 5,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.ASIAN),
            visitingEvenings = emptyList(),
            deliveryDistance = 10,
            ratingLikelihood = RatingLikelihood.SOME
        )
        val eventGroup = EventGroup(
            id = 3,
            size = 4,
            tableType = TableType.SEPARATED,
            visitingAt = 6,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            eventEvening = 1,
            eventDishes = emptyMap()
        )
        val secondRegularGroup = regularGroup(id = 4)
        val customers: List<CustomerGroup> = listOf(
            firstRegularGroup,
            casualGroup,
            eventGroup,
            secondRegularGroup
        )

        val result = filterRegularGroups(customers)

        assertEquals(listOf(firstRegularGroup, secondRegularGroup), result)
        assertSame(firstRegularGroup, result[0])
        assertSame(secondRegularGroup, result[1])
    }

    @Test
    fun `filtering returns an empty list when no regular groups exist`() {
        val customers: List<CustomerGroup> = listOf(
            CasualGroup(
                id = 5,
                size = 2,
                tableType = TableType.BAR,
                visitingAt = 7,
                foodPreferences = emptyList(),
                restaurantTypes = emptyList(),
                visitingEvenings = emptyList(),
                deliveryDistance = 0,
                ratingLikelihood = RatingLikelihood.NEVER
            )
        )

        assertEquals(emptyList(), filterRegularGroups(customers))
    }

    private fun regularGroup(id: Int) = RegularGroup(
        id = id,
        size = 2,
        tableType = TableType.COMMON,
        visitingAt = 4,
        foodPreferences = listOf(FoodPreference(emptyList(), emptyList(), emptyList())),
        visitingStart = 1,
        visitingPeriod = 2,
        restaurantId = 10
    )

    private fun filterRegularGroups(customers: List<CustomerGroup>): List<RegularGroup> {
        val method = Simulation::class.java.getDeclaredMethod("filterRegularGroups", List::class.java)
        method.isAccessible = true
        val result = assertIs<List<*>>(method.invoke(simulation, customers))
        return result.map { assertIs<RegularGroup>(it) }
    }
}
