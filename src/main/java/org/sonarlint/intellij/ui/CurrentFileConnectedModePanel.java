/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2022 SonarSource
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
package org.sonarlint.intellij.ui;

import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.TooltipWithClickableLinks;
import icons.SonarLintIcons;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.sonarlint.intellij.common.util.SonarLintUtils;
import org.sonarlint.intellij.config.global.ServerConnection;
import org.sonarlint.intellij.core.ModuleBindingManager;
import org.sonarlint.intellij.core.ProjectBindingManager;
import org.sonarlint.intellij.exception.InvalidBindingException;
import org.sonarlint.intellij.util.SonarLintActions;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class CurrentFileConnectedModePanel {

  private static final String CONNECTED_MODE_DOCUMENTATION_URL = "https://github.com/SonarSource/sonarlint-intellij/wiki/Bind-to-SonarQube-or-SonarCloud";

  private static final String NOT_CONNECTED = "Not Connected";
  private static final String CONNECTED = "Connected";
  private final Project project;

  private JPanel panel;
  private CardLayout layout;
  private JLabel connectedCard;

  CurrentFileConnectedModePanel(Project project) {
    this.project = project;
    createPanel();
    switchCards();
    CurrentFileStatusPanel.subscribeToEventsThatAffectCurrentFile(project, this::switchCards);
    // TODO Also subscribe to branch management service event
  }

  private void createPanel() {
    layout = new CardLayout();
    panel = new JPanel(layout);

    var notConnectedCard = new JLabel(SonarLintIcons.NOT_CONNECTED);
    var notConnectedTooltip = new TooltipWithClickableLinks.ForBrowser(notConnectedCard,
      "<h3>Not Connected</h3>" +
      "<p>Click to synchronize your project with SonarQube or SonarCloud.</p>" +
      "<p><a href=\"" + CONNECTED_MODE_DOCUMENTATION_URL + "\">Learn More</a></p>"
    );
    IdeTooltipManager.getInstance().setCustomTooltip(notConnectedCard, notConnectedTooltip);

    connectedCard = new JLabel(SonarLintIcons.CONNECTED);

    panel.add(notConnectedCard, NOT_CONNECTED);
    panel.add(connectedCard, CONNECTED);
    panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    layout.show(panel, NOT_CONNECTED);
    var clickListener = new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        ActionManager.getInstance().tryToExecute(
          SonarLintActions.getInstance().configure(), e, panel, null, true
        );
      }
    };
    notConnectedCard.addMouseListener(clickListener);
    connectedCard.addMouseListener(clickListener);
  }

  private void switchCards() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    var selectedFile = SonarLintUtils.getSelectedFile(project);
    if (selectedFile != null) {
      // Checking connected mode state may take time, so lets move from EDT to pooled thread
      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        var projectBindingManager = SonarLintUtils.getService(project, ProjectBindingManager.class);
        try {
          var serverConnection = projectBindingManager.getServerConnection();
          var module = ModuleUtilCore.findModuleForFile(selectedFile, project);
          var projectKey = SonarLintUtils.getService(module, ModuleBindingManager.class).resolveProjectKey();
          // TODO Get actual branch name from service
          var branchName = "master";
          var connectedTooltip = new TooltipWithClickableLinks.ForBrowser(connectedCard, buildTooltipHtml(serverConnection, projectKey, branchName));
          IdeTooltipManager.getInstance().setCustomTooltip(connectedCard, connectedTooltip);
          switchCard(CONNECTED);
        } catch (InvalidBindingException e) {
          switchCard(NOT_CONNECTED);
        }
      });
    } else {
      switchCard(NOT_CONNECTED);
    }
  }

  private static String buildTooltipHtml(ServerConnection serverConnection, String projectKey, String branchName) {
    var projectOverviewUrl =
      String.format("%s/dashboard?id=%s&branch=%s",
        serverConnection.getHostUrl(),
        URLEncoder.encode(projectKey, StandardCharsets.UTF_8),
        URLEncoder.encode(branchName, StandardCharsets.UTF_8)
      );
    return String.format(
      "<h3>Connected to %s</h3>" +
      "<p>Bound to project '%s' on connection '%s'</p>" +
      "<p>Synchronized with branch '%s'</p>" +
      "<p><a href=\"%s\">Open Project Overview</a></p>",
      serverConnection.getProductName(), escapeHtml(projectKey), escapeHtml(serverConnection.getName()), escapeHtml(branchName), projectOverviewUrl);
  }

  private void switchCard(String cardName) {
    GuiUtils.invokeLaterIfNeeded(() -> layout.show(panel, cardName), ModalityState.defaultModalityState());
  }

  JPanel getPanel() {
    return panel;
  }
}
