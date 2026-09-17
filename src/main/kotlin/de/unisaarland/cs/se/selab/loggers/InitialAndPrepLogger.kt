package de.unisaarland.cs.se.selab.loggers
import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.Time.evening
import de.unisaarland.cs.se.selab.enums.LogLevel
import de.unisaarland.cs.se.selab.enums.MeasurementUnit
/**
 * Handles initialization and preparation logs.
 */
object InitialAndPrepLogger {
    /**
     * Logs the result of parsing a file.
     */
    fun logInitialization(
        success: Boolean,
        filename: String
    ) {
        when (success) {
            true -> Logger.log(
                LogLevel.INFO,
                "Initialization Info: $filename successfully parsed and validated."
            )
            false -> Logger.log(
                LogLevel.IMPORTANT,
                "Initialization Info: $filename is invalid."
            )
        }
    }

    /**
     * Logs the start of the simulation.
     */
    fun logSimulationStart() {
        Logger.log(LogLevel.INFO, "Simulation Info: Simulation started.")
    }

    /**
     * Logs an incident.
     */
    fun logIncident(incidentId: Id, incidentType: String) {
        Logger.log(
            LogLevel.IMPORTANT,
            "Incident: Incident $incidentId of type $incidentType" +
                " occurred before evening $evening."
        )
    }

    /**
     * Logs the start of preparation.
     */
    fun logPreparationStart() {
        Logger.log(LogLevel.IMPORTANT, "Preparation: Preparation for evening $evening starts.")
    }

    /**
     * Logs when a group has no reservation.
     */
    fun logFohNoReservation(groupId: Id) {
        Logger.log(
            LogLevel.IMPORTANT,
            "FOH No Reserving (R ${Logger.restaurantID}): No table could be" +
                " reserved for group $groupId."
        )
    }

    /**
     * Logs ingredients procured from the supplier.
     */
    fun logPantryProcured(
        amount: Int,
        unit: MeasurementUnit,
        name: String
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Pantry (R ${Logger.restaurantID}): Procured $amount ${unit.label} of $name" +
                " from the supplier."
        )
    }

    /**
     * Logs when the pantry is restocked.
     */
    fun logPantryRestocked() {
        Logger.log(
            LogLevel.INFO,
            "Pantry (R ${Logger.restaurantID}): Restocked ingredients."
        )
    }

    /**
     * Logs an ingredient removed from the pantry.
     */
    fun logPantryRemovedIngredient(
        removedIngredientAmount: Int,
        unit: MeasurementUnit,
        ingredientName: String
    ) {
        Logger.log(
            LogLevel.DEBUG,
            "Pantry (R ${Logger.restaurantID}): Removed $removedIngredientAmount " +
                "${unit.label} of $ingredientName from the pantry."
        )
    }
}
