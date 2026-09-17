package com.github.mobdev778.plugin.findinclass.domain

import java.util.LinkedList

data class FieldDescriptor(
    val name: String,
    val type: String,
    val fieldType: FieldType,
    val file: String,
    val lineNumber: Int,
    val parent: FieldDescriptor?,
) {

    fun getFullName(): String {
        val list = LinkedList<String>()
        var node: FieldDescriptor? = this
        while (node != null) {
            list.addFirst(node.name)
            node = node.parent
        }
        return list.joinToString(".")
    }
}