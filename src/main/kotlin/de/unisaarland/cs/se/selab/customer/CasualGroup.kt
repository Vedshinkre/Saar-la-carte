package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType

/** Represents a casual customer group. */
class CasualGroup(
    id: Id,
    size: Int,
    tableType: TableType,
    visitingAt: Tick, //I also want to access this
    foodPreferences: List<FoodPreference>,
    val restaurantTypes: List<RestaurantType>,
    private val visitingEvenings: List<Evening>,
    val deliveryDistance: Int,
    val ratingLikelihood: RatingLikelihood
) : CustomerGroup(id, size, tableType, visitingAt, foodPreferences)
