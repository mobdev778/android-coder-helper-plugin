package com.github.mobdev778.plugin.clonedebuggedobject.presentation

import com.github.mobdev778.MyBundle
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.enableNewSwingCompositing
import java.awt.Component
import java.awt.Point
import java.awt.datatransfer.StringSelection
import javax.swing.Action
import javax.swing.JComponent

class CloneObjectDialog(
    private val project: Project?,
    private val code: String,
    parent: Component? = null,
) : DialogWrapper(project, parent, true, IdeModalityType.IDE) {

    init {
        title = MyBundle.message("cloneObject.dialog.title")
        init()
    }

    override fun createActions(): Array<Action> = arrayOf()

    override fun createContentPaneBorder() = null

    override fun createSouthPanel(): JComponent? = null

    @OptIn(ExperimentalJewelApi::class)
    override fun createCenterPanel(): JComponent {
        enableNewSwingCompositing()

        val component = JewelComposePanel {
            CloneObjectDialogContent(
                code = code,
                onCopyClick = ::copyCode,
                onClose = { close(CANCEL_EXIT_CODE) },
            )
        }
        component.preferredSize = JBUI.size(1024, 768)
        component.minimumSize = JBUI.size(800, 600)

        return component
    }

    private fun copyCode() {
        CopyPasteManager.getInstance().setContents(StringSelection(code))
        showToast(MyBundle.message("cloneObject.dialog.copied"))
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
    }
}
