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
package org.sonarlint.intellij.finding.issue

import java.net.URI
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.FileEditDto
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.QuickFixDto
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.TextEditDto
import org.sonarsource.sonarlint.core.rpc.protocol.common.TextRangeDto

fun aQuickFix(message: String, fileEdits: List<FileEditDto>) = QuickFixDto(fileEdits, message)

fun aFileEdit(fileUri: URI, textEdits: List<TextEditDto>) =
    FileEditDto(fileUri, textEdits)

fun aTextEdit(range: TextRangeDto, newText: String) = TextEditDto(range, newText)

fun aTextRange(
    startLine: Int,
    startLineOffset: Int,
    endLine: Int,
    endLineOffset: Int,
) = TextRangeDto(startLine, startLineOffset, endLine, endLineOffset)
