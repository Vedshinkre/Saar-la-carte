package actors

import enums.CookType
import food.Recipe
import types.Id
import types.Tick

class Cook(val type: CookType) {
    var id: Id? = null
    private var orderId: Id? = null
    private var currentRecipe: Recipe? = null
    private var remainingTicks: Tick = 0
    var isCooking: Boolean = false
        private set
}