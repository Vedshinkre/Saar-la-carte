package de.unisaarland.cs.se.selab.loggers
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Time.evening
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit

object InitialAndPrepLogger {
    fun logInitialization(
        success: Boolean,
        filename: String
    ) {
        when (success) {
            true -> Logger.log(
                LogLevel.INFO,
                "Initialization Info: $filename successfully parsed and" +
                    "validated."
            )
            false -> Logger.log(
                LogLevel.IMPORTANT,
                "Initialization Info: $filename is invalid"
            )
        }
    }
    fun logSimulationStart() {
        Logger.log(LogLevel.INFO, "Simulation Info: Simulation started")
    }
    fun logIncident(incidentId: Id, incidentType: String) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Incident: Incident $incidentId of type $incidentType" +
                "occurred before evening $evening"
        )
    }
    fun logPreparationStart() {
        Logger.log(LogLevel.IMPORTANT, "Preparation: Preparation for evening $evening starts.")
    }
    fun logFohNoReservation(groupId: Id) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH No Reserving (R ${Logger.restaurantID}): No table could be" +
                "reserved for group $groupId."
        )
    }

    fun logPantryProcured(
        amount: Int,
        unit: MeasurementUnit,
        name: String
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Pantry (R ${Logger.restaurantID}): Procured $amount$unit of $name" +
                "from the supplier."
        )
    }

    fun logPantryRestocked() {
        Logger.log(
            LogLevel.INFO,
            "Pantry (R ${Logger.restaurantID}): Restocked ingredients."
        )
    }

    fun logPantryRemovedIngredient(
        removedIngredientAmount: Int,
        ingredientName: String
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Pantry (R ${Logger.restaurantID}): Removed $removedIngredientAmount" +
                "of $ingredientName from the pantry."
        )
    }
}
