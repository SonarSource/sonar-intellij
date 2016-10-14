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
package org.sonarlint.intellij.issue;

import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;

public class ServerIssuePointer implements IssuePointer {

  private final ServerIssue serverIssue;

  public ServerIssuePointer(ServerIssue serverIssue) {
    this.serverIssue = serverIssue;
  }

  @Override
  public Integer getLine() {
    return serverIssue.line();
  }

  @Override
  public String getMessage() {
    return serverIssue.message();
  }

  @Override
  public Integer getLineHash() {
    return serverIssue.checksum().hashCode();
  }

  @Override
  public String getRuleKey() {
    return serverIssue.ruleKey();
  }

  @Override
  public String getServerIssueKey() {
    return serverIssue.key();
  }

  @Override
  public Long getCreationDate() {
    return serverIssue.creationDate().getEpochSecond() * 1000;
  }

  @Override
  public boolean isResolved() {
    return !serverIssue.resolution().isEmpty();
  }

  @Override public String getAssignee() {
    return serverIssue.assigneeLogin();
  }
}
