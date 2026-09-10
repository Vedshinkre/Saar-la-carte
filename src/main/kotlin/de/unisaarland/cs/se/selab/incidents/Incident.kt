package incidents

import types.Evening
import types.Id

abstract class Incident(private val id: Id, private val evening: Evening)