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
package org.sonarlint.intellij.ui.tree;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.tree.DefaultTreeModel;
import org.sonarlint.intellij.actions.filters.SecurityHotspotFilters;
import org.sonarlint.intellij.common.util.SonarLintUtils;
import org.sonarlint.intellij.editor.CodeAnalyzerRestarter;
import org.sonarlint.intellij.finding.hotspot.LiveSecurityHotspot;
import org.sonarlint.intellij.ui.nodes.AbstractNode;
import org.sonarlint.intellij.ui.nodes.FileNode;
import org.sonarlint.intellij.ui.nodes.LiveSecurityHotspotNode;
import org.sonarlint.intellij.ui.nodes.SummaryNode;
import org.sonarsource.sonarlint.core.clientapi.backend.hotspot.HotspotStatus;
import org.sonarsource.sonarlint.core.commons.VulnerabilityProbability;

/**
 * Responsible for maintaining the tree model and send change events when needed.
 * Should be optimized to minimize the recreation of portions of the tree.
 *
 * There are 2 implementations within this class
 * - Security Hotspots within a file node (used for the report tab)
 * - Security Hotspots directly child of the summary node (used for the security hotspots tab)
 *
 * In the report tab, there is no filtering mechanism, the nodes are simply deleted when needed
 * In the Security Hotspots tab, there is a filtering mechanism that hides or not some nodes
 */
public class SecurityHotspotTreeModelBuilder implements FindingTreeModelBuilder {
  private static final List<VulnerabilityProbability> VULNERABILITY_PROBABILITIES = List.of(VulnerabilityProbability.HIGH,
    VulnerabilityProbability.MEDIUM, VulnerabilityProbability.LOW);
  private static final Comparator<LiveSecurityHotspot> SECURITY_HOTSPOT_COMPARATOR = new SecurityHotspotComparator();
  private static final Comparator<LiveSecurityHotspotNode> SECURITY_HOTSPOT_WITHOUT_FILE_COMPARATOR = new LiveSecurityHotspotNodeComparator();

  private final FindingTreeIndex index;
  private SecurityHotspotFilters currentFilter = SecurityHotspotFilters.DEFAULT_FILTER;
  private DefaultTreeModel model;
  private SummaryNode summary;
  private List<LiveSecurityHotspotNode> nonFilteredNodes;
  private List<LiveSecurityHotspotNode> filteredNodes;

  public SecurityHotspotTreeModelBuilder() {
    this.index = new FindingTreeIndex();
  }

  /**
   * Creates the model with a basic root
   */
  public DefaultTreeModel createModel() {
    summary = new SummaryNode(true);
    model = new DefaultTreeModel(summary);
    model.setRoot(summary);
    nonFilteredNodes = new ArrayList<>();
    filteredNodes = new ArrayList<>();
    return model;
  }

  public int numberHotspots() {
    return summary.getFindingCount();
  }

  private SummaryNode getFilesParent() {
    return summary;
  }

  public void updateModel(Map<VirtualFile, Collection<LiveSecurityHotspot>> map, String emptyText) {
    summary.setEmptyText(emptyText);

    var toRemove = index.getAllFiles().stream().filter(f -> !map.containsKey(f)).collect(Collectors.toList());

    nonFilteredNodes.clear();
    toRemove.forEach(this::removeFile);

    for (var e : map.entrySet()) {
      setFileSecurityHotspots(e.getKey(), e.getValue());
    }

    model.nodeChanged(summary);
  }

  private void setFileSecurityHotspots(VirtualFile file, Iterable<LiveSecurityHotspot> securityHotspots) {
    if (!accept(file)) {
      removeFile(file);
      return;
    }

    var filtered = filter(securityHotspots, false);
    if (filtered.isEmpty()) {
      removeFile(file);
      return;
    }

    var newFile = false;
    var fNode = index.getFileNode(file);
    if (fNode == null) {
      newFile = true;
      fNode = new FileNode(file, true);
      index.setFileNode(fNode);
    }

    setFileNodeSecurityHotspots(fNode, filtered);

    if (newFile) {
      var parent = getFilesParent();
      var idx = parent.insertFileNode(fNode, new FileNodeComparator());
      var newIdx = new int[] {idx};
      model.nodesWereInserted(parent, newIdx);
      model.nodeChanged(parent);
    } else {
      model.nodeStructureChanged(fNode);
    }
  }

  private void setFileNodeSecurityHotspots(FileNode node, Iterable<LiveSecurityHotspot> securityHotspotsPointer) {
    node.removeAllChildren();

    var securityHotspots = new TreeSet<>(SECURITY_HOTSPOT_COMPARATOR);

    for (var securityHotspot : securityHotspotsPointer) {
      securityHotspots.add(securityHotspot);
    }

    for (var securityHotspot : securityHotspots) {
      var iNode = new LiveSecurityHotspotNode(securityHotspot, false);
      node.add(iNode);

      nonFilteredNodes.add(iNode);
    }
  }

