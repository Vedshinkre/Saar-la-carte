package de.unisaarland.cs.se.selab.incidents

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.food.Ingredient

class PackagingChangeIncident(
    private val id: Id,
    private val evening: Evening,
    private val ingredient: Ingredient,
    private val packagingVolume: Int
) : Incident(id, evening)
