package de.unisaarland.cs.se.selab.systemtest.selab26.utils

/**
 * Shared formatting utilities for test log generators.
 */
object TestLogFormatter {
    fun formatIds(ids: List<Int>): String = ids.sorted().joinToString(",")

    fun formatMap(map: Map<String, Int>): String = map.entries
        .sortedBy { it.key }
        .joinToString(",") { "${it.key}:${it.value}" }
}