  private void removeFile(VirtualFile file) {
    var node = index.getFileNode(file);

    if (node != null) {
      index.remove(node.file());
      model.removeNodeFromParent(node);
    }
  }

  public LiveSecurityHotspot findHotspotByKey(String securityHotspotKey) {
    var nodes = summary.children();
    while (nodes.hasMoreElements()) {
      var securityHotspotNode = (LiveSecurityHotspotNode) nodes.nextElement();
      if (securityHotspotKey.equals(securityHotspotNode.getHotspot().getServerFindingKey())) {
        return securityHotspotNode.getHotspot();
      }
    }
    return null;
  }

  public int updateModelWithoutFileNode(Map<VirtualFile, Collection<LiveSecurityHotspot>> map, String emptyText) {
    summary.setEmptyText(emptyText);
    summary.removeAllChildren();

    for (var e : map.entrySet()) {
      setSecurityHotspots(e.getKey(), e.getValue());
    }

    copyToFilteredNodes();
    model.nodeChanged(summary);

    return summary.getFindingCount();
  }

  private void setSecurityHotspots(VirtualFile file, Iterable<LiveSecurityHotspot> securityHotspots) {
    if (!accept(file)) {
      return;
    }

    var filtered = filter(securityHotspots, true);
    if (filtered.isEmpty()) {
      return;
    }

    setRootSecurityHotspots(filtered);
  }

  private void setRootSecurityHotspots(Iterable<LiveSecurityHotspot> securityHotspotsPointer) {
    for (var securityHotspot : securityHotspotsPointer) {
      var iNode = new LiveSecurityHotspotNode(securityHotspot, true);
      var idx = summary.insertLiveSecurityHotspotNode(iNode, SECURITY_HOTSPOT_WITHOUT_FILE_COMPARATOR);
      var newIdx = new int[] {idx};
      model.nodesWereInserted(summary, newIdx);
      model.nodeChanged(summary);
    }
  }

  private void copyToFilteredNodes() {
    nonFilteredNodes.clear();
    Collections.list(summary.children()).forEach(e -> {
      var securityHotspotNode = (LiveSecurityHotspotNode) e;
      nonFilteredNodes.add((LiveSecurityHotspotNode) securityHotspotNode.clone());
    });
  }

  public int applyCurrentFiltering(Project project) {
    return filterSecurityHotspots(project, currentFilter);
  }

  public int updateStatusAndApplyCurrentFiltering(Project project, String securityHotspotKey, HotspotStatus status) {
    for (var securityHotspotNode : nonFilteredNodes) {
      if (securityHotspotKey.equals(securityHotspotNode.getHotspot().getServerFindingKey())) {
        securityHotspotNode.getHotspot().setStatus(status);
        break;
      }
    }

    return filterSecurityHotspots(project, currentFilter);
  }

  public boolean updateStatusForHotspotWithFileNode(String securityHotspotKey, HotspotStatus status) {
    var optionalNode = nonFilteredNodes
      .stream()
      .filter(node -> securityHotspotKey.equals(node.getHotspot().getServerFindingKey()))
      .findFirst();

    if (optionalNode.isPresent()) {
      var hotspotNode = optionalNode.get();
      var hotspot = hotspotNode.getHotspot();
      hotspot.setStatus(status);
      if (hotspot.isResolved()) {
        var fileNode = (FileNode) hotspotNode.getParent();
        fileNode.remove(hotspotNode);
        if (fileNode.getFindingCount() == 0) {
          index.remove(fileNode.file());
          summary.remove(fileNode);
        }
      }
      model.reload();
      return true;
    }
    return false;
  }

  public Collection<LiveSecurityHotspotNode> getFilteredNodes() {
    return filteredNodes;
  }

  private Collection<VirtualFile> getFilesForNodes() {
    return nonFilteredNodes.stream().map(LiveSecurityHotspotNode::getHotspot).map(LiveSecurityHotspot::getFile).collect(Collectors.toSet());
  }

  public int filterSecurityHotspots(Project project, SecurityHotspotFilters filter) {
    var fileList = getFilesForNodes();
    currentFilter = filter;
    filteredNodes.clear();
    Collections.list(summary.children()).forEach(e -> model.removeNodeFromParent((LiveSecurityHotspotNode) e));
    for (var securityHotspotNode : nonFilteredNodes) {
      if (filter.shouldIncludeSecurityHotspot(securityHotspotNode.getHotspot())) {
        fileList.add(securityHotspotNode.getHotspot().getFile());
        var idx = summary.insertLiveSecurityHotspotNode(securityHotspotNode, SECURITY_HOTSPOT_WITHOUT_FILE_COMPARATOR);
        var newIdx = new int[] {idx};
        model.nodesWereInserted(summary, newIdx);
        model.nodeChanged(summary);
        filteredNodes.add(securityHotspotNode);
      }
    }

    model.reload();
    SonarLintUtils.getService(project, CodeAnalyzerRestarter.class).refreshFiles(fileList);
    return filteredNodes.size();
  }

  public void clear() {
    updateModel(Collections.emptyMap(), "No analysis done");
  }

  private static List<LiveSecurityHotspot> filter(Iterable<LiveSecurityHotspot> securityHotspots, boolean enableResolved) {
    return StreamSupport.stream(securityHotspots.spliterator(), false)
      .filter(hotspot -> accept(hotspot, enableResolved))
      .collect(Collectors.toList());
  }

  private static boolean accept(LiveSecurityHotspot securityHotspot, boolean enableResolved) {
    if (enableResolved) {
      return securityHotspot.isValid();
    } else {
      return !securityHotspot.isResolved() && securityHotspot.isValid();
    }
  }

  private static boolean accept(VirtualFile file) {
    return file.isValid();
  }

  private static class FileNodeComparator implements Comparator<FileNode> {
    @Override
    public int compare(FileNode o1, FileNode o2) {
      int c = o1.file().getName().compareTo(o2.file().getName());
      if (c != 0) {
        return c;
      }

      return o1.file().getPath().compareTo(o2.file().getPath());
    }
  }

  static class LiveSecurityHotspotNodeComparator implements Comparator<LiveSecurityHotspotNode> {
    @Override
    public int compare(LiveSecurityHotspotNode o1, LiveSecurityHotspotNode o2) {
      int c = o1.getHotspot().getVulnerabilityProbability().compareTo(o2.getHotspot().getVulnerabilityProbability());
      if (c != 0) {
        return c;
      }

      var r1 = o1.getHotspot().getRange();
      var r2 = o2.getHotspot().getRange();

      var rangeStart1 = (r1 == null) ? -1 : r1.getStartOffset();
      var rangeStart2 = (r2 == null) ? -1 : r2.getStartOffset();

      return ComparisonChain.start()
        .compare(o1.getHotspot().getFile().getPath(), o2.getHotspot().getFile().getPath())
        .compare(rangeStart1, rangeStart2)
        .compare(o1.getHotspot().getRuleKey(), o2.getHotspot().getRuleKey())
        .compare(o1.getHotspot().uid(), o2.getHotspot().uid())
        .result();
    }
  }

  static class SecurityHotspotComparator implements Comparator<LiveSecurityHotspot> {
    @Override
    public int compare(@Nonnull LiveSecurityHotspot o1, @Nonnull LiveSecurityHotspot o2) {
      var vulnerabilityCompare = Ordering.explicit(VULNERABILITY_PROBABILITIES)
        .compare(o1.getVulnerabilityProbability(), o2.getVulnerabilityProbability());

      if (vulnerabilityCompare != 0) {
        return vulnerabilityCompare;
      }

      var r1 = o1.getRange();
      var r2 = o2.getRange();

      var rangeStart1 = (r1 == null) ? -1 : r1.getStartOffset();
      var rangeStart2 = (r2 == null) ? -1 : r2.getStartOffset();

      return ComparisonChain.start()
        .compare(rangeStart1, rangeStart2)
        .compare(o1.getRuleKey(), o2.getRuleKey())
        .compare(o1.uid(), o2.uid())
        .result();
    }
  }

  @CheckForNull
  public LiveSecurityHotspotNode getNextHotspot(AbstractNode startNode) {
    if (!(startNode instanceof LiveSecurityHotspotNode)) {
      return firstHotspotDown(startNode);
    }

    var next = getNextNode(startNode);

    if (next == null) {
      // no next node in the entire tree
      return null;
    }

    if (next instanceof LiveSecurityHotspotNode) {
      return (LiveSecurityHotspotNode) next;
    }

    return firstHotspotDown(next);
  }

  @CheckForNull
  public LiveSecurityHotspotNode getPreviousHotspot(AbstractNode startNode) {
    var next = getPreviousNode(startNode);

    if (next == null) {
      // no next node in the entire tree
      return null;
    }

    if (next instanceof LiveSecurityHotspotNode) {
      return (LiveSecurityHotspotNode) next;
    }

    return lastHotspotDown(next);
  }

  /**
   * Finds the first Security Hotspot node which is child of a given node.
   */
  @CheckForNull
  private static LiveSecurityHotspotNode firstHotspotDown(AbstractNode node) {
    if (node instanceof LiveSecurityHotspotNode) {
      return (LiveSecurityHotspotNode) node;
    }

    if (node.getChildCount() > 0) {
      var firstChild = node.getFirstChild();
      return firstHotspotDown((AbstractNode) firstChild);
    }

    return null;
  }

  /**
   * Finds the first Security Hotspot node which is child of a given node.
   */
  @CheckForNull
  private static LiveSecurityHotspotNode lastHotspotDown(AbstractNode node) {
    if (node instanceof LiveSecurityHotspotNode) {
      return (LiveSecurityHotspotNode) node;
    }

    var lastChild = node.getLastChild();

    if (lastChild == null) {
      return null;
    }

    return lastHotspotDown((AbstractNode) lastChild);
  }
}
