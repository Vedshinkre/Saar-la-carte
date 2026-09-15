package simulationtests

/**import de.unisaarland.cs.se.selab.customer.CasualGroup
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.customer.EventGroup
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.system.Simulation
import de.unisaarland.cs.se.selab.system.SimulationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FilteringCustomerGroupsTest {
 private val simulation = Simulation(SimulationConfig())

 @Test
 fun `filtering returns casual groups in original order`() {
 val first = casualGroup(1)
 val event = eventGroup(2, evening = 1)
 val second = casualGroup(3)

 val result = invokeFilter("filterCasualGroups", listOf(first, event, second))

 assertEquals(listOf(first, second), result)
 }

 @Test
 fun `event groups for tonight preserve customer order`() {
 val first = eventGroup(2, evening = 1)
 val second = eventGroup(3, evening = 2)

 val method = Simulation::class.java.getDeclaredMethod(
 "getEventGroupsForTonight",
 Int::class.javaPrimitiveType
 )
 method.isAccessible = true
 val result = assertIs<List<*>>(method.invoke(simulation, 1))
 .map { assertIs<EventGroup>(it) }

 assertEquals(listOf(first), result)
 }

 private fun invokeFilter(
 methodName: String,
 customers: List<CustomerGroup>
 ): List<CustomerGroup> {
 val method = Simulation::class.java.getDeclaredMethod(methodName, List::class.java)
 method.isAccessible = true
 val result = assertIs<List<*>>(method.invoke(simulation, customers))
 return result.map { assertIs<CustomerGroup>(it) }
 }

 private fun casualGroup(id: Int) = CasualGroup(
 id = id,
 size = 2,
 tableType = TableType.COMMON,
 visitingAt = 1,
 foodPreferences = emptyList(),
 restaurantTypes = listOf(RestaurantType.ASIAN),
 visitingEvenings = emptyList(),
 deliveryDistance = 0,
 ratingLikelihood = RatingLikelihood.NEVER
 )

 private fun eventGroup(id: Int, evening: Int) = EventGroup(
 id = id,
 size = 2,
 tableType = TableType.COMMON,
 visitingAt = 1,
 foodPreferences = emptyList(),
 restaurantTypes = listOf(RestaurantType.ASIAN),
 eventEvening = evening,
 eventDishes = emptyMap()
 )
}**/
