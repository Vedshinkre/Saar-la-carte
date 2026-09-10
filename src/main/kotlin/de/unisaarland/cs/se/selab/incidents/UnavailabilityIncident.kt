package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.food.Ingredient
import de.unisaarland.cs.se.selab.food.Stock

class UnavailabilityIncident(
    private val id: Id,
    private val evening: Evening,
    private val ingredient: Ingredient,
    private val duration: Int,
    private val stock: Stock
) : Incident(id, evening)
