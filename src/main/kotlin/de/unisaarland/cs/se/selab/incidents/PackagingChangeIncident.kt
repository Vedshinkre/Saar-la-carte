package incidents

import Evening
import Id
import food.Ingredient

class PackagingChangeIncident(
	private val id: Id,
	private val evening: Evening,
	private val ingredient: Ingredient,
	private val packagingVolume: Int
) : Incident(id, evening)