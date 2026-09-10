package de.unisaarland.cs.se.selab

object Time {
    val tick: Tick = 1
    val evening: Evening = 1
    var maxTicks: Tick = 0
        set(value) {
            field = if (maxTicksAlreadySet) field else value
            maxTicksAlreadySet = true
        }
    private var maxTicksAlreadySet: Boolean = false
}