package de.unisaarland.cs.se.selab.customer

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.enums.RatingLikelihood
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType

class CasualGroup(
    private val id: Id,
    val size: Int,
    val tableType: TableType,
    private val visitingAt: Tick,
    private val foodPreference: List<FoodPreference>,
    val restaurantTypes: List<RestaurantType>,
    private val visitingEvenings: List<Evening>,
    private val deliveryDistance: Int,
    val ratingLikelihood: RatingLikelihood
) : CustomerGroup(id, size, tableType, visitingAt, foodPreference)
