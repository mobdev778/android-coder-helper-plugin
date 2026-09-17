package com.github.mobdev778.plugin.findinclass

import com.github.mobdev778.plugin.findinclass.data.ClassFieldsWalker
import com.github.mobdev778.plugin.findinclass.domain.FieldDescriptorInteractor
import com.github.mobdev778.plugin.findinclass.presentation.FindInClassDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClass

class FindInClassAction : DumbAwareAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return

        val element = file.findElementAt(editor.caretModel.offset) ?: return

        val klass = PsiTreeUtil.getParentOfType(element, KtClass::class.java) ?: return
        val prefixTree = ClassFieldsWalker.collect(klass)
        val fieldDescriptorInteractor = FieldDescriptorInteractor(prefixTree)
        FindInClassDialog(project, klass.name, fieldDescriptorInteractor).show()
    }
}