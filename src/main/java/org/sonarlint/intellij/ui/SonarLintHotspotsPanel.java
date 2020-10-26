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
package org.sonarlint.intellij.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.Splitter;
import com.intellij.tools.SimpleActionGroup;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBTabbedPane;
import org.sonarlint.intellij.issue.hotspot.LocalHotspot;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class SonarLintHotspotsPanel extends SimpleToolWindowPanel {
  private static final String ID = "Hotspots";
  private static final String SPLIT_PROPORTION_PROPERTY = "SONARLINT_HOTSPOTS_SPLIT_PROPORTION";
  private static final float DEFAULT_SPLIT_PROPORTION = 0.5f;

  private final JBTabbedPane hotspotDetailsTab;
  private final SonarLintHotspotsListPanel hotspotsListPanel;
  private final SonarLintHotspotDescriptionPanel riskDescriptionPanel;
  private final SonarLintHotspotDescriptionPanel vulnerabilityDescriptionPanel;
  private final SonarLintHotspotDescriptionPanel fixRecommendationsPanel;
  private final SonarLintHotspotSummaryPanel summaryPanel;

  public SonarLintHotspotsPanel(Project project) {
    super(false, true);
    fillToolbar();

    hotspotsListPanel = new SonarLintHotspotsListPanel(project);
    summaryPanel = new SonarLintHotspotSummaryPanel();
    riskDescriptionPanel = new SonarLintHotspotDescriptionPanel(project);
    vulnerabilityDescriptionPanel = new SonarLintHotspotDescriptionPanel(project);
    fixRecommendationsPanel = new SonarLintHotspotDescriptionPanel(project);

    hotspotDetailsTab = new JBTabbedPane();
    hotspotDetailsTab.addTab("What's the risk ?", null, scrollable(riskDescriptionPanel.getPanel()), "Risk description");
    hotspotDetailsTab.addTab("Are you at risk ?", null, scrollable(vulnerabilityDescriptionPanel.getPanel()), "Vulnerability description");
    hotspotDetailsTab.addTab("How can you fix it ?", null, scrollable(fixRecommendationsPanel.getPanel()), "Recommendations");
    hotspotDetailsTab.addTab("Summary", null, scrollable(summaryPanel.getPanel()), "Details about the hotspot");
    hotspotDetailsTab.setVisible(false);

    super.setContent(createSplitter(hotspotsListPanel.getPanel(), hotspotDetailsTab, SPLIT_PROPORTION_PROPERTY, project));
  }

  private static JScrollPane scrollable(JComponent component) {
    JScrollPane scrollableRulePanel = ScrollPaneFactory.createScrollPane(
      component,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollableRulePanel.getVerticalScrollBar().setUnitIncrement(10);
    return scrollableRulePanel;
  }

  private void fillToolbar() {
    ActionToolbar mainToolbar = ActionManager.getInstance().createActionToolbar(ID, createActionGroup(), false);
    mainToolbar.setTargetComponent(this);
    Box toolBarBox = Box.createHorizontalBox();
    toolBarBox.add(mainToolbar.getComponent());
    super.setToolbar(toolBarBox);
    mainToolbar.getComponent().setVisible(true);
  }

  private static ActionGroup createActionGroup() {
    SimpleActionGroup actionGroup = new SimpleActionGroup();
    actionGroup.add(ActionManager.getInstance().getAction("SonarLint.OpenHotspot"));
    return actionGroup;
  }

  protected JComponent createSplitter(JComponent c1, JComponent c2, String proportionProperty, Project project) {
    float savedProportion = PropertiesComponent.getInstance(project).getFloat(proportionProperty, DEFAULT_SPLIT_PROPORTION);

    final Splitter splitter = new Splitter(false);
    splitter.setFirstComponent(c1);
    splitter.setSecondComponent(c2);
    splitter.setProportion(savedProportion);
    splitter.setHonorComponentsMinimumSize(true);
    splitter.addPropertyChangeListener(Splitter.PROP_PROPORTION,
      evt -> PropertiesComponent.getInstance(project).setValue(proportionProperty, Float.toString(splitter.getProportion())));

    return splitter;
  }

  public void setHotspot(LocalHotspot hotspot) {
    hotspotDetailsTab.setVisible(true);
    hotspotsListPanel.setHotspot(hotspot);
    riskDescriptionPanel.setDescription(hotspot.remote.rule.riskDescription);
    vulnerabilityDescriptionPanel.setDescription(hotspot.remote.rule.vulnerabilityDescription);
    fixRecommendationsPanel.setDescription(hotspot.remote.rule.fixRecommendations);
    summaryPanel.setDetails(hotspot);
  }

}
