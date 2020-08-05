/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2020 SonarSource
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
package org.sonarlint.intellij.editor;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.LowPriorityAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.sonarlint.intellij.issue.LiveIssue;
import org.sonarlint.intellij.util.SonarLintUtils;

public class ShowLocationsIntention implements IntentionAction, LowPriorityAction {
  private final LiveIssue issue;

  public ShowLocationsIntention(LiveIssue issue) {
    this.issue = issue;
  }

  @Nls @NotNull @Override public String getText() {
    return "Highlight all locations involved in this issue";
  }

  @Nls @NotNull @Override public String getFamilyName() {
    return "SonarLint locations";
  }

  @Override public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
    SonarLintUtils.getService(project, SonarLintHighlighting.class).highlightIssue(issue);
  }

  @Override public boolean startInWriteAction() {
    return false;
  }
}
