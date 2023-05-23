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
package org.sonarlint.intellij.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.tools.SimpleActionGroup;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;
import org.sonarlint.intellij.analysis.AnalysisResult;
import org.sonarlint.intellij.common.util.SonarLintUtils;
import org.sonarlint.intellij.editor.EditorDecorator;
import org.sonarlint.intellij.finding.LiveFinding;
import org.sonarlint.intellij.messages.StatusListener;
import org.sonarlint.intellij.trigger.TriggerType;
import org.sonarlint.intellij.ui.nodes.IssueNode;
import org.sonarlint.intellij.ui.nodes.LiveSecurityHotspotNode;
import org.sonarlint.intellij.ui.tree.FlowsTree;
import org.sonarlint.intellij.ui.tree.FlowsTreeModelBuilder;
import org.sonarlint.intellij.ui.tree.IssueTree;
import org.sonarlint.intellij.ui.tree.IssueTreeModelBuilder;
import org.sonarlint.intellij.ui.tree.SecurityHotspotTree;
import org.sonarlint.intellij.ui.tree.SecurityHotspotTreeModelBuilder;
import org.sonarlint.intellij.util.SonarLintActions;
import org.sonarsource.sonarlint.core.clientapi.backend.hotspot.HotspotStatus;

import static org.sonarlint.intellij.ui.SonarLintToolWindowFactory.createSplitter;
import static org.sonarlint.intellij.ui.UiUtils.runOnUiThread;

public class ReportPanel extends SimpleToolWindowPanel implements Disposable {
  private static final String SPLIT_PROPORTION_PROPERTY = "SONARLINT_ANALYSIS_RESULTS_SPLIT_PROPORTION";
  private static final String ID = "SonarLint";
  private static final int RULE_TAB_INDEX = 0;
  private static final int LOCATIONS_TAB_INDEX = 1;
  protected final Project project;
  private final LastAnalysisPanel lastAnalysisPanel;
  protected SonarLintRulePanel rulePanel;
  protected JBTabbedPane detailsTab;
  protected Tree tree;
  protected IssueTreeModelBuilder treeBuilder;
  protected FlowsTree flowsTree;
  protected FlowsTreeModelBuilder flowsTreeBuilder;
  private ActionToolbar mainToolbar;
  protected SecurityHotspotTreeModelBuilder securityHotspotTreeBuilder;
  protected Tree securityHotspotTree;
  private JScrollPane findingsTreePane;

  public ReportPanel(Project project) {
    super(false, true);
    this.project = project;
    this.lastAnalysisPanel = new LastAnalysisPanel();

    createFlowsTree();
    createIssuesTree();
    createSecurityHotspotsTree();
    createTabs();
    handleListener();
    disableSecurityHotspotTree();

    initPanel();

    // Subscribe to events
    subscribeToEvents();
  }

  public void updateFindings(AnalysisResult analysisResult) {
    if (project.isDisposed()) {
      return;
    }
    lastAnalysisPanel.update(analysisResult.getAnalysisDate(), whatAnalyzed(analysisResult));
    var findings = analysisResult.getFindings();
    treeBuilder.updateModel(findings.getIssuesPerFile(), "No issues found");
    securityHotspotTreeBuilder.updateModel(findings.getSecurityHotspotsPerFile(), "No Security Hotspots found");

    disableEmptyDisplay(true);

    if (findings.getSecurityHotspotsPerFile().isEmpty()) {
      disableSecurityHotspotTree();
    } else {
      enableHotspotTree();
    }

    expandTree();
  }

  public void updateStatusForSecurityHotspot(String securityHotspotKey, HotspotStatus status) {
    var wasUpdated = securityHotspotTreeBuilder.updateStatusForHotspotWithFileNode(securityHotspotKey, status);
    if (wasUpdated) {
      expandTree();
    }
  }

  private void initPanel() {
    // Findings panel with tree
    var findingsPanel = new JBPanelWithEmptyText(new BorderLayout());

    initEmptyViews(findingsPanel);

    var treePanel = new JBPanel<ReportPanel>(new VerticalFlowLayout(0, 0));
    treePanel.add(tree);
    treePanel.add(securityHotspotTree);
    findingsTreePane = ScrollPaneFactory.createScrollPane(treePanel);
    findingsPanel.add(findingsTreePane, BorderLayout.CENTER);
    findingsPanel.add(lastAnalysisPanel, BorderLayout.SOUTH);
    setToolbar(createActionGroup());
    disableEmptyDisplay(false);

    // Put everything together
    super.setContent(createSplitter(project, this, this, findingsPanel, detailsTab, SPLIT_PROPORTION_PROPERTY, 0.5f));
  }

  private void initEmptyViews(JBPanelWithEmptyText findingsPanel) {
    var statusText = findingsPanel.getEmptyText();
    var sonarLintActions = SonarLintActions.getInstance();
    var analyzeChangedFiles = sonarLintActions.analyzeChangedFiles();
    var analyzeAllFiles = sonarLintActions.analyzeAllFiles();
    statusText.setText(LastAnalysisPanel.NEVER_ANALYZED_EMPTY_TEXT);
    statusText.appendLine("");
    if (analyzeChangedFiles.getTemplateText() != null) {
      statusText.appendText(analyzeChangedFiles.getTemplateText(), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES,
        ignore -> ActionUtil.invokeAction(analyzeChangedFiles, this, CurrentFilePanel.SONARLINT_TOOLWINDOW_ID, null, null));
      if (analyzeAllFiles.getTemplateText() != null) {
        statusText.appendText(" or ");
      }
    }
    if (analyzeAllFiles.getTemplateText() != null) {
      statusText.appendText(analyzeAllFiles.getTemplateText(), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES,
        ignore -> ActionUtil.invokeAction(analyzeAllFiles, this, CurrentFilePanel.SONARLINT_TOOLWINDOW_ID, null, null));
    }
  }

  private void refreshToolbar() {
    mainToolbar.updateActionsImmediately();
  }

  private void enableHotspotTree() {
    tree.setShowsRootHandles(true);
    securityHotspotTree.setShowsRootHandles(true);
    securityHotspotTree.setVisible(true);
  }

  private void disableSecurityHotspotTree() {
    tree.setShowsRootHandles(false);
    securityHotspotTree.setShowsRootHandles(false);
    securityHotspotTree.setVisible(false);
  }

  private void issueTreeSelectionChanged(TreeSelectionEvent e) {
    if (!tree.isSelectionEmpty()) {
      securityHotspotTree.clearSelection();
    }
    var selectedIssueNodes = tree.getSelectedNodes(IssueNode.class, null);
    if (selectedIssueNodes.length > 0) {
      updateOnSelect(selectedIssueNodes[0].issue());
    } else {
      clearSelectionChanged(false);
    }
  }

  private void securityHotspotTreeSelectionChanged(TreeSelectionEvent e) {
    if (e.getSource() instanceof SecurityHotspotTree) {
      if (!securityHotspotTree.isSelectionEmpty()) {
        tree.clearSelection();
      }
      var selectedHotspotsNodes = securityHotspotTree.getSelectedNodes(LiveSecurityHotspotNode.class, null);
      if (selectedHotspotsNodes.length > 0) {
        updateOnSelect(selectedHotspotsNodes[0].getHotspot());
      } else {
        clearSelectionChanged(false);
      }
    }
  }

  private void updateOnSelect(LiveFinding liveFinding) {
    var moduleForFile = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(liveFinding.psiFile().getVirtualFile());

    if (moduleForFile == null) {
      clearSelectionChanged(true);
      return;
    }

    rulePanel.setSelectedFinding(moduleForFile, liveFinding);
    SonarLintUtils.getService(project, EditorDecorator.class).highlightFinding(liveFinding);
    flowsTree.getEmptyText().setText("Selected finding doesn't have flows");
    flowsTreeBuilder.populateForFinding(liveFinding);
    flowsTree.expandAll();
  }

  private void setToolbar(ActionGroup group) {
    if (mainToolbar != null) {
      mainToolbar.setTargetComponent(null);
      super.setToolbar(null);
      mainToolbar = null;
    }
    mainToolbar = ActionManager.getInstance().createActionToolbar(ID, group, false);
    mainToolbar.setTargetComponent(this);
    var toolBarBox = Box.createHorizontalBox();
    toolBarBox.add(mainToolbar.getComponent());
    super.setToolbar(toolBarBox);
    mainToolbar.getComponent().setVisible(true);
  }

  private static SimpleActionGroup createActionGroup() {
    var sonarLintActions = SonarLintActions.getInstance();
    var actionGroup = new SimpleActionGroup();
    actionGroup.add(sonarLintActions.analyzeChangedFiles());
    actionGroup.add(sonarLintActions.analyzeAllFiles());
    actionGroup.add(sonarLintActions.cancelAnalysis());
    actionGroup.add(sonarLintActions.configure());
    actionGroup.add(sonarLintActions.clearReport());
    return actionGroup;
  }

  private String whatAnalyzed(AnalysisResult analysisResult) {
    var trigger = analysisResult.getTriggerType();
    if (TriggerType.ALL.equals(trigger)) {
      return "all project files";
    }
    if (TriggerType.CHANGED_FILES.equals(trigger)) {
      return "SCM changed files";
    }
    var filesCount = analysisResult.getAnalyzedFiles().size();
    if (filesCount == 1) {
      return "1 file";
    }
    return filesCount + " files";
  }

  private void handleListener() {
    tree.addTreeSelectionListener(this::issueTreeSelectionChanged);
    securityHotspotTree.addTreeSelectionListener(this::securityHotspotTreeSelectionChanged);
  }

  private void createSecurityHotspotsTree() {
    securityHotspotTreeBuilder = new SecurityHotspotTreeModelBuilder();
    var model = securityHotspotTreeBuilder.createModel();
    securityHotspotTree = new SecurityHotspotTree(project, model);
    manageInteraction(securityHotspotTree);
  }

  private void createTabs() {
    // Flows panel with tree
    var flowsPanel = ScrollPaneFactory.createScrollPane(flowsTree, true);
    flowsPanel.getVerticalScrollBar().setUnitIncrement(10);

    // Rule panel
    rulePanel = new SonarLintRulePanel(project, this);

    detailsTab = new JBTabbedPane();
    detailsTab.insertTab("Rule", null, rulePanel, "Details about the rule", RULE_TAB_INDEX);
    detailsTab.insertTab("Locations", null, flowsPanel, "All locations involved in the finding", LOCATIONS_TAB_INDEX);
  }

  private void clearSelectionChanged(boolean isModuleMissing) {
    flowsTreeBuilder.clearFlows();
    if (isModuleMissing) {
      flowsTree.getEmptyText().setText("Finding location has been deleted");
      rulePanel.clearDeletedFile();
    } else {
      flowsTree.getEmptyText().setText("No finding selected");
      rulePanel.clear();
    }
    var highlighting = SonarLintUtils.getService(project, EditorDecorator.class);
    highlighting.removeHighlights();
  }

  private void createFlowsTree() {
    flowsTreeBuilder = new FlowsTreeModelBuilder();
    var model = flowsTreeBuilder.createModel();
    flowsTree = new FlowsTree(project, model);
    flowsTreeBuilder.clearFlows();
    flowsTree.getEmptyText().setText("No finding selected");
  }

  private void createIssuesTree() {
    treeBuilder = new IssueTreeModelBuilder();
    var model = treeBuilder.createModel();
    tree = new IssueTree(project, model);
    manageInteraction(tree);
  }

  private void manageInteraction(Tree tree) {
    tree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
          var highlighting = SonarLintUtils.getService(project, EditorDecorator.class);
          highlighting.removeHighlights();
        }
      }
    });
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
  }

  private void subscribeToEvents() {
    var busConnection = project.getMessageBus().connect(project);
    busConnection.subscribe(StatusListener.SONARLINT_STATUS_TOPIC,
      newStatus -> runOnUiThread(project, this::refreshToolbar));
  }

  public void clear() {
    if (project.isDisposed()) {
      return;
    }
    lastAnalysisPanel.clear();
    treeBuilder.clear();
    securityHotspotTreeBuilder.clear();
    disableEmptyDisplay(false);
  }

  private void expandTree() {
    if (treeBuilder.numberIssues() < 30) {
      TreeUtil.expandAll(tree);
    } else {
      tree.expandRow(0);
    }

    if (securityHotspotTreeBuilder.numberHotspots() < 30) {
      TreeUtil.expandAll(securityHotspotTree);
    } else {
      securityHotspotTree.expandRow(0);
    }
  }

  private void disableEmptyDisplay(Boolean state) {
    findingsTreePane.setVisible(state);
    lastAnalysisPanel.setVisible(state);
  }

  @Override
  // called automatically because the panel is one of the content of the tool window
  public void dispose() {
    lastAnalysisPanel.dispose();
  }

}
