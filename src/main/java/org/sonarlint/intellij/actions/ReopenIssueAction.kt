/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2023 SonarSource
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.intellij.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DoNotAskOption
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import org.sonarlint.intellij.analysis.AnalysisStatus
import org.sonarlint.intellij.common.ui.SonarLintConsole
import org.sonarlint.intellij.common.util.SonarLintUtils
import org.sonarlint.intellij.config.global.ServerConnection
import org.sonarlint.intellij.core.BackendService
import org.sonarlint.intellij.core.ProjectBindingManager
import org.sonarlint.intellij.editor.CodeAnalyzerRestarter
import org.sonarlint.intellij.finding.LiveFinding
import org.sonarlint.intellij.finding.issue.LiveIssue
import org.sonarlint.intellij.ui.UiUtils
import org.sonarlint.intellij.util.DataKeys
import org.sonarlint.intellij.util.displayErrorNotification
import org.sonarlint.intellij.util.displaySuccessfulNotification

private const val SKIP_CONFIRM_REOPEN_DIALOG_PROPERTY = "SonarLint.reopenIssue.hideConfirmation"

class ReopenIssueAction(
    private var issue: LiveIssue? = null
)  :
    AbstractSonarAction(
        "Reopen", "Reopen the issue", null
    ), IntentionAction, PriorityAction, Iconable {
    companion object {
        private const val errorTitle = "<b>SonarLint - Unable to reopen the issue</b>"
        private const val content = "The issue was successfully reopened"

        val GROUP: NotificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("SonarLint: Mark Issue as Resolved")

        fun canBeReopened(project: Project, issue: LiveIssue): Boolean {
            return serverConnection(project) != null && issue.isResolved && issue.serverFindingKey == null
        }

        fun reopenIssueDialog(project: Project, issue: LiveFinding) {
            val connection = serverConnection(project) ?: return displayErrorNotification(
                project,
                errorTitle, "No connection could be found", GROUP
            )

            val file = issue.file() ?: return displayErrorNotification(project, errorTitle, "The file could not be found", GROUP)

            val module = ModuleUtil.findModuleForFile(file, project) ?: return displayErrorNotification(
                project, errorTitle, "No module could be found for this file", GROUP
            )
            val serverKey =
                issue.getServerKey() ?: issue.id?.toString() ?: return displayErrorNotification(
                    project,
                    errorTitle,
                    "The issue key could not be found",
                    GROUP
                )

            if (confirm(project, connection.productName)) {
                reopenIssue(project, module, issue, serverKey)
            }
        }

        private fun reopenIssue(
            project: Project,
            module: Module,
            issue: LiveFinding,
            issueKey: String
        ) {
            SonarLintUtils.getService(BackendService::class.java)
                .reopenIssue(module, issueKey)
                .thenAccept {
                    updateUI(project, issue)
                    displaySuccessfulNotification(project, content, GROUP)
                }
                .exceptionally { error ->
                    SonarLintConsole.get(project).error("Error while reopening the issue", error)
                    displayErrorNotification(project, "Could not reopen the issue", GROUP)
                    null
                }
        }

        private fun updateUI(project: Project, issue: LiveFinding) {
            UiUtils.runOnUiThread(project) {
                issue.isResolved = false
                SonarLintUtils.getService(project, SonarLintToolWindow::class.java).reopenIssue()
                SonarLintUtils.getService(project, CodeAnalyzerRestarter::class.java).refreshOpenFiles()
            }
        }

        private fun confirm(project: Project, productName: String): Boolean {
            return shouldSkipConfirmationDialogForReopening() || MessageDialogBuilder.okCancel(
                "Confirm reopening the issue",
                "Are you sure you want to reopen this issue? The status won't be updated on $productName"
            )
                .yesText("Confirm")
                .noText("Cancel")
                .doNotAsk(DoNotShowAgain())
                .ask(project)
        }

        private fun shouldSkipConfirmationDialogForReopening() = PropertiesComponent.getInstance()
            .getBoolean(SKIP_CONFIRM_REOPEN_DIALOG_PROPERTY, false)

        private fun serverConnection(project: Project): ServerConnection? = SonarLintUtils.getService(
            project,
            ProjectBindingManager::class.java
        ).tryGetServerConnection().orElse(null)
    }

    override fun isEnabled(e: AnActionEvent, project: Project, status: AnalysisStatus): Boolean {
        val issue: LiveIssue = e.getData(DataKeys.ISSUE_DATA_KEY)
            ?: return false
        return canBeReopened(project, issue)
    }

    override fun updatePresentation(e: AnActionEvent, project: Project) {
        e.presentation.description = "Reopen the issue"
    }


    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val issue: LiveIssue = e.getData(DataKeys.ISSUE_DATA_KEY)
            ?: return displayErrorNotification(project, errorTitle, "The issue could not be found", GROUP)

        reopenIssueDialog(project, issue)
    }


    private class DoNotShowAgain : DoNotAskOption {
        override fun isToBeShown() = true

        override fun setToBeShown(toBeShown: Boolean, exitCode: Int) {
            PropertiesComponent.getInstance().setValue(SKIP_CONFIRM_REOPEN_DIALOG_PROPERTY, java.lang.Boolean.toString(!toBeShown))
        }

        override fun canBeHidden() = true

        override fun shouldSaveOptionsOnCancel() = false

        override fun getDoNotShowMessage() = "Don't show again"
    }

    override fun getPriority() = PriorityAction.Priority.NORMAL

    override fun getIcon(flags: Int) = AllIcons.Actions.BuildLoadChanges

    override fun startInWriteAction() = false

    override fun getText() = "SonarLint: Reopen issue"

    override fun getFamilyName(): String {
        return "SonarLint reopen issue"
    }

    override fun isVisible(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        val issue: LiveIssue = e.getData(DataKeys.ISSUE_DATA_KEY)
            ?: return false
        return canBeReopened(project, issue)
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return issue?.let { canBeReopened(project, it) } ?: false
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        file?.let {
            issue?.let { reopenIssueDialog(project, it) }
        }
    }
}
