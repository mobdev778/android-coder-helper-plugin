package com.github.mobdev778.plugin.findinclass.presentation

import androidx.compose.runtime.collectAsState
import com.github.mobdev778.MyBundle
import com.github.mobdev778.plugin.findinclass.data.FieldPrefixTree
import com.github.mobdev778.plugin.findinclass.domain.FieldDescriptor
import com.github.mobdev778.plugin.findinclass.domain.FieldDescriptorInteractor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.enableNewSwingCompositing
import java.awt.Component
import java.awt.Point
import java.awt.datatransfer.StringSelection
import javax.swing.Action
import javax.swing.JComponent

class FindInClassDialog(
    private val project: Project?,
    private val className: String?,
    private val fieldDescriptorInteractor: FieldDescriptorInteractor,
    parent: Component? = null,
) : DialogWrapper(project, parent, true, IdeModalityType.IDE) {

    init {
        title = MyBundle.message("dialog.title")
        init()
    }

    override fun createActions(): Array<Action> = arrayOf()

    override fun createContentPaneBorder() = null

    override fun createSouthPanel(): JComponent? = null

    @OptIn(ExperimentalJewelApi::class)
    override fun createCenterPanel(): JComponent {
        enableNewSwingCompositing()

        val component = JewelComposePanel {
            val descriptors = fieldDescriptorInteractor.fieldsFlow.collectAsState().value
            FindInClassDialogContent(
                className = className,
                descriptors = descriptors,
                onSearch = { query ->
                    fieldDescriptorInteractor.onQueryChanged(query)
                },
                onDescriptorClick = ::navigateToDescriptor,
                onCopyClick = ::copyDescriptor,
                onClose = { close(CANCEL_EXIT_CODE) },
            )
        }
        component.preferredSize = JBUI.size(1024, 768)
        component.minimumSize = JBUI.size(800, 600)

        return component
    }

    private fun copyDescriptor(descriptor: FieldDescriptor) {
        CopyPasteManager.getInstance().setContents(StringSelection(descriptor.getFullName()))
        showToast(MyBundle.message("dialog.copied", descriptor.getFullName()))
    }

    private fun showToast(message: String) {
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
            .setFadeoutTime(1000)
            .createBalloon()
            .show(
                RelativePoint(contentPane, Point(contentPane.width / 2, 8)),
                Balloon.Position.below,
            )
        close(OK_EXIT_CODE)
    }

    private fun navigateToDescriptor(descriptor: FieldDescriptor) {
        val project = project ?: return
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(descriptor.file) ?: return
        OpenFileDescriptor(project, virtualFile, descriptor.lineNumber - 1, 0).navigate(true)
        close(OK_EXIT_CODE)
    }
}