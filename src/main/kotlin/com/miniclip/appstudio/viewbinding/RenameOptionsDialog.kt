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

    var addAccessor: Boolean = true
    var accessor: String = "binding"

    init {
        addAccessor = props.getBoolean(ADD_ACCESSOR, addAccessor)
        accessor = props.getValue(STR_ACCESSOR, accessor)

        title = "Rename Variables"
        init()
    }

    override fun getDimensionServiceKey(): String {
        return "com.miniclip.appstudio.viewbinding-migration.dialog"
    }

    override fun dispose() {
        super.dispose()
        props.setValue(ADD_ACCESSOR, addAccessor)
        props.setValue(STR_ACCESSOR, accessor)
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            titledRow("Rename Variables") {
                fullRow {
                    checkBox("Add accessor prefix",
                        getter = { addAccessor },
                        setter = { value -> addAccessor = value },
                    )
                }
                fullRow {
                    label("Accessor name")
                    textField(
                        getter = { accessor },
                        setter = { value -> accessor = value },
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
        private const val ADD_ACCESSOR = "$NS.addAccessor"
        private const val STR_ACCESSOR = "$NS.accessor"
    }
}