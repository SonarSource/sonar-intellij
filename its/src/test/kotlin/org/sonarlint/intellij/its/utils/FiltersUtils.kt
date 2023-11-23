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
package org.sonarlint.intellij.its.utils

import com.intellij.remoterobot.fixtures.ActionButtonFixture
import org.sonarlint.intellij.its.BaseUiTest
import org.sonarlint.intellij.its.fixtures.idea
import org.sonarlint.intellij.its.fixtures.tool.window.toolWindow

class FiltersUtils {

    companion object {
        fun setFocusOnNewCode() {
            setFocusOnNewCode(true)
        }

        fun resetFocusOnNewCode() {
            setFocusOnNewCode(false)
        }

        fun setFocusOnNewCode(focusOnNewCode: Boolean) {
            with(BaseUiTest.remoteRobot) {
                idea {
                    toolWindow("SonarLint") {
                        ensureOpen()
                        tabTitleContains("Current File") { select() }
                        content("CurrentFilePanel") {
                            val toolBarButton = toolBarButton("Set Focus on New Code")
                            val isFocusActivated = toolBarButton.popState() == ActionButtonFixture.PopState.SELECTED || toolBarButton.popState() == ActionButtonFixture.PopState.PUSHED
                            if (focusOnNewCode != isFocusActivated) {
                                toolBarButton.click()
                            }
                        }
                    }
                }
            }
        }

        fun showResolvedIssues() {
            with(BaseUiTest.remoteRobot) {
                idea {
                    toolWindow("SonarLint") {
                        ensureOpen()
                        tabTitleContains("Current File") { select() }
                        content("CurrentFilePanel") {
                            toolBarButton("Include Resolved Issues").click()
                        }
                    }
                }
            }
        }
    }

}
