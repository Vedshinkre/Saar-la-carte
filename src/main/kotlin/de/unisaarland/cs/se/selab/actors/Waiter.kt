package de.unisaarland.cs.se.selab.actors

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.ActionType

class Waiter {
    var id: Id? = null
    private var currentLoad: Int = 0
    private val tickLoads: Map<ActionType, Int> = mapOf(
        ActionType.SEAT to 0,
        ActionType.TAKE_ORDER to 0,
        ActionType.SERVE to 0,
        ActionType.ESCORT to 0,
    )
}
