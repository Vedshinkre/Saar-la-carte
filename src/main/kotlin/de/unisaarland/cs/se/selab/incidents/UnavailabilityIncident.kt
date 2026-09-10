package incidents

import food.Ingredient
import food.Stock
import types.Evening
import types.Id

class UnavailabilityIncident(
    private val id: Id,
    private val evening: Evening,
    private val ingredient: Ingredient,
    private val duration: Int,
    private val stock: Stock
) : Incident(id, evening)