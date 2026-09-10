package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.food.Order
import restaurant.Pantry

class Kitchen(private val cooks: List<Cook>, private val pantry: Pantry, private val orderQueue: ArrayDeque<Order>) {
    private var finishedDishes = 0
    private var cookedDishes = 0
}
