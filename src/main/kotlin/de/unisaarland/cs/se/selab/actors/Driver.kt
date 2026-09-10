package de.unisaarland.cs.se.selab.actors

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.customer.CustomerGroup
import de.unisaarland.cs.se.selab.enums.DriverState
import de.unisaarland.cs.se.selab.food.Order

class Driver {
    var id: Id? = null
    var currentOrder: Order? = null
    var targetGroup: CustomerGroup? = null
    var state: DriverState = DriverState.IDLE
    private var remainingTicks: Tick = 0
    private var totalTripTicks: Tick = 0
    private var ticksToDest: Tick = 0
}
