package com.github.mobdev778.plugin.findinclass.data

import com.github.mobdev778.plugin.findinclass.domain.FieldDescriptor

internal class PrefixTreeNode {
    val children = LinkedHashMap<Char, PrefixTreeNode>()
    val descriptors = LinkedHashSet<FieldDescriptor>()
}