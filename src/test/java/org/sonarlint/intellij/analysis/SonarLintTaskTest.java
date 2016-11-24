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
package org.sonarlint.intellij.analysis;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.ArrayList;
import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.intellij.SonarTest;
import org.sonarlint.intellij.core.ServerIssueUpdater;
import org.sonarlint.intellij.issue.IssueManager;
import org.sonarlint.intellij.issue.IssueProcessor;
import org.sonarlint.intellij.messages.TaskListener;
import org.sonarlint.intellij.trigger.TriggerType;
import org.sonarlint.intellij.ui.SonarLintConsole;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SonarLintTaskTest extends SonarTest {
  private SonarLintTask task;
  private IssueProcessor processor;
  private HashSet<VirtualFile> files;
  private ProgressIndicator progress;
  private SonarLintJob job;
  private SonarLintAnalyzer sonarLintAnalyzer;
  private AnalysisResults analysisResults;

  @Before
  public void prepare() {
    files = new HashSet<>();
    VirtualFile testFile = mock(VirtualFile.class);
    files.add(testFile);
    job = createJob();
    progress = mock(ProgressIndicator.class);
    analysisResults = mock(AnalysisResults.class);
    when(progress.isCanceled()).thenReturn(false);
    processor = mock(IssueProcessor.class);
    SonarLintConsole console = mock(SonarLintConsole.class);
    task = new SonarLintTask(processor, job, false, true);
    sonarLintAnalyzer = mock(SonarLintAnalyzer.class);
    when(sonarLintAnalyzer.analyzeModule(eq(module), eq(job.files()), any(IssueListener.class))).thenReturn(analysisResults);
    super.register(SonarLintStatus.class, new SonarLintStatus(getProject()));
    super.register(SonarLintAnalyzer.class, sonarLintAnalyzer);
    super.register(SonarLintConsole.class, console);
    super.register(ServerIssueUpdater.class, mock(ServerIssueUpdater.class));
    super.register(IssueManager.class, mock(IssueManager.class));

    //IntelliJ light test fixtures appear to reuse the same project container, so we need to ensure that status is stopped.
    SonarLintStatus.get(getProject()).stopRun();
  }

  @Test
  public void testTask() {
    TaskListener listener = mock(TaskListener.class);
    getProject().getMessageBus().connect(getProject()).subscribe(TaskListener.SONARLINT_TASK_TOPIC, listener);

    assertThat(task.shouldStartInBackground()).isTrue();
    task.run(progress);

    verify(sonarLintAnalyzer).analyzeModule(eq(module), eq(job.files()), any(IssueListener.class));
    verify(processor).process(job, progress, new ArrayList<>(), new ArrayList<>());
    verify(listener).ended(job);

    verifyNoMoreInteractions(sonarLintAnalyzer);
    verifyNoMoreInteractions(processor);
  }

  @Test
  public void testCallListenerOnError() {
    TaskListener listener = mock(TaskListener.class);
    getProject().getMessageBus().connect(getProject()).subscribe(TaskListener.SONARLINT_TASK_TOPIC, listener);

    doThrow(new IllegalStateException("error")).when(sonarLintAnalyzer).analyzeModule(eq(module), eq(job.files()), any(IssueListener.class));
    task.run(progress);

    // never called because of error
    verifyZeroInteractions(processor);

    // still called
    verify(listener).ended(job);
    verifyNoMoreInteractions(listener);
  }

  private SonarLintJob createJob() {
    return new SonarLintJob(module, files, TriggerType.ACTION);
  }
}
