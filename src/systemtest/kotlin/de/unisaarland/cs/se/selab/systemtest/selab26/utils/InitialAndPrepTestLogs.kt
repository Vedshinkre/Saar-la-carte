package de.unisaarland.cs.se.selab.systemtest.selab26.utils

/**
 * initial logs.
 */

object InitialAndPrepTestLogs {
    const val SIM_START = "[INFO] Simulation Info: Simulation started."
    fun initSuccess(file: String) = "[INFO] Initialization Info: $file successfully parsed and validated."
    fun initFail(file: String) = "[IMPORTANT] Initialization Info: $file is invalid."
    fun incident(id: Int, type: String, evening: Int) =
        "[IMPORTANT] Incident: Incident $id of type $type occurred before evening $evening."
    fun prepStart(evening: Int) = "[IMPORTANT] Preparation: Preparation for evening $evening starts."
    fun fohNoReserving(restId: Int, groupId: Int) =
        "[IMPORTANT] FOH No Reserving (R $restId): No table could be reserved for group $groupId."
    fun pantryRemoved(restId: Int, amount: Int, unit: String, name: String) =
        "[DEBUG] Pantry (R $restId): Removed $amount $unit of $name from the pantry."
    fun pantryProcured(restId: Int, amount: Int, unit: String, name: String) =
        "[DEBUG] Pantry (R $restId): Procured $amount $unit of $name from the supplier."
    fun pantryRestocked(restId: Int) = "[INFO] Pantry (R $restId): Restocked ingredients."
}
