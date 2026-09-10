package de.unisaarland.cs.se.selab.food

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import java.util.concurrent.atomic.AtomicInteger

class Order(private val dishes: List<Dish>) {
    private val id: Id = nextId.getAndIncrement()
    private val orderedAt: Tick = Time.tick
    private val servedAt: Tick? = null
    private val deliveredAt: Tick? = null

    private companion object {
        val nextId = AtomicInteger(1)
    }
}
