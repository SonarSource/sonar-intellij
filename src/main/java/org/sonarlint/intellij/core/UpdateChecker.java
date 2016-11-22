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
package org.sonarlint.intellij.core;

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.sonarlint.intellij.config.project.SonarLintProjectSettings;
import org.sonarlint.intellij.util.SonarLintUtils;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.GlobalStorageStatus;
import org.sonarsource.sonarlint.core.client.api.connected.ModuleStorageStatus;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.StorageUpdateCheckResult;

public class UpdateChecker extends AbstractProjectComponent {

  private static final Logger LOG = Logger.getInstance(UpdateChecker.class);

  private final ProjectBindingManager projectBindingManager;
  private final SonarLintProjectSettings projectSettings;
  private final SonarLintProjectNotifications notifications;
  private ScheduledFuture<?> scheduledTask;

  public UpdateChecker(Project project, ProjectBindingManager projectBindingManager, SonarLintProjectSettings projectSettings, SonarLintProjectNotifications notifications) {
    super(project);
    this.projectBindingManager = projectBindingManager;
    this.projectSettings = projectSettings;
    this.notifications = notifications;
  }

  @Override
  public void initComponent() {
    scheduledTask = JobScheduler.getScheduler().scheduleWithFixedDelay(this::checkForUpdate, 10, 24 * 60 * 60, TimeUnit.SECONDS);
  }

  @Override
  public void projectClosed() {
    scheduledTask.cancel(true);
  }

  private void checkForUpdate() {
    try {
      if (projectSettings.isBindingEnabled()) {
        LOG.debug("Checking for updates...");
        ConnectedSonarLintEngine engine = projectBindingManager.getConnectedEngine();
        GlobalStorageStatus globalStorageStatus = engine.getGlobalStorageStatus();
        ModuleStorageStatus moduleStorageStatus = engine.getModuleStorageStatus(projectSettings.getProjectKey());
        if (globalStorageStatus == null || globalStorageStatus.isStale() || moduleStorageStatus == null || moduleStorageStatus.isStale()) {
          LOG.debug("Storage is stale. Skip updates checking.");
          return;
        }
        List<String> changelog = new ArrayList<>();
        ServerConfiguration serverConfiguration = SonarLintUtils.getServerConfiguration(projectBindingManager.getSonarQubeServer());
        boolean hasGlobalUpdates = checkForGlobalUpdates(changelog, engine, serverConfiguration);
        checkForProjectUpdates(changelog, engine, serverConfiguration);
        if (!changelog.isEmpty()) {
          notifications.notifyServerHasUpdates(projectSettings.getServerId(), changelog, engine, projectBindingManager.getSonarQubeServer(), !hasGlobalUpdates);
        }
      }
    } catch (Exception e) {
      LOG.warn("There was an error while checking for updates", e);
    }
  }

  private void checkForProjectUpdates(List<String> changelog, ConnectedSonarLintEngine engine, ServerConfiguration serverConfiguration) {
    StorageUpdateCheckResult moduleUpdateCheckResult = engine.checkIfModuleStorageNeedUpdate(serverConfiguration, projectSettings.getProjectKey(), null);
    if (moduleUpdateCheckResult.needUpdate()) {
      changelog.addAll(moduleUpdateCheckResult.changelog());
    }
  }

  private static boolean checkForGlobalUpdates(List<String> changelog, ConnectedSonarLintEngine engine, ServerConfiguration serverConfiguration) {
    StorageUpdateCheckResult checkForUpdateResult = engine.checkIfGlobalStorageNeedUpdate(serverConfiguration, null);
    if (checkForUpdateResult.needUpdate()) {
      changelog.addAll(checkForUpdateResult.changelog());
      return true;
    }
    return false;
  }

}
