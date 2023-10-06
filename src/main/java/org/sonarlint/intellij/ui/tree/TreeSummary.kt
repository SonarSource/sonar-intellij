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
package org.sonarlint.intellij.ui.tree

import com.intellij.openapi.project.Project
import java.util.Locale
import org.sonarlint.intellij.cayc.CleanAsYouCodeService
import org.sonarlint.intellij.common.util.SonarLintUtils.getService
import org.sonarlint.intellij.core.BackendService

class TreeSummary(private val project: Project, private val treeContentKind: TreeContentKind, private val holdsOldFindings: Boolean) {
    private var emptyText = DEFAULT_EMPTY_TEXT
    var text: String = emptyText
        private set

    fun refresh(filesCount: Int, findingsCount: Int) {
        emptyText = computeEmptyText()
        text = computeText(filesCount, findingsCount)
    }

    fun reset() {
        emptyText = DEFAULT_EMPTY_TEXT
        text = emptyText
    }

    private fun computeText(filesCount: Int, findingsCount: Int): String {
        if (findingsCount == 0) {
            return emptyText
        }

        var sinceText = ""
        var newOrOldOrNothing = ""
        if (isFocusOnNewCode()) {
            sinceText = if (holdsOldFindings) "" else getCodePeriod()
            newOrOldOrNothing = if (holdsOldFindings) "older " else "new "
        }

        return String.format(FORMAT, findingsCount, newOrOldOrNothing, pluralize(treeContentKind.displayName, findingsCount), filesCount, pluralize("file", filesCount), sinceText)
    }

    private fun getCodePeriod(): String {
        return " " + getService(BackendService::class.java).getNewCodePeriodText(project).replaceFirstChar { char -> char.lowercase(Locale.getDefault()) }
    }

    private fun computeEmptyText(): String {
        if (isFocusOnNewCode()) {
            return if (holdsOldFindings) {
                "No older ${treeContentKind.displayName}s"
            } else {
                "No new ${treeContentKind.displayName}s${getCodePeriod()}"
            }
        }
        return "No ${treeContentKind.displayName}s to display"
    }

    private fun isFocusOnNewCode() = getService(project, CleanAsYouCodeService::class.java).shouldFocusOnNewCode()

    companion object {
        private const val DEFAULT_EMPTY_TEXT = "No analysis done"
        private const val FORMAT = "Found %d %s%s in %d %s%s"

        private fun pluralize(word: String, count: Int): String {
            return if (count == 1) word else word + "s"
        }
    }
}