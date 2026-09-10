package de.unisaarland.cs.se.selab.actors

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.CookType
import de.unisaarland.cs.se.selab.food.Recipe

class Cook(val type: CookType) {
    var id: Id? = null
    private var orderId: Id? = null
    private var currentRecipe: Recipe? = null
    private var remainingTicks: Tick = 0
    var isCooking: Boolean = false
        private set
}
