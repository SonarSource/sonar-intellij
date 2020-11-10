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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import java.time.ZonedDateTime;
import javax.swing.event.HyperlinkEvent;
import org.jetbrains.annotations.NotNull;
import org.sonarlint.intellij.config.global.SonarLintGlobalSettings;
import org.sonarlint.intellij.config.global.SonarQubeServer;
import org.sonarlint.intellij.config.project.SonarLintProjectSettings;
import org.sonarlint.intellij.config.project.SonarLintProjectState;
import org.sonarlint.intellij.exception.InvalidBindingException;
import org.sonarlint.intellij.messages.GlobalConfigurationListener;
import org.sonarlint.intellij.messages.ProjectConfigurationListener;
import org.sonarlint.intellij.telemetry.SonarLintTelemetry;
import org.sonarlint.intellij.util.SonarLintUtils;
import org.sonarsource.sonarlint.core.client.api.common.NotificationConfiguration;
import org.sonarsource.sonarlint.core.client.api.notifications.LastNotificationTime;
import org.sonarsource.sonarlint.core.client.api.notifications.ServerNotification;
import org.sonarsource.sonarlint.core.client.api.notifications.ServerNotificationListener;

import static org.sonarlint.intellij.config.Settings.getSettingsFor;

public class ProjectServerNotifications {
  private static final NotificationGroup SONARQUBE_GROUP = NotificationGroup.balloonGroup("SonarLint: SonarQube Events");
  private final EventListener eventListener;
  private final ProjectNotificationTime notificationTime;
  private final MessageBusConnection busConnection;
  private final Project myProject;

  public ProjectServerNotifications(Project project) {
    myProject = project;
    this.eventListener = new EventListener();
    this.notificationTime = new ProjectNotificationTime();
    this.busConnection = project.getMessageBus().connect(myProject);
  }

  public void init() {
    register();
    busConnection.subscribe(ProjectConfigurationListener.TOPIC, settings -> {
      // always reset notification date, whether bound or not
      SonarLintProjectState projectState = SonarLintUtils.getService(myProject, SonarLintProjectState.class);
      projectState.setLastEventPolling(ZonedDateTime.now());
      register();
    });
    busConnection.subscribe(GlobalConfigurationListener.TOPIC, new GlobalConfigurationListener.Adapter() {
      @Override public void applied(SonarLintGlobalSettings settings) {
        register();
      }
    });
  }


  private void register() {
    SonarLintProjectSettings settings = getSettingsFor(myProject);
    unregister();
    if (settings.isBindingEnabled()) {
      SonarQubeServer server;
      try {
        ProjectBindingManager bindingManager = SonarLintUtils.getService(myProject, ProjectBindingManager.class);
        server = bindingManager.getSonarQubeServer();
      } catch (InvalidBindingException e) {
        // do nothing
        return;
      }
      if (server.enableNotifications()) {
        NotificationConfiguration config = createConfiguration(settings, server);
        ServerNotificationsFacade.get().register(config);
      }
    }
  }

  public void unregister() {
    ServerNotificationsFacade.get().unregister(eventListener);
  }

  private NotificationConfiguration createConfiguration(SonarLintProjectSettings settings, SonarQubeServer server) {
    String projectKey = settings.getProjectKey();
    return new NotificationConfiguration(eventListener, notificationTime, projectKey, () -> SonarLintUtils.getServerConfiguration(server));
  }

  /**
   * Read and save directly from the mutable object.
   * Any changes in the project settings will affect the next request.
   */
  private class ProjectNotificationTime implements LastNotificationTime {

    @Override public ZonedDateTime get() {
      SonarLintProjectState projectState = SonarLintUtils.getService(myProject, SonarLintProjectState.class);
      ZonedDateTime lastEventPolling = projectState.getLastEventPolling();
      if (lastEventPolling == null) {
        lastEventPolling = ZonedDateTime.now();
        projectState.setLastEventPolling(lastEventPolling);
      }
      return lastEventPolling;
    }

    @Override public void set(ZonedDateTime dateTime) {
      SonarLintProjectState projectState = SonarLintUtils.getService(myProject, SonarLintProjectState.class);
      ZonedDateTime lastEventPolling = projectState.getLastEventPolling();
      if (lastEventPolling != null && dateTime.isBefore(lastEventPolling)) {
        // this can happen if the settings changed between the read and write
        return;
      }
      projectState.setLastEventPolling(dateTime);
    }
  }

  /**
   * Simply displays the events and discards it
   */
  private class EventListener implements ServerNotificationListener {
    @Override public void handle(ServerNotification notification) {
      SonarLintTelemetry telemetry = SonarLintUtils.getService(SonarLintTelemetry.class);
      telemetry.devNotificationsReceived();
      Notification notif = SONARQUBE_GROUP.createNotification(
        "<b>SonarQube event</b>",
        createMessage(notification),
        NotificationType.INFORMATION,
        new NotificationListener.UrlOpeningListener(true) {
          @Override
          protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
            SonarLintTelemetry telemetry = SonarLintUtils.getService(SonarLintTelemetry.class);
            telemetry.devNotificationsClicked();
            super.hyperlinkActivated(notification, event);
          }
        });
      notif.setImportant(true);
      notif.notify(myProject);
    }

    private String createMessage(ServerNotification notification) {
      return notification.message() + ".&nbsp;<a href=\"" + notification.link() + "\">Check it here</a>.";
    }
  }
}
