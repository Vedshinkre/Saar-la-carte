package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.actors.Cook
import de.unisaarland.cs.se.selab.food.Order
import restaurant.Pantry

class Countertop(private val pantry: Pantry, private val orderQueue: ArrayDeque<Order>, private val cooks: List<Cook>)
