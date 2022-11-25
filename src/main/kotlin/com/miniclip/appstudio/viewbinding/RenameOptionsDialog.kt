package com.miniclip.appstudio.viewbinding

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiElement
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.fullRow
import com.intellij.ui.layout.panel
import javax.swing.JComponent

/**
 * A dialog with options for the rename action.
 */
class RenameOptionsDialog(
        private val project: Project,
        private val variables: List<PsiElement>) :
        DialogWrapper(project, true, IdeModalityType.PROJECT) {

    private val props = PropertiesComponent.getInstance(project)

    var addReceiver: Boolean = true
    var receiverName: String = "binding"

    init {
        addReceiver = props.getBoolean(ADD_RECEIVER, addReceiver)
        receiverName = props.getValue(STR_RECEIVER, receiverName)

        title = "Rename Variables"
        init()
    }

    override fun getDimensionServiceKey(): String {
        return "com.miniclip.appstudio.viewbinding-migration.dialog"
    }

    override fun dispose() {
        super.dispose()
        props.setValue(ADD_RECEIVER, addReceiver)
        props.setValue(STR_RECEIVER, receiverName)
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            titledRow("Rename Variables") {
                fullRow {
                    checkBox("Add receiver prefix",
                        getter = { addReceiver },
                        setter = { value -> addReceiver = value },
                    )
                }
                fullRow {
                    label("Receiver name")
                    textField(
                        getter = { receiverName },
                        setter = { value -> receiverName = value },
                    )
                }
                hideableRow("Preview (${variables.size} items)") {
                    for (ref in variables) {
                        fullRow {
                            label(ref.text).growPolicy(GrowPolicy.SHORT_TEXT)
                            label(" > ")
                            label(RenameAction.androidIdToVariable(ref.text)).growPolicy(GrowPolicy.SHORT_TEXT)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val NS = "ViewBindingRename"
        private const val ADD_RECEIVER = "$NS.addReceiver"
        private const val STR_RECEIVER = "$NS.receiver"
    }
}