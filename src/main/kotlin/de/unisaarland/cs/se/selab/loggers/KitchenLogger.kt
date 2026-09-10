package de.unisaarland.cs.se.selab.loggers

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.LogLevel

object KitchenLogger {

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
                "of type $cookType starts cooking $numberOfMeals meals of dish $dishName" +
                "based on order $baseOrderId for orders $allOrderIds"
        )
    }
    fun logKitchenMealCooked(
        cookId: Int,
        numberOfMeals: Int,
        dishName: String,
        cookDurationTick: Int
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Kitchen Meal Cooked (R ${Logger.restaurantID}): Cook ${cookId}" +
                "finished cooking $numberOfMeals:number meals of dish $dishName" + " " + "${cookDurationTick}" +
                "ticks after ordering."
        )
    }
    fun logKitchenStatus(
        numberOfCooks: Int,
        totalNumberOfMeals: Int,
        finishedNumberOfMeals: Int,
        servableMeals: Int
    ) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Kitchen Status (R ${Logger.restaurantID}): $numberOfCooks cooks were" +
                "active cooking $totalNumberOfMeals and finishing $finishedNumberOfMeals" +
                "meals. $servableMeals meals can be served by the waitstaff."
        )
    }
}
