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
package org.sonarlint.intellij.core;

import com.intellij.openapi.project.Project;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonarlint.intellij.config.global.SonarLintGlobalSettings;
import org.sonarlint.intellij.config.global.SonarQubeServer;
import org.sonarlint.intellij.config.project.SonarLintProjectSettings;
import org.sonarlint.intellij.exception.InvalidBindingException;
import org.sonarlint.intellij.ui.SonarLintConsole;
import org.sonarlint.intellij.util.SonarLintUtils;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;

public class ProjectBindingManager {
  private final Project myProject;

  public ProjectBindingManager(Project project) {
    this.myProject = project;
  }

  /**
   * Returns a Facade with the appropriate engine (standalone or connected) based on the current project and module configurations.
   * In case of a problem, it handles the displaying of errors (Logging, user notifications, ..) and throws an InvalidBindingException.
   *
   * @throws InvalidBindingException If current project binding is invalid
   */
  public synchronized SonarLintFacade getFacade() throws InvalidBindingException {
    return getFacade(false);
  }

  public synchronized SonarLintFacade getFacade(boolean logDetails) throws InvalidBindingException {
    SonarLintEngineManager engineManager = SonarLintUtils.getService(SonarLintEngineManager.class);
    SonarLintProjectSettings projectSettings = SonarLintUtils.getService(myProject, SonarLintProjectSettings.class);
    SonarLintProjectNotifications notifications = SonarLintUtils.getService(myProject, SonarLintProjectNotifications.class);
    SonarLintConsole console = SonarLintUtils.getService(myProject, SonarLintConsole.class);
    if (projectSettings.isBindingEnabled()) {
      String serverId = projectSettings.getServerId();
      String projectKey = projectSettings.getProjectKey();
      checkBindingStatus(notifications, serverId, projectKey);
      if (logDetails) {
        console.info(String.format("Using configuration of '%s' in server '%s'", projectKey, serverId));
      }
      ConnectedSonarLintEngine engine = engineManager.getConnectedEngine(notifications, serverId, projectKey);
      return new ConnectedSonarLintFacade(engine, projectSettings, console, myProject);
    }

    return new StandaloneSonarLintFacade(projectSettings, console, myProject, engineManager.getStandaloneEngine());
  }

  public synchronized ConnectedSonarLintEngine getConnectedEngineSkipChecks() {
    SonarLintEngineManager engineManager = SonarLintUtils.getService(SonarLintEngineManager.class);
    SonarLintProjectSettings projectSettings = SonarLintUtils.getService(myProject, SonarLintProjectSettings.class);
    return engineManager.getConnectedEngine(projectSettings.getServerId());
  }

  public synchronized ConnectedSonarLintEngine getConnectedEngine() throws InvalidBindingException {
    SonarLintProjectSettings projectSettings = SonarLintUtils.getService(myProject, SonarLintProjectSettings.class);
    if (!projectSettings.isBindingEnabled()) {
      throw new IllegalStateException("Project is not bound to a SonarQube project");
    }
    SonarLintProjectNotifications notifications = SonarLintUtils.getService(myProject, SonarLintProjectNotifications.class);
    String serverId = projectSettings.getServerId();
    String projectKey = projectSettings.getProjectKey();
    checkBindingStatus(notifications, serverId, projectKey);

    SonarLintEngineManager engineManager = SonarLintUtils.getService(SonarLintEngineManager.class);
    return engineManager.getConnectedEngine(notifications, serverId, projectKey);
  }

  public synchronized SonarQubeServer getSonarQubeServer() throws InvalidBindingException {
    SonarLintProjectSettings projectSettings = SonarLintUtils.getService(myProject, SonarLintProjectSettings.class);
    String serverId = projectSettings.getServerId();
    SonarLintGlobalSettings globalSettings = SonarLintUtils.getService(SonarLintGlobalSettings.class);
    List<SonarQubeServer> servers = globalSettings.getSonarQubeServers();

    Optional<SonarQubeServer> server = servers.stream().filter(s -> s.getName().equals(serverId)).findAny();
    return server.orElseThrow(() -> new InvalidBindingException("SonarQube server configuration does not exist for server id: " + serverId));
  }

  private static void checkBindingStatus(SonarLintProjectNotifications notifications, @Nullable String serverId, @Nullable String projectKey) throws InvalidBindingException {
    if (serverId == null) {
      notifications.notifyServerIdInvalid();
      throw new InvalidBindingException("Project has an invalid binding");
    } else if (projectKey == null) {
      notifications.notifyModuleInvalid();
      throw new InvalidBindingException("Project has an invalid binding");
    }
  }
}
