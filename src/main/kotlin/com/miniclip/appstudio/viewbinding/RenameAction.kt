package com.miniclip.appstudio.viewbinding

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.isInImportDirective


/**
 * The [AnAction] which finds synthetic properties and renames them to ViewBinding friendly names.
 */
class RenameAction : AnAction() {

    /**
     * Enable this Action for Kotlin files only.
     */
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabledAndVisible = editor != null && psiFile is KtFile
    }

    /**
     * Loop through file and rename variable references from snake_case to pascalCase.
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getRequiredData(CommonDataKeys.PROJECT)
        val psiFile = event.getRequiredData(CommonDataKeys.PSI_FILE) as KtFile
        val psiFactory = KtPsiFactory(event.project, true)

        val variables = ApplicationManager.getApplication().runReadAction(Computable {
            PsiTreeUtil.collectElements(psiFile) { element ->
                element is KtNameReferenceExpression && isSyntheticProperty(element)
            }
        })

        if (variables.isEmpty()) {
            notify(project, "No synthetic properties found to rename.", NotificationType.ERROR)
        } else {
            val dialog = RenameOptionsDialog(project, variables.toList())
            if (dialog.showAndGet()) {

                WriteCommandAction.runWriteCommandAction(psiFile.project) {
                    for (element in variables) {
                        val before = element.parent.text
                        // add accessor and dot
                        if (dialog.addAccessor && dialog.accessor.isNotBlank() && element.prevSibling == null) {
                            element.parent.addBefore(psiFactory.createSimpleName(dialog.accessor), element)
                            element.parent.addBefore(psiFactory.createDot(), element)
                        }
                        // rename
                        val pascal = androidIdToVariable(element.text)
                        val replacement = psiFactory.createSimpleName(pascal)
                        element.replace(replacement)

                        LOG.info("Changed name reference from $before to ${replacement.parent.text}")
                    }
                    notify(project, "${variables.size} variables have been renamed.", NotificationType.INFORMATION)
                }

                // commit changes
                with (PsiDocumentManager.getInstance(project)) {
                    getDocument(psiFile)?.let(::commitDocument)
                }
            }
        }
    }

    private fun isSyntheticProperty(element: KtNameReferenceExpression): Boolean {
        // skip imports
        if (element.isInImportDirective()) {
            return false
        }
        // synthetic properties contain 2 references
        if (element.references.size < 2) {
            return false
        }

        val nameRef = element.references.find { it is KtSimpleNameReference }
        val referencedElement = nameRef?.resolve()

        return referencedElement is XmlAttributeValue && referencedElement.value.startsWith("@+id/")
    }

    private fun notify(project: Project?, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("com.miniclip.appstudio.viewbinding-migration.notify")
                .createNotification(content, type)
                .notify(project)
    }


    companion object {
        private val LOG = logger<RenameAction>()
        private val PASCAL_PATTERN = Regex("_([a-z])")

        fun androidIdToVariable(id: String): String {
            return toPascal(id.substringAfter('/'))
        }

        fun previewVariable(id: String, accessor: String?): String {
            val variable = androidIdToVariable(id)
            return if (accessor.isNullOrEmpty()) variable else "$accessor.$variable"
        }

        private fun toPascal(name: String): String {
            return name.lowercase().replace(PASCAL_PATTERN) { match -> match.groupValues[1].uppercase() }
        }
    }
}