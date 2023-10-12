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
package org.sonarlint.intellij.its.tests.domain

import com.intellij.remoterobot.RemoteRobot
import org.assertj.core.api.Assertions
import org.sonarlint.intellij.its.fixtures.closeAllGotItTooltips
import org.sonarlint.intellij.its.fixtures.dialog
import org.sonarlint.intellij.its.fixtures.idea
import org.sonarlint.intellij.its.fixtures.notification
import org.sonarlint.intellij.its.fixtures.tool.window.toolWindow
import org.sonarlint.intellij.its.tests.AllUiTests.Companion.ISSUE_PROJECT_KEY
import org.sonarlint.intellij.its.tests.AllUiTests.Companion.ORCHESTRATOR
import org.sonarlint.intellij.its.tests.AllUiTests.Companion.token
import org.sonarlint.intellij.its.utils.ProjectBindingUtils.Companion.bindProjectToSonarQube

class CurrentFileTabTests {

    companion object {
        fun changeStatusAndPressChange(remoteRobot: RemoteRobot, status: String) {
            with(remoteRobot) {
                idea {
                    dialog("Mark Issue as Resolved on SonarQube") {
                        content(status) {
                            click()
                        }

                        pressButton("Mark Issue as...")
                    }
                }
            }
        }

        fun bindProjectFromCurrentFilePanel(remoteRobot: RemoteRobot) {
            with(remoteRobot) {
                idea {
                    toolWindow("SonarLint") {
                        ensureOpen()
                        tab("Current File") { select() }
                        content("CurrentFilePanel") {
                            toolBarButton("Configure SonarLint").click()
                        }
                    }
                    bindProjectToSonarQube(
                        remoteRobot,
                        ORCHESTRATOR.server.url,
                        token,
                        ISSUE_PROJECT_KEY
                    )
                }
            }
        }

        fun confirm(remoteRobot: RemoteRobot) {
            with(remoteRobot) {
                idea {
                    dialog("Confirm marking issue as resolved") {
                        pressButton("Confirm")
                    }
                }
            }
        }

        fun verifyIssueStatusWasSuccessfullyChanged(remoteRobot: RemoteRobot) {
            with(remoteRobot) {
                idea {
                    notification("The issue was successfully marked as resolved")
                    toolWindow("SonarLint") {
                        content("CurrentFilePanel") {
                            hasText("No issues found in the current opened file")
                        }
                    }
                }
            }
        }

        fun verifyIssueTreeContainsMessages(remoteRobot: RemoteRobot, vararg expectedMessages: String) {
            with(remoteRobot) {
                idea {
                    toolWindow("SonarLint") {
                        ensureOpen()
                        tabTitleContains("Current File") { select() }
                        expectedMessages.forEach { Assertions.assertThat(hasText(it)).isTrue() }
                    }
                }
            }
        }

        fun openIssueReviewDialogFromList(remoteRobot: RemoteRobot, issueMessage: String) {
            with(remoteRobot) {
                idea {
                    toolWindow("SonarLint") {
                        ensureOpen()
                        closeAllGotItTooltips()
                        tabTitleContains("Current File") { select() }
                        findText(issueMessage).rightClick()
                    }
                    actionMenuItem("Mark Issue as...") {
                        click()
                    }
                }
            }
        }
    }

}
