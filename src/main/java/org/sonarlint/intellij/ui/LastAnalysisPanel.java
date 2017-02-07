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
package org.sonarlint.intellij.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import icons.SonarLintIcons;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import org.sonarlint.intellij.util.SonarLintUtils;

public class LastAnalysisPanel implements Disposable {
  private LocalDateTime lastAnalysis;
  private final Supplier<String> emptyTestSupplier;
  private GridBagConstraints gc;
  private Timer lastAnalysisTimeUpdater;
  private JLabel lastAnalysisLabel;
  private JLabel icon;
  private JPanel panel;

  public LastAnalysisPanel(LocalDateTime lastAnalysis, Project project, Supplier<String> emptyTestSupplier) {
    this.lastAnalysis = lastAnalysis;
    this.emptyTestSupplier = emptyTestSupplier;
    createComponents();
    setLabel();
    setTimer();
    Disposer.register(project, this);
  }

  public JPanel getPanel() {
    return panel;
  }

  public void update(LocalDateTime lastAnalysis) {
    this.lastAnalysis = lastAnalysis;
    setLabel();
  }

  private void setLabel() {
    panel.removeAll();
    if (lastAnalysis == null) {
      lastAnalysisLabel.setText(emptyTestSupplier.get());
      panel.add(icon);
      panel.add(lastAnalysisLabel, gc);
    } else {
      lastAnalysisLabel.setText("Analysis done " + SonarLintUtils.age(System.currentTimeMillis()));
      panel.add(lastAnalysisLabel, gc);
    }

    panel.add(Box.createHorizontalBox(), gc);
  }

  private void createComponents() {
    panel = new JPanel(new GridBagLayout());
    icon = new JLabel(SonarLintIcons.INFO);
    lastAnalysisLabel = new JLabel("");
    gc = new GridBagConstraints(GridBagConstraints.RELATIVE, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0);

    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.weightx = 1;
  }

  @Override
  public void dispose() {
    if (lastAnalysisTimeUpdater != null) {
      lastAnalysisTimeUpdater.stop();
      lastAnalysisTimeUpdater = null;
    }
  }

  private void setTimer() {
    lastAnalysisTimeUpdater = new Timer(5000, e -> setLabel());
  }
}
