package de.unisaarland.cs.se.selab.actors

/**
 * Represents the output of function cook .
 */
// rather than outputting tuple for the cook function , we return dataclass object
data class CookResult(
    val chefWasActive: Boolean,
    val totalAssignedMeals: Int,
    val finishedThisTick: Int
)