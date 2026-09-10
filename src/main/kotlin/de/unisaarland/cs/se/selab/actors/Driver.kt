package actors

import customer.CustomerGroup
import enums.DriverState
import food.Order
import types.Id
import types.Tick

class Driver {
    var id: Id? = null
    var currentOrder: Order? = null
    var targetGroup: CustomerGroup? = null
    var state: DriverState = DriverState.IDLE
    private var remainingTicks: Tick = 0
    private var totalTripTicks: Tick = 0
    private var ticksToDest: Tick = 0
}