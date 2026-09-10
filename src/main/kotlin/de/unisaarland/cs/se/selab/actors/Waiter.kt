package actors

import enums.ActionType
import types.Id

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
