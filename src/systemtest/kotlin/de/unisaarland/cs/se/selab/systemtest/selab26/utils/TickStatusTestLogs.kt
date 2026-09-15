package de.unisaarland.cs.se.selab.systemtest.selab26.utils

/**
 * status logs.
 */
object TickStatusTestLogs {
    fun servingStart(evening: Int) = "[IMPORTANT] Serving: Serving of evening $evening starts."
    fun tickStart(tick: Int, evening: Int) = "[IMPORTANT] Simulation: Tick $tick ($evening) started."
    fun restDecision(groupId: Int, restId: Int) =
        "[DEBUG] Restaurant Decision: Group $groupId decided on restaurant $restId."
    fun restNoDecision(groupId: Int) =
        "[DEBUG] Restaurant No Decision: Group $groupId could not decide for a restaurant."
    fun restStart(restId: Int) = "[DEBUG] Restaurant Start (R $restId): Restaurant $restId simulates a tick."
    fun restEnd(restId: Int) = "[DEBUG] Restaurant End (R $restId): Restaurant $restId finished simulating the tick."
    fun servingEnd(evening: Int) = "[IMPORTANT] Serving: Serving of evening $evening ends."
}
