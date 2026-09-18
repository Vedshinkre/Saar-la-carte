package deliveryservicetest

import org.junit.jupiter.api.Disabled

// Disabled: this whole file was written against Driver.processTick(), which the delivery
// refactor in 347de06 ("refactor delivery code to match logs from spec adjustment") replaced
// with the separate driveTowardsCustomer/driveTowardsRestaurant/logArrival/handOverToCustomer/
// finishReturnTrip steps orchestrated by FrontOfHouse. That behavior is now covered by
// DeliveryPipelineTest. Kept disabled (rather than deleted) so the original per-branch driver
// coverage can be ported over to the new API instead of quietly dropped.
@Disabled("Driver.processTick() was removed by the delivery refactor; see DeliveryPipelineTest")
class DriverTest {

    /*
    @BeforeEach
    fun setup() {
        // Reset time
        Time.tick = 0

        // Initialize the logger to DEBUG so it accepts all log messages
        Logger.setup(LogLevel.DEBUG)
    }
    private fun createDeliveryGroup(visitingAt: Int = VISITING): CasualGroup {
        return CasualGroup(
            id = 1,
            size = 2,
            tableType = TableType.COMMON,
            visitingAt = visitingAt,
            foodPreferences = emptyList(),
            restaurantTypes = listOf(RestaurantType.EUROPEAN),
            visitingEvenings = listOf(1),
            deliveryDistance = 5,
            ratingLikelihood = RatingLikelihood.ALWAYS
        )
    }

    private fun setupDriver(state: DriverState, ticksToDest: Int): Driver {
        return Driver().apply {
            id = 1
            this.state = state
            targetGroup = createDeliveryGroup(10)
            currentOrder = mock<Order> { whenever(it.dishes).thenReturn(emptyList()) }
            this.ticksToDest = ticksToDest
            totalTripTicks = 10
        }
    }

    //  Null / Early Return Branch Tests

    @Test
    fun `processTick - does nothing -state IDLE or WAITING`() {
        val driver = setupDriver(DriverState.IDLE, 5)
        driver.processTick()
        assertEquals(5, driver.ticksToDest) // Unchanged

        driver.state = DriverState.WAITING
        driver.processTick()
        assertEquals(5, driver.ticksToDest) // Unchanged
    }

    @Test
    fun `driveToCustomer-returns early-missing attributes`() {
        val driver = setupDriver(DriverState.DELIVERING, 5)

        // Null group
        driver.targetGroup = null
        driver.processTick()
        assertEquals(5, driver.ticksToDest)

        // Wrong group type
        driver.targetGroup = mock<CustomerGroup>()
        driver.processTick()
        assertEquals(5, driver.ticksToDest)

        // Null order
        driver.targetGroup = createDeliveryGroup(10)
        driver.currentOrder = null
        driver.processTick()
        assertEquals(5, driver.ticksToDest)

        // Null ID
        driver.currentOrder = mock<Order> {
            whenever(it.id).thenReturn(99)
            whenever(it.dishes).thenReturn(emptyList())
        }
        driver.id = null
        driver.processTick()
        assertEquals(5, driver.ticksToDest)
    }

    @Test
    fun `driveToRestaurant - returns early if missing ID`() {
        val driver = setupDriver(DriverState.RETURNING, 5)
        driver.id = null
        driver.processTick()
        assertEquals(5, driver.ticksToDest) // Unchanged
    }

    // success paths

    @Test
    fun `driveToCustomer - arrives early-POSITIVE experience`() {
        val driver = setupDriver(DriverState.DELIVERING, 1)
        Time.tick = 8 // visitingAt is 10

        driver.processTick()

        // Detekt
        val customerGroup = driver.targetGroup as? CasualGroup ?: error("targetGroup is null or wrong type")

        assertEquals(ExperienceType.POSITIVE, customerGroup.experience)
        assertEquals(DriverState.RETURNING, driver.state)
        assertEquals(5, driver.ticksToDest) // totalTripTicks(10) / 2
    }

    @Test
    fun `driveToCustomer - arrives exactly on time-NEUTRAL experience`() {
        val driver = setupDriver(DriverState.DELIVERING, 1)
        Time.tick = 10 // visitingAt is 10

        driver.processTick()

        // Detekt
        val customerGroup = driver.targetGroup as? CasualGroup ?: error("targetGroup is null or wrong type")

        assertEquals(ExperienceType.NEUTRAL, customerGroup.experience)
        assertEquals(DriverState.RETURNING, driver.state)
    }

    @Test
    fun `driveToCustomer - arrives late-NEGATIVE experience`() {
        val driver = setupDriver(DriverState.DELIVERING, 1)
        Time.tick = 11 // Late

        driver.processTick()

        // Detekt
        val customerGroup = driver.targetGroup as? CasualGroup ?: error("targetGroup is null or wrong type")

        assertEquals(ExperienceType.NEGATIVE, customerGroup.experience)
    }

    // edge cases

    @Test
    fun `driveToCustomer - timeouts trigger abortOrder and NEGATIVE experience`() {
        val driver = setupDriver(DriverState.DELIVERING, 5)
        val mockDishCooking = mock<Dish> { whenever(it.status).thenReturn(DishStatus.COOKING) }
        val mockDishCooked = mock<Dish> { whenever(it.status).thenReturn(DishStatus.COOKED) }
        val mockOrder = mock<Order> { whenever(it.dishes).thenReturn(listOf(mockDishCooking, mockDishCooked)) }
        driver.currentOrder = mockOrder

        // Timeout formula: group.visitingAt(10) + Constants.CUSTOMER_DELIVERY_WAIT_TICKS
        Time.tick = 10 + Constants.CUSTOMER_DELIVERY_WAIT_TICKS + 1

        driver.processTick()

        // Verify status updates
        assertEquals(ExperienceType.NEGATIVE, (driver.targetGroup as CasualGroup).experience)
        assertEquals(DriverState.RETURNING, driver.state)

        // Verify abortOrder correctly skipped the EATEN dish but updated the COOKING dish
        verify(mockDishCooking).status = DishStatus.ABORTED
        verify(mockDishCooked, never()).status = DishStatus.ABORTED
    }

    @Test
    fun `driveToRestaurant - finishes trip and resets state`() {
        val driver = setupDriver(DriverState.RETURNING, 1)

        driver.processTick()

        assertEquals(0, driver.ticksToDest)
        assertEquals(DriverState.IDLE, driver.state)
        assertNull(driver.currentOrder)
        assertNull(driver.targetGroup)
    }
    */
}
