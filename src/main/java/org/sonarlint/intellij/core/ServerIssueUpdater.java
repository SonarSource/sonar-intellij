/**
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

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.sonarlint.intellij.issue.IssuePointer;
import org.sonarlint.intellij.issue.IssueStore;
import org.sonarlint.intellij.issue.ServerIssuePointer;
import org.sonarlint.intellij.util.SonarLintUtils;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;

public class ServerIssueUpdater extends AbstractProjectComponent {

  private final IssueStore store;

  public ServerIssueUpdater(Project project, IssueStore store) {
    super(project);
    this.store = store;
  }

  public void trackServerIssues(Set<VirtualFile> virtualFiles) {
    virtualFiles.forEach(this::trackServerIssues);
  }

  private void trackServerIssues(VirtualFile virtualFile) {
    Optional<ConnectedSonarLintEngine> connectedEngineOptional = SonarLintUtils.get(SonarLintServerManager.class).getConnectedEngine(this.myProject);
    if (!connectedEngineOptional.isPresent()) {
      return;
    }

    ConnectedSonarLintEngine engine = connectedEngineOptional.get();

    String moduleKey = getModuleKey();
    String relativePath = getRelativePath(virtualFile);

    fetchAndMatchServerIssues(virtualFile, moduleKey, relativePath);
  }

  private void fetchAndMatchServerIssues(VirtualFile virtualFile, String moduleKey, String relativePath) {
    // TODO do all this in a thread

    Iterator<ServerIssue> serverIssues = fetchServerIssues(moduleKey, relativePath);

    Collection<IssuePointer> serverIssuePointers = toStream(serverIssues).map(ServerIssuePointer::new).collect(Collectors.toList());

    if (serverIssuePointers.isEmpty()) {
      return;
    }

    store.storeServerIssues(virtualFile, serverIssuePointers);
  }

  private <T> Stream<T> toStream(Iterator<T> iterator) {
    Iterable<T> iterable = () -> iterator;
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  private Iterator<ServerIssue> fetchServerIssues(String moduleKey, String relativePath) {
    // TODO download the issues for the file
    // TODO if download fails -> log it; get issues from local store
    return Collections.emptyIterator();
  }

  private String getRelativePath(VirtualFile virtualFile) {
    if (myProject.getBasePath() == null) {
      throw new IllegalStateException("no base path in default project");
    }
    return virtualFile.getPath().substring(myProject.getBasePath().length());
  }

  private String getModuleKey() {
    // TODO
    return myProject.getBasePath();
  }
}
