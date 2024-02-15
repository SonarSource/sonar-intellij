/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2024 SonarSource
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
package org.sonarlint.intellij.its.utils

import com.intellij.remoterobot.fixtures.ActionButtonFixture
import com.intellij.remoterobot.fixtures.JListFixture
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import org.assertj.swing.timing.Pause
import org.sonarlint.intellij.its.BaseUiTest.Companion.remoteRobot
import org.sonarlint.intellij.its.fixtures.IdeaFrame
import org.sonarlint.intellij.its.fixtures.PreferencesDialog
import org.sonarlint.intellij.its.fixtures.clickWhenEnabled
import org.sonarlint.intellij.its.fixtures.dialog
import org.sonarlint.intellij.its.fixtures.idea
import org.sonarlint.intellij.its.fixtures.jPasswordField
import org.sonarlint.intellij.its.fixtures.jRadioButtons
import org.sonarlint.intellij.its.fixtures.jbTextFields
import org.sonarlint.intellij.its.fixtures.preferencesDialog
import org.sonarlint.intellij.its.fixtures.waitUntilLoaded
import org.sonarlint.intellij.its.fixtures.welcomeFrame
import java.time.Duration

class SettingsUtils {

    companion object {
        fun sonarLintGlobalSettings(function: PreferencesDialog.() -> Unit) {
            settings {
                // let the dialog settle (if we type the search query too soon it might be cleared for no reason)
                Pause.pause(3000)

                // Search for SonarLint because sometimes it is off the screen
                search("SonarLint")

                tree {
                    waitUntilLoaded()
                    // little trick to check if the search has been applied
                    waitFor(Duration.ofSeconds(10), Duration.ofSeconds(1)) { collectRows().size in 1..10 }
                    Pause.pause(1000)
                    clickPath("Tools", "SonarLint")
                }

                // let the SonarLint view settle (sometimes the UI thread blocks for a few seconds)
                Pause.pause(4000)

                function(this)
            }
        }

        fun clearConnections() {
            sonarLintGlobalSettings {
                val removeButton = actionButton(ActionButtonFixture.byTooltipText("Remove"))
                jList(JListFixture.byType()) {
                    while (collectItems().isNotEmpty()) {
                        removeButton.clickWhenEnabled()
                        optionalStep {
                            dialog("Connection In Use") {
                                button("Yes").click()
                            }
                        }
                    }
                }
                pressOk()
            }
        }

        fun toggleRule() {
            sonarLintGlobalSettings {
                findText("Rules").click()
                searchRule("java:S139")
                findText("Comments should not be located at the end of lines of code").doubleClick()
                Pause.pause(10000)
                button("Apply").click()
                Pause.pause(2000)
                button("OK").click()
            }
        }

        fun clearConnectionsAndAddSonarQubeConnection(serverUrl: String, token: String) {
            sonarLintGlobalSettings {
                val removeButton = actionButton(ActionButtonFixture.byTooltipText("Remove"))
                jList(JListFixture.byType()) {
                    while (collectItems().isNotEmpty()) {
                        removeButton.clickWhenEnabled()
                        optionalStep {
                            dialog("Connection In Use") {
                                button("Yes").click()
                            }
                        }
                    }
                }
                actionButton(ActionButtonFixture.byTooltipText("Add")).clickWhenEnabled()
                dialog("New Connection: Server Details") {
                    keyboard { enterText("Orchestrator") }
                    jRadioButtons()[1].select()
                    jbTextFields()[1].text = serverUrl
                    button("Next").click()
                }
                dialog("New Connection: Authentication") {
                    jPasswordField().text = token
                    button("Next").click()
                }
                dialog("New Connection: Configure Notifications") {
                    button("Next").click()
                }
                dialog("New Connection: Configuration completed") {
                    pressCreate()
                }
                pressOk()
            }
        }

        fun addSonarCloudConnection(token: String, connectionName: String) {
            sonarLintGlobalSettings {
                actionButton(ActionButtonFixture.byTooltipText("Add")).clickWhenEnabled()
                dialog("New Connection: Server Details") {
                    keyboard { enterText(connectionName) }
                    button("Next").click()
                }
                dialog("New Connection: Authentication") {
                    jPasswordField().text = token
                    button("Next").click()
                }
                dialog("New Connection: Organization") {
                    button("Next").click()
                }
                dialog("New Connection: Configure Notifications") {
                    button("Next").click()
                }
                dialog("New Connection: Configuration completed") {
                    pressCreate()
                }
                pressOk()
            }
        }

        fun clickPowerSaveMode() {
            optionalIdeaFrame()?.apply {
                actionMenu("File") {
                    open()
                    item("Power Save Mode") {
                        click()
                    }
                }
            }
        }

        fun goBackToWelcomeScreen() {
            optionalIdeaFrame()?.apply {
                actionMenu("File") {
                    open()
                    item("Close Project") {
                        click()
                    }
                }
            }
        }

        private fun optionalIdeaFrame(): IdeaFrame? {
            var ideaFrame: IdeaFrame? = null
            with(remoteRobot) {
                optionalStep {
                    // we might be on the welcome screen
                    ideaFrame = idea(Duration.ofSeconds(1))
                }
            }
            return ideaFrame
        }

        private fun settings(function: PreferencesDialog.() -> Unit) {
            with(remoteRobot) {
                try {
                    welcomeFrame {
                        openPreferences()
                    }
                } catch (e: Exception) {
                    idea {
                        openSettings()
                    }
                }
                preferencesDialog {
                    function(this)
                }
            }
        }
    }

}
