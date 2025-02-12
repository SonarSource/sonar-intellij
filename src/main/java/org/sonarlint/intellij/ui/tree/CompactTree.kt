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
package org.sonarlint.intellij.ui.tree

class CompactTree(private val nodesByParent: Map<Any, List<Any>>) {
    fun getChild(parent: Any, index: Int) = nodesByParent[parent]?.getOrNull(index)
    fun getChildCount(parent: Any) = nodesByParent[parent]?.size ?: 0

    fun getIndexOfChild(parent: Any?, child: Any?): Int {
        if (parent == null || child == null) {
            return -1
        }
        return nodesByParent[parent]?.indexOf(child) ?: -1
    }
}
