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
package org.sonarlint.intellij.ui.nodes;

import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonarlint.intellij.ui.tree.TreeCellRenderer;

public class SummaryNode extends AbstractNode {
  private String emptyText;
  private Optional<Float> density = Optional.empty();
  private Optional<Float> duplicationDensityThreshold = Optional.empty();

  public SummaryNode() {
    super();
    this.emptyText = "No issues to display";
  }

  public void setEmptyText(String emptyText) {
    this.emptyText = emptyText;
  }

  private String getText() {
    var issues = getIssueCount();
    var files = getChildCount();

    if (issues == 0) {
      return emptyText;
    }

    return String.format("Found %d %s in %d %s%s", issues, issues == 1 ? "issue" : "issues", files,
      files == 1 ? "file" : "files",
      density.map(d -> String.format(" (Duplication is %.2f%%, QG limit is %.2f)", d * 100, duplicationDensityThreshold.orElse(0.f))).orElse(""));
  }

  public int insertFileNode(FileNode newChild, Comparator<FileNode> comparator) {
    if (children == null) {
      insert(newChild, 0);
      return 0;
    }

    // keep the cast for Java 8 compat
    var nodes = children.stream().map(FileNode.class::cast).collect(Collectors.<FileNode>toList());
    var foundIndex = Collections.binarySearch(nodes, newChild, comparator);
    if (foundIndex >= 0) {
      throw new IllegalArgumentException("Child already exists");
    }

    int insertIdx = -foundIndex - 1;
    insert(newChild, insertIdx);
    return insertIdx;
  }

  @Override
  public void render(TreeCellRenderer renderer) {
    renderer.append(getText());
  }

  public void showDuplicationDensity(Optional<Float> duplicationDensityThreshold, float density) {
    this.density = Optional.of(density);
    this.duplicationDensityThreshold = duplicationDensityThreshold;
    this.setDirty();
  }
}
