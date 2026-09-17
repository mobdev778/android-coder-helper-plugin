package com.github.mobdev778.plugin.removeprintln

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

class RemovePrintlnAction : DumbAwareAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        val kotlinFiles = collectKotlinFiles(selectedFiles)
        if (kotlinFiles.isEmpty()) return

        WriteCommandAction.runWriteCommandAction(project) {
            val fileDocumentManager = FileDocumentManager.getInstance()
            for (file in kotlinFiles) {
                val document = fileDocumentManager.getDocument(file) ?: continue
                removePrintlnLines(document)
            }
        }
    }

    private fun collectKotlinFiles(files: Array<VirtualFile>): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        for (file in files) {
            if (file.isDirectory) {
                VfsUtilCore.iterateChildrenRecursively(file, null) { child ->
                    if (!child.isDirectory && child.name.endsWith(KOTLIN_EXTENSION)) {
                        result.add(child)
                    }
                    true
                }
            } else if (file.name.endsWith(KOTLIN_EXTENSION)) {
                result.add(file)
            }
        }
        return result
    }

    private fun removePrintlnLines(document: Document) {
        val linesToDelete = mutableListOf<Int>()
        for (line in 0 until document.lineCount) {
            val startOffset = document.getLineStartOffset(line)
            val endOffset = document.getLineEndOffset(line)
            val text = document.getText(TextRange(startOffset, endOffset))
            if (text.contains(PRINTLN_MARKER)) {
                linesToDelete.add(line)
            }
        }

        for (line in linesToDelete.asReversed()) {
            val startOffset = document.getLineStartOffset(line)
            val endOffset = if (line + 1 < document.lineCount) {
                document.getLineStartOffset(line + 1)
            } else {
                document.getLineEndOffset(line)
            }
            document.deleteString(startOffset, endOffset)
        }
    }

    private companion object {
        const val KOTLIN_EXTENSION = ".kt"
        const val PRINTLN_MARKER = "println(\"!!!"
    }
}