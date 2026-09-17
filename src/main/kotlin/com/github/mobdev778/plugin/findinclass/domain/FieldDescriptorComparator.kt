package com.github.mobdev778.plugin.findinclass.domain

import kotlin.text.compareTo

class FieldDescriptorComparator(
    private val query: String,
) : Comparator<FieldDescriptor> {

    override fun compare(first: FieldDescriptor, second: FieldDescriptor): Int {
        val scoreComparison = score(second).compareTo(score(first))
        if (scoreComparison != 0) return scoreComparison

        val nameComparison = first.name.compareTo(second.name, ignoreCase = true)
        if (nameComparison != 0) return nameComparison

        return first.type.compareTo(second.type, ignoreCase = true)
    }

    private fun score(descriptor: FieldDescriptor): Int =
        maxOf(matchScore(descriptor.name), matchScore(descriptor.type))

    private fun matchScore(value: String): Int = when {
        value.equals(query, ignoreCase = true) -> 3
        value.startsWith(query, ignoreCase = true) -> 2
        value.contains(query, ignoreCase = true) -> 1
        else -> 0
    }
}
