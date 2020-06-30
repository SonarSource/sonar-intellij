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
package org.sonarlint.intellij.util;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.ProjectManager;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.sonarlint.intellij.ui.SonarLintConsole;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;

public class GlobalLogOutput implements LogOutput {

  @Override
  public void log(String msg, Level level) {
    switch (level) {
      case TRACE:
      case DEBUG:
        debug(msg);
        break;
      case ERROR:
        getConsolesOfOpenedProjects().forEach(console -> console.error(msg));
        break;
      case INFO:
      case WARN:
      default:
        info(msg);
    }
  }

  public static void error(String msg, Throwable t) {
    getConsolesOfOpenedProjects()
      .forEach(sonarLintConsole -> sonarLintConsole.error(msg, t));
  }

  public static void debug(String msg) {
    getConsolesOfOpenedProjects()
      .forEach(sonarLintConsole -> sonarLintConsole.debug(msg));
  }

  public static void info(String msg) {
    getConsolesOfOpenedProjects()
      .forEach(sonarLintConsole -> sonarLintConsole.info(msg));
  }

  @NotNull
  private static Stream<SonarLintConsole> getConsolesOfOpenedProjects() {
    return Arrays.stream(ProjectManager.getInstance().getOpenProjects())
      .map(project -> ServiceManager.getService(project, SonarLintConsole.class))
      // Some console might already been disposed when plugin is unloading
      .filter(Objects::nonNull);
  }

}
