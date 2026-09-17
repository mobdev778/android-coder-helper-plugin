package com.github.mobdev778.plugin.clonedebuggedobject.domain

import com.squareup.kotlinpoet.CodeBlock
import com.sun.jdi.ArrayReference
import com.sun.jdi.BooleanValue
import com.sun.jdi.ByteValue
import com.sun.jdi.CharValue
import com.sun.jdi.ClassType
import com.sun.jdi.DoubleValue
import com.sun.jdi.FloatValue
import com.sun.jdi.IntegerValue
import com.sun.jdi.LongValue
import com.sun.jdi.ObjectReference
import com.sun.jdi.ShortValue
import com.sun.jdi.StringReference
import com.sun.jdi.Value
import com.sun.jdi.VoidValue

object CloneObjectGenerator {

    fun generate(variableName: String, value: Value): String? {
        val expression = buildValue(value, 0) ?: return null
        return CodeBlock.builder()
            .add("val %N = %L", variableName, expression)
            .build()
            .toString()
    }

    private fun buildValue(value: Value?, depth: Int): CodeBlock? {
        if (depth > MAX_DEPTH) return CodeBlock.of("null")
        return when (value) {
            null -> CodeBlock.of("null")
            is VoidValue -> CodeBlock.of("Unit")
            is BooleanValue -> CodeBlock.of("%L", value.value().toString())
            is ByteValue -> CodeBlock.of("%L", value.value().toString())
            is ShortValue -> CodeBlock.of("%L", value.value().toString())
            is IntegerValue -> CodeBlock.of("%L", value.value().toString())
            is LongValue -> CodeBlock.of("%L", value.value().toString() + "L")
            is FloatValue -> CodeBlock.of("%L", value.value().toString() + "f")
            is DoubleValue -> CodeBlock.of("%L", value.value().toString())
            is CharValue -> CodeBlock.of("'%L'", value.value().toString().replace("'", "\\'"))
            is StringReference -> CodeBlock.of("%S", value.value())
            is ArrayReference -> buildArray(value, depth)
            is ObjectReference -> buildObject(value, depth)
            else -> null
        }
    }

    private fun buildArray(array: ArrayReference, depth: Int): CodeBlock {
        val values = runCatching { array.getValues() }.getOrNull().orEmpty()
        val builder = CodeBlock.builder().add("arrayOf(")
        values.forEachIndexed { index, element ->
            if (index > 0) builder.add(", ")
            builder.add(buildValue(element, depth + 1) ?: CodeBlock.of("null"))
        }
        return builder.add(")").build()
    }

    private fun buildObject(obj: ObjectReference, depth: Int): CodeBlock? {
        val referenceType = obj.referenceType()

        enumConstantName(obj)?.let { constant ->
            return CodeBlock.of("%L.%L", simpleName(referenceType.name()), constant)
        }

        readListElements(obj)?.let { elements ->
            if (elements.isEmpty()) return CodeBlock.of("emptyList()")
            val builder = CodeBlock.builder().add("listOf(")
            elements.forEachIndexed { index, element ->
                if (index > 0) builder.add(", ")
                builder.add(buildValue(element, depth + 1) ?: CodeBlock.of("null"))
            }
            return builder.add(")").build()
        }

        val fieldValues = referenceType.allFields()
            .filter { !it.isStatic() }
            .mapNotNull { field ->
                val fieldValue = runCatching { obj.getValue(field) }.getOrNull()
                if (fieldValue == null) null else field.name() to fieldValue
            }

        val methods = referenceType.allMethods()
        val constructors = methods
            .filter { it.isConstructor() }
            .sortedByDescending { it.argumentTypeNames().size }
        val setters = methods
            .filter {
                !it.isConstructor() && !it.isStatic() &&
                    it.argumentTypeNames().size == 1 &&
                    it.name().startsWith("set") && it.name().length > 3
            }
            .map { it.name() }
            .toSet()

        val className = simpleName(referenceType.name())

        val noArgConstructor = constructors.firstOrNull { it.argumentTypeNames().isEmpty() }
        if (noArgConstructor != null && setters.isNotEmpty()) {
            return buildWithSetters(className, fieldValues, setters, depth)
        }

        val constructor = constructors.firstOrNull()
        if (constructor != null) {
            val args = fieldValues.take(constructor.argumentTypeNames().size)
            val builder = CodeBlock.builder().add("%L(", className)
            args.forEachIndexed { index, (_, fieldValue) ->
                if (index > 0) builder.add(", ")
                builder.add(buildValue(fieldValue, depth + 1) ?: CodeBlock.of("null"))
            }
            return builder.add(")").build()
        }

        return null
    }

    private fun buildWithSetters(
        className: String,
        fieldValues: List<Pair<String, Value>>,
        setters: Set<String>,
        depth: Int,
    ): CodeBlock {
        val builder = CodeBlock.builder()
        builder.add("%L().apply {\n", className)
        builder.indent()
        for ((fieldName, fieldValue) in fieldValues) {
            val setterName = "set" + fieldName.replaceFirstChar { it.uppercaseChar() }
            if (setterName in setters) {
                builder.addStatement("%N(%L)", setterName, buildValue(fieldValue, depth + 1) ?: CodeBlock.of("null"))
            }
        }
        builder.unindent()
        builder.add("}")
        return builder.build()
    }

    private fun enumConstantName(obj: ObjectReference): String? {
        val referenceType = obj.referenceType() as? ClassType ?: return null
        if (!referenceType.isEnum()) return null
        val nameField = referenceType.fieldByName("name") ?: return null
        return (runCatching { obj.getValue(nameField) }.getOrNull() as? StringReference)?.value()
    }

    private fun readListElements(obj: ObjectReference): List<Value>? {
        val referenceType = obj.referenceType() as? ClassType ?: return null
        val implementsList = referenceType.allInterfaces().any { it.name() == "java.util.List" }
        if (!implementsList) return null

        when (referenceType.name()) {
            EMPTY_LIST_NAME, COLLECTIONS_EMPTY_LIST_NAME -> return emptyList()
            COLLECTIONS_SINGLETON_LIST_NAME -> {
                val element = referenceType.fieldByName("element")
                    ?.let { runCatching { obj.getValue(it) }.getOrNull() }
                return if (element != null) listOf(element) else null
            }

            ARRAYS_ARRAY_LIST_NAME -> {
                val array = referenceType.fieldByName("a")
                    ?.let { runCatching { obj.getValue(it) }.getOrNull() as? ArrayReference }
                return array?.getValues()
            }
        }

        val elementDataField = referenceType.fieldByName("elementData")
        if (elementDataField != null) {
            val array = runCatching { obj.getValue(elementDataField) }.getOrNull() as? ArrayReference
                ?: return null
            val sizeField = referenceType.fieldByName("size")
            val size = sizeField
                ?.let { runCatching { obj.getValue(it) }.getOrNull() as? IntegerValue }
                ?.value()
                ?: array.length()
            return array.getValues().take(size)
        }

        val firstField = referenceType.fieldByName("first")
        if (firstField != null) {
            val result = mutableListOf<Value>()
            var node = runCatching { obj.getValue(firstField) }.getOrNull() as? ObjectReference
            while (node != null && result.size < MAX_ELEMENTS) {
                val itemField = node.referenceType().fieldByName("item") ?: break
                val item = runCatching { node.getValue(itemField) }.getOrNull() ?: break
                result.add(item)
                val nextField = node.referenceType().fieldByName("next")
                node = nextField?.let { runCatching { node.getValue(it) }.getOrNull() as? ObjectReference }
            }
            return result
        }

        return null
    }

    private fun simpleName(name: String): String =
        name.substringAfterLast('.').replace('$', '.')

    private const val MAX_DEPTH = 8
    private const val MAX_ELEMENTS = 64
    private const val EMPTY_LIST_NAME = "kotlin.collections.EmptyList"
    private const val COLLECTIONS_EMPTY_LIST_NAME = "java.util.Collections\$EmptyList"
    private const val COLLECTIONS_SINGLETON_LIST_NAME = "java.util.Collections\$SingletonList"
    private const val ARRAYS_ARRAY_LIST_NAME = "java.util.Arrays\$ArrayList"
}