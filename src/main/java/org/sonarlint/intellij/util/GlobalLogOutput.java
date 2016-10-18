/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015 SonarSource
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
package org.sonarlint.intellij.util;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Disposer;
import java.util.LinkedList;
import java.util.List;
import org.sonarlint.intellij.ui.SonarLintConsole;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;

public class GlobalLogOutput  extends ApplicationComponent.Adapter implements LogOutput, Disposable {
  private final ProjectManager projectManager;
  private final List<SonarLintConsole> consoles;

  public GlobalLogOutput(ProjectManager projectManager) {
    this.consoles = new LinkedList<>();
    this.projectManager = projectManager;
  }

  @Override
  public void initComponent() {
    projectManager.addProjectManagerListener(new ProjectListener(), this);
  }

  public static GlobalLogOutput get() {
    return ApplicationManager.getApplication().getComponent(GlobalLogOutput.class);
  }

  @Override
  public void log(String msg, Level level) {
    switch (level) {
      case TRACE:
      case DEBUG:
        for(SonarLintConsole c : consoles) {
          c.debug(msg);
        }
        break;
      case ERROR:
        for(SonarLintConsole c : consoles) {
          c.error(msg);
        }
        break;
      case INFO:
      case WARN:
      default:
        for(SonarLintConsole c : consoles) {
          c.info(msg);
        }
    }
  }

  @Override public void dispose() {
    // should remove listener in ProjectManager
    Disposer.dispose(this);
  }

  private class ProjectListener implements ProjectManagerListener {

    @Override public void projectOpened(Project project) {
      consoles.add(project.getComponent(SonarLintConsole.class));
    }

    @Override public boolean canCloseProject(Project project) {
      return true;
    }

    @Override public void projectClosed(Project project) {
      //nothing to do
    }

    @Override public void projectClosing(Project project) {
      consoles.remove(project.getComponent(SonarLintConsole.class));
    }
  }
}
