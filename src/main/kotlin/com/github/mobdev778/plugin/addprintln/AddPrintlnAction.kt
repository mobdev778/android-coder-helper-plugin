package com.github.mobdev778.plugin.addprintln

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.TextRange

class AddPrintlnAction : DumbAwareAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return

        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText ?: return
        if (selectedText.isBlank()) return

        val document = editor.document
        val lineNumber = document.getLineNumber(selectionModel.selectionStart)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)

        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        val indentation = lineText.takeWhile { it == ' ' || it == '\t' }

        val insertion = indentation + "println(\"!!! $selectedText: \${$selectedText}\")"

        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(lineStartOffset, "$insertion\n")
        }
    }
}