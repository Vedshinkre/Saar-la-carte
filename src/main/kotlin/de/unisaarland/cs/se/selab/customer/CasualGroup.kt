package customer

import enums.RatingLikelihood
import enums.RestaurantType
import enums.TableType
import types.Evening
import types.Id
import types.Tick

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