package incidents

import food.Ingredient
import types.Evening
import types.Id

class PackagingChangeIncident(
    private val id: Id,
    private val evening: Evening,
    private val ingredient: Ingredient,
    private val packagingVolume: Int
) : Incident(id, evening)