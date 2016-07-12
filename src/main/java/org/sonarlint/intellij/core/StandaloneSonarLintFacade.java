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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCoreUtil;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.sonarlint.intellij.config.project.SonarLintProjectSettings;
import org.sonarlint.intellij.ui.SonarLintConsole;
import org.sonarlint.intellij.util.ProjectLogOutput;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

public final class StandaloneSonarLintFacade implements SonarLintFacade {
  private StandaloneSonarLintEngine sonarlint;
  private Project project;

  public StandaloneSonarLintFacade(Project project, StandaloneSonarLintEngine engine) {
    this.project = project;
    this.sonarlint = engine;
  }

  @Nullable
  @Override
  public synchronized String getDescription(String ruleKey) {
    if (sonarlint == null) {
      return null;
    }
    RuleDetails details = sonarlint.getRuleDetails(ruleKey);
    if (details == null) {
      return null;
    }
    return details.getHtmlDescription();
  }

  @Nullable
  @Override
  public synchronized String getRuleName(String ruleKey) {
    if (sonarlint == null) {
      return null;
    }
    RuleDetails details = sonarlint.getRuleDetails(ruleKey);
    if (details == null) {
      return null;
    }
    return details.getName();
  }

  @Override
  public synchronized AnalysisResults startAnalysis(List<ClientInputFile> inputFiles, IssueListener issueListener, Map<String, String> additionalProps) {
    SonarLintProjectSettings projectSettings = project.getComponent(SonarLintProjectSettings.class);
    SonarLintConsole console = project.getComponent(SonarLintConsole.class);

    Path baseDir = Paths.get(project.getBasePath());
    Path workDir = baseDir.resolve(ProjectCoreUtil.DIRECTORY_BASED_PROJECT_DIR).resolve("sonarlint").toAbsolutePath();
    Map<String, String> props = new HashMap<>();
    props.putAll(additionalProps);
    props.putAll(projectSettings.getAdditionalProperties());
    StandaloneAnalysisConfiguration config = new StandaloneAnalysisConfiguration(baseDir, workDir, inputFiles, props);
    console.debug("Starting analysis with configuration:\n" + config.toString());

    return sonarlint.analyze(config, issueListener, new ProjectLogOutput(console, projectSettings));
  }
}
