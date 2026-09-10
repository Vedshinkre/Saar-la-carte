package incidents

import Evening
import Id
import food.Ingredient
import food.Stock

class UnavailabilityIncident(
	private val id: Id,
	private val evening: Evening,
	private val ingredient: Ingredient,
	private val duration: Int,
	private val stock: Stock
) : Incident(id, evening)