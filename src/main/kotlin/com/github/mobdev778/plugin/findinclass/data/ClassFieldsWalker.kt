package com.github.mobdev778.plugin.findinclass.data

import com.github.mobdev778.plugin.findinclass.domain.FieldDescriptor
import com.github.mobdev778.plugin.findinclass.domain.FieldType
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

object ClassFieldsWalker {

    fun collect(klass: KtClass): FieldPrefixTree {
        val prefixTree = FieldPrefixTree()
        val visited = HashSet<KtClassOrObject>()
        walk(klass, prefixTree, visited, null)
        return prefixTree
    }

    private fun walk(
        klass: KtClassOrObject,
        tree: FieldPrefixTree,
        visited: MutableSet<KtClassOrObject>,
        parent: FieldDescriptor?,
    ) {
        if (!visited.add(klass)) {
            return
        }

        for (property in klass.declarations.filterIsInstance<KtProperty>()) {
            visitProperty(
                property.name,
                property.typeReference,
                property.filePath(),
                property.lineNumber(),
                tree,
                visited,
                parent
            )
        }

        for (parameter in klass.primaryConstructorParameters.filter { it.hasValOrVar() }) {
            visitProperty(
                parameter.name,
                parameter.typeReference,
                parameter.filePath(),
                parameter.lineNumber(),
                tree,
                visited,
                parent
            )
        }

        val interfaceKlass = klass as? KtClass
        if (interfaceKlass != null && interfaceKlass.isInterface()) {
            val lightClass = interfaceKlass.toLightClass()
            if (lightClass != null) {
                val scope = GlobalSearchScope.allScope(interfaceKlass.project)
                for (implementer in ClassInheritorsSearch.search(lightClass, scope, true)) {
                    (implementer as? KtLightClass)?.kotlinOrigin?.let { walk(it, tree, visited, parent) }
                }
            }
        }
    }

    private fun visitProperty(
        name: String?,
        typeReference: KtTypeReference?,
        filePath: String?,
        lineNumber: Int,
        tree: FieldPrefixTree,
        visited: MutableSet<KtClassOrObject>,
        parent: FieldDescriptor?,
    ) {
        if (name != null && filePath != null) {
            var fieldType = FieldType.Default
            var className: String? = null
            var clazz: KtClassOrObject? = null

            for (typeClass in typeReference.resolveClasses()) {
                typeClass.fqName?.asString()?.let { cName ->
                    when {
                        cName.endsWith("kotlin.Array") -> fieldType = FieldType.Array
                        cName.endsWith("collections.List") -> fieldType = FieldType.List
                        cName.endsWith("collections.Set") -> fieldType = FieldType.Set
                        cName.endsWith("collections.Collection") -> fieldType = FieldType.Collection
                        else -> {
                            className = typeClass.name
                            clazz = typeClass
                        }
                    }
                }
            }

            if (className != null && clazz != null) {
                val descriptor = FieldDescriptor(
                    name = name,
                    type = className,
                    fieldType = fieldType,
                    file = filePath,
                    lineNumber = lineNumber,
                    parent = parent,
                )
                tree.insert(name, descriptor)
                tree.insert(className.substringAfterLast("."), descriptor)
                walk(clazz, tree, visited, descriptor)
            }
        }
    }

    private fun PsiElement.filePath(): String? = containingFile.virtualFile?.path

    private fun PsiElement.lineNumber(): Int {
        val document = PsiDocumentManager.getInstance(project).getDocument(containingFile) ?: return -1
        return document.getLineNumber(textOffset) + 1
    }

    private fun KtTypeReference?.resolveClasses(): List<KtClassOrObject> {
        val reference = this ?: return emptyList()
        val typeElement = reference.typeElement ?: return emptyList()
        val classes = mutableListOf<KtClassOrObject>()
        collectClasses(typeElement, classes)
        return classes
    }

    private fun collectClasses(
        element: KtTypeElement,
        classes: MutableList<KtClassOrObject>,
    ) {
        when (element) {
            is KtNullableType -> element.innerType?.let { collectClasses(it, classes) }
            is KtUserType -> {
                val klass = element.referenceExpression
                    ?.references
                    ?.lastOrNull()
                    ?.resolve() as? KtClassOrObject
                if (klass != null) {
                    classes += klass
                }
                element.typeArguments.forEach { argument ->
                    argument.typeReference?.typeElement?.let { collectClasses(it, classes) }
                }
            }

            is KtFunctionType -> {
                element.receiverTypeReference?.typeElement?.let { collectClasses(it, classes) }
                element.parameters.forEach { parameter ->
                    parameter.typeReference?.typeElement?.let { collectClasses(it, classes) }
                }
                element.returnTypeReference?.typeElement?.let { collectClasses(it, classes) }
            }
        }
    }
}
