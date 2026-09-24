package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Evening
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Tick
import de.unisaarland.cs.se.selab.Time
import de.unisaarland.cs.se.selab.enums.RestaurantType
import de.unisaarland.cs.se.selab.enums.TableType
import de.unisaarland.cs.se.selab.food.Recipe

const val TWENTY_FOUR = 24

/**
 * class shared between restaurant and browsing service
 *
 * @property restaurantId the restaurant's unique id
 * @property restaurantType the restaurant's cuisine/type
 * @property openingTickStart the first tick of the evening the restaurant is open
 * @property openingTickEnd the tick at which the restaurant closes for the evening
 * @property event whether the restaurant hosts an event tonight
 * @property positiveRatings positive ratings collected before this simulation run
 * @property negativeRatings negative ratings collected before this simulation run
 * @property menu the recipes this restaurant currently serves
 */
class RestaurantStats(
    val restaurantId: Id,
    val restaurantType: RestaurantType,
    val openingTickStart: Tick,
    val openingTickEnd: Tick,
    val event: Boolean,
    var positiveRatings: Int,
    var negativeRatings: Int,
    val menu: List<Recipe>,
) {
    var simulationPositiveRatings: Int = 0
    var simulationNegativeRatings: Int = 0
    var availableDrivers: Int = 0
    val availableSeats: MutableMap<TableType, Int> = mutableMapOf()
    val availableEventSeats: MutableMap<TableType, Int> = mutableMapOf()

    /**
     * Whether the restaurant is open at [tick], allowing for the buffer before closing, and can
     * still take an event booking for [eventEvening].
     *
     * @param tick the tick to check
     * @param eventEvening the evening the event visit would take place on
     * @return `true` if the restaurant is open at [tick]
     */
    fun isOpenAt(tick: Tick, eventEvening: Evening): Boolean {
        return tick in openingTickStart..openingTickEnd - 3 && openingTickStart <= TWENTY_FOUR * eventEvening
    }

    /**
     * Whether the restaurant is open right now, allowing for the buffer before closing.
     *
     * @return `true` if the current tick falls within the restaurant's opening hours
     */
    fun isOpen(): Boolean {
        return Time.tick in openingTickStart..openingTickEnd - 3
    }
}
