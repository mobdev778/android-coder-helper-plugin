package com.github.mobdev778.plugin.clonedebuggedobject

import com.github.mobdev778.plugin.clonedebuggedobject.domain.CloneObjectGenerator
import com.github.mobdev778.plugin.clonedebuggedobject.domain.CodeFormatter
import com.github.mobdev778.plugin.clonedebuggedobject.presentation.CloneObjectDialog
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.DebuggerManagerThreadImpl
import com.intellij.debugger.engine.events.DebuggerCommandImpl
import com.intellij.debugger.engine.jdi.StackFrameProxy
import com.intellij.debugger.impl.PrioritizedTask
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.sun.jdi.Value

class CloneDebuggedObjectAction : DumbAwareAction() {

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val variableName = identifierAtCaret(event) ?: return
        val value = readValue(project, variableName) ?: return
        val code = CloneObjectGenerator.generate(variableName, value) ?: return
        val formattedCode = CodeFormatter.format(code)

        CloneObjectDialog(project, formattedCode).show()
    }

    private fun readValue(project: Project, variableName: String): Value? {
        val context = DebuggerManagerEx.getInstanceEx(project).context
        val frameProxy = context.frameProxy ?: return null
        val managerThread = context.debugProcess?.managerThread ?: return null

        if (DebuggerManagerThreadImpl.isManagerThread()) {
            return readValueOnFrame(frameProxy, variableName)
        }

        var value: Value? = null
        val command = object : DebuggerCommandImpl(PrioritizedTask.Priority.HIGH) {
            override fun action() {
                value = readValueOnFrame(frameProxy, variableName)
            }
        }
        managerThread.invokeAndWait(command)
        return value
    }

    private fun readValueOnFrame(frameProxy: StackFrameProxy, variableName: String): Value? {
        val stackFrame = runCatching { frameProxy.stackFrame }.getOrNull() ?: return null

        val localVariable = runCatching { stackFrame.visibleVariables() }
            .getOrNull()
            ?.firstOrNull { it.name() == variableName }
        if (localVariable != null) {
            return runCatching { stackFrame.getValue(localVariable) }.getOrNull()
        }

        val thisObject = runCatching { stackFrame.thisObject() }.getOrNull() ?: return null
        val field = runCatching { thisObject.referenceType().fieldByName(variableName) }.getOrNull()
            ?: return null
        return runCatching { thisObject.getValue(field) }.getOrNull()
    }

    private fun identifierAtCaret(event: AnActionEvent): String? {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return null
        val element = file.findElementAt(editor.caretModel.offset) ?: return null
        return identifierText(element)
    }

    private fun identifierText(element: PsiElement): String? {
        val text = element.text
        if (text.isEmpty()) return null
        if (!text.first().isJavaIdentifierStart()) return null
        if (!text.all { it.isJavaIdentifierPart() }) return null
        return text
    }
}