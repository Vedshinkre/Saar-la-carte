package restaurant

import enums.TableStatus
import enums.TableType
import types.Id

class Table(val id: Id, val size: Int, val tableType: TableType) {
    var status: TableStatus = TableStatus.FREE
}