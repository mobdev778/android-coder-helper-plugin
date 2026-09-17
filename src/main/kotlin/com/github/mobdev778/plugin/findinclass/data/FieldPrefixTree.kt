package com.github.mobdev778.plugin.findinclass.data

import com.github.mobdev778.plugin.findinclass.domain.FieldDescriptor
import com.github.mobdev778.plugin.findinclass.domain.FieldDescriptorComparator
import java.util.LinkedList
import kotlin.collections.iterator
import kotlin.text.iterator

class FieldPrefixTree {

    private val root = PrefixTreeNode()

    fun insert(word: String, descriptor: FieldDescriptor) {
        if (word.isEmpty()) return
        var node = root
        for (ch in word) {
            node = node.children.getOrPut(ch) { PrefixTreeNode() }
        }
        node.descriptors.add(descriptor)
    }

    fun find(query: String): List<FieldDescriptor> {
        if (query.isEmpty()) {
            return emptyList()
        }

        val result = LinkedHashSet<FieldDescriptor>()
        val visited = HashSet<BFSNode>()

        val queue = LinkedList<BFSNode>()
        queue.add(BFSNode(root, 0, MAX_ERRORS))

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) {
                continue
            }

            val node = current.node
            val index = current.index
            val manna = current.manna

            if (index == query.length) {
                collectDescriptors(node, result)
                continue
            }

            val ch = query[index]
            for ((key, child) in node.children) {
                if (key.equals(ch, ignoreCase = true)) {
                    queue.add(BFSNode(child, index + 1, manna))
                } else if (manna > 0) {
                    queue.add(BFSNode(child, index + 1, manna - 1))
                }
            }

            if (manna > 0) {
                for ((_, child) in node.children) {
                    queue.add(BFSNode(child, index, manna - 1))
                }
                queue.add(BFSNode(node, index + 1, manna - 1))
            }
        }

        return result.sortedWith(FieldDescriptorComparator(query))
    }

    fun collect(): List<String> {
        val result = LinkedHashSet<String>()
        collect(root, StringBuilder(), result)
        return result.sorted()
    }

    private fun collect(
        prefixTreeNode: PrefixTreeNode,
        prefix: StringBuilder,
        result: MutableSet<String>
    ) {
        if (prefixTreeNode.descriptors.isNotEmpty()) {
            for (descriptor in prefixTreeNode.descriptors) {
                result.add(descriptor.name + ":" + descriptor.type)
            }
        }
        for ((ch, child) in prefixTreeNode.children) {
            prefix.append(ch)
            collect(child, prefix, result)
            prefix.deleteCharAt(prefix.length - 1)
        }
    }

    private fun collectDescriptors(
        node: PrefixTreeNode,
        result: MutableSet<FieldDescriptor>,
    ) {
        for (descriptor in node.descriptors) {
            result.add(descriptor)
        }
        for ((_, child) in node.children) {
            collectDescriptors(child, result)
        }
    }

    companion object {
        private const val MAX_ERRORS = 1
    }
}