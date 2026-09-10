package de.unisaarland.cs.se.selab.restaurant

import de.unisaarland.cs.se.selab.Id
import de.unisaarland.cs.se.selab.enums.TableStatus
import de.unisaarland.cs.se.selab.enums.TableType

class Table(val id: Id, val size: Int, val tableType: TableType) {
    var status: TableStatus = TableStatus.FREE
}
