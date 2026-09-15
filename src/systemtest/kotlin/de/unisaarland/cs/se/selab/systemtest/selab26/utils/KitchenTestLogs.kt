package de.unisaarland.cs.se.selab.systemtest.selab26.utils

/**
 * working of kitchen logs
 */
object KitchenTestLogs {
    fun kitchenAssign(
        restId: Int,
        cookId: Int,
        cookType: String,
        meals: Int,
        dishName: String,
        baseOrderId: Int,
        allOrders: List<Int>
    ) = "[IMPORTANT] Kitchen Dish Assignment (R $restId): Cook $cookId of type $cookType starts cooking $meals " +
        "meals of dish $dishName based on order $baseOrderId for orders ${TestLogFormatter.formatIds(allOrders)}."

    fun kitchenCooked(restId: Int, cookId: Int, meals: Int, dishName: String, ticks: Int) =
        "[IMPORTANT] Kitchen Meal Cooked (R $restId): Cook $cookId finished cooking $meals meals of dish " +
            "$dishName $ticks ticks after ordering."

    fun kitchenStatus(restId: Int, cooks: Int, cooking: Int, finished: Int, servable: Int) =
        "[DEBUG] Kitchen Status (R $restId): $cooks cooks were active cooking $cooking " +
            "and finishing $finished meals. $servable meals can be served by the waitstaff."
}
