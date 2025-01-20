/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2025 SonarSource
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

import com.intellij.openapi.actionSystem.AnActionEvent
import org.sonarlint.intellij.common.util.SonarLintUtils.getService
import org.sonarlint.intellij.ui.UiUtils.Companion.runOnUiThread

class ShowLogAction : AbstractSonarAction("Show Log") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        runOnUiThread(project) {
            getService(project, SonarLintToolWindow::class.java).openLogTab()
        }
    }

}
