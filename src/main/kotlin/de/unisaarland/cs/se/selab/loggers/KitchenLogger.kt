package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.LogLevel
/**
 * Handles kitchen logs.
 */
object KitchenLogger {
    /**
     * Logs a cook starting a dish.
     *
     * @param cookId the id of the cook
     * @param cookType the type of the cook
     * @param numberOfMeals the number of meals
     * @param dishName the name of the dish
     * @param baseOrderId the id of the order the cook started with
     * @param allOrderIds the ids of all orders
     */
    fun logKitchenDishAssignment(
        cookId: Id,
        cookType: String,
        numberOfMeals: Int,
        dishName: String,
        baseOrderId: Id,
        allOrderIds: List<Id>
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Kitchen Dish Assignment (R ${Logger.restaurantID}): Cook $cookId" +
                " of type $cookType starts cooking $numberOfMeals meals of dish $dishName" +
                " based on order $baseOrderId for orders ${Logger.formatIds(allOrderIds)}."
        )
    }

    /**
     * Logs finished meals.
     *
     * @param cookId the id of the cook
     * @param numberOfMeals the number of meals
     * @param dishName the name of the dish
     * @param cookDurationTick the number of ticks the cook needs for the dish
     */
    fun logKitchenMealCooked(
        cookId: Id,
        numberOfMeals: Int,
        dishName: String,
        cookDurationTick: Int
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Kitchen Meal Cooked (R ${Logger.restaurantID}): Cook $cookId" +
                " finished cooking $numberOfMeals meals of dish $dishName $cookDurationTick" +
                " ticks after ordering."
        )
    }

    /**
     * Logs the kitchen status.
     *
     * @param numberOfCooks the number of cooks
     * @param totalNumberOfMeals the total number of meals
     * @param finishedNumberOfMeals the finished number of meals
     * @param servableMeals the number of meals the waiters can serve
     */
    fun logKitchenStatus(
        numberOfCooks: Int,
        totalNumberOfMeals: Int,
        finishedNumberOfMeals: Int,
        servableMeals: Int
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Kitchen Status (R ${Logger.restaurantID}): $numberOfCooks cooks were" +
                " active cooking $totalNumberOfMeals and finishing $finishedNumberOfMeals" +
                " meals. $servableMeals meals can be served by the waitstaff."
        )
    }
}
