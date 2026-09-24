package de.unisaarland.cs.se.selab.actors

/**
Returned by [Cook.cookDishes] at the end of each cooking tick to communicate activity
 * status and meal transitions to the countertop and logging functions
 * @property chefWasActive indicates whether the cook was actively engaged in preparing meals during this tick.
 * @property totalAssignedMeals the total count of dishes currently assigned to and handled by the cook.
 * @property finishedThisTick the number of dishes that finished cooking and transitioned to cooked during this tick.
 */
// rather than outputting tuple for the cook function , we return dataclass object
data class CookResult(
    val chefWasActive: Boolean,
    val totalAssignedMeals: Int,
    val finishedThisTick: Int
)
