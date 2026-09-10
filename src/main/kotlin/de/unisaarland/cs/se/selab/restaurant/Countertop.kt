package restaurant

import actors.Cook
import food.Order
import java.util.*

class Countertop(private val pantry: Pantry, private val orderQueue: Queue<Order>, private val cooks: List<Cook>)