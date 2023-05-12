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
package org.sonarlint.intellij.actions.filters;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.sonarlint.intellij.actions.AbstractSonarToggleAction;
import org.sonarlint.intellij.actions.SonarLintToolWindow;

import static org.sonarlint.intellij.common.util.SonarLintUtils.getService;

public class FilterSecurityHotspotAction extends AbstractSonarToggleAction {

  private final SecurityHotspotFilters filter;

  public FilterSecurityHotspotAction(SecurityHotspotFilters filter) {
    super(filter.getTitle());
    this.filter = filter;
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return FilterSecurityHotspotSettings.getCurrentlySelectedFilter() == filter;
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean enabled) {
    var project = e.getProject();
    if (project == null) {
      return;
    }

    if (enabled && FilterSecurityHotspotSettings.getCurrentlySelectedFilter() != filter) {
      getService(project, SonarLintToolWindow.class).filterSecurityHotspotTab(filter);
      FilterSecurityHotspotSettings.setCurrentlySelectedFilter(filter);
    }
  }

}
