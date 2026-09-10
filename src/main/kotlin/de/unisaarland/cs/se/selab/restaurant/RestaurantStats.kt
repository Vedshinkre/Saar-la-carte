package restaurant

import enums.RestaurantType
import food.Recipe
import types.Id
import types.Tick

class RestaurantStats(
    private val restaurantId: Id,
    private val restaurantType: RestaurantType,
    private val openingTickStart: Tick,
    private val openingTickEnd: Tick,
    private val event: Boolean,
    private var positiveRatings: Int,
    private var negativeRatings: Int,
    private val menu: List<Recipe>
)