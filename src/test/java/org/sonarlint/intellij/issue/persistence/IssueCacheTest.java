package org.sonarlint.intellij.issue.persistence;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarlint.intellij.issue.IssueMatcher;
import org.sonarlint.intellij.issue.LocalIssuePointer;
import org.sonarlint.intellij.proto.Sonarlint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class IssueCacheTest {
  private IssueCache cache;
  private IssuePersistence store;
  private IssueMatcher matcher;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    Project project = mock(Project.class);
    matcher = mock(IssueMatcher.class);
    store = mock(IssuePersistence.class);
    cache = new IssueCache(project, store, matcher);

    when(project.getBasePath()).thenReturn("/root");

  }

  @Test
  public void should_save_and_read_cache_only() {
    VirtualFile file = createTestFile("file1");
    LocalIssuePointer issue1 = createTestIssue("r1");
    cache.save(file, Collections.singleton(issue1));

    assertThat(cache.read(file)).containsOnly(issue1);

    verifyZeroInteractions(store);
    verifyZeroInteractions(matcher);
  }

  @Test
  public void should_fallback_persistence() throws IOException {
    VirtualFile file = createTestFile("file1");
    LocalIssuePointer issue1 = createTestIssue("r1");
    cache.save(file, Collections.singleton(issue1));

    VirtualFile cacheMiss = createTestFile("file2");
    assertThat(cache.read(cacheMiss)).isNull();

    verify(store).read("file2");
    verifyZeroInteractions(matcher);
  }

  @Test
  public void should_flush_if_full() throws IOException {
    LocalIssuePointer issue1 = createTestIssue("r1");
    VirtualFile file0 = createTestFile("file0");
    cache.save(file0, Collections.singleton(issue1));

    for(int i=1; i<IssueCache.MAX_ENTRIES; i++) {
      VirtualFile file = createTestFile("file" + i);
      cache.save(file, Collections.singleton(issue1));
    }

    // oldest access should be file1 after this
    assertThat(cache.read(file0)).containsOnly(issue1);

    verifyZeroInteractions(store);

    VirtualFile file = createTestFile("anotherfile");
    cache.save(file, Collections.singleton(issue1));

    verify(store).save(eq("file1"), any(Sonarlint.Issues.class));
  }

  @Test
  public void should_clear_store() {
    LocalIssuePointer issue1 = createTestIssue("r1");
    VirtualFile file0 = createTestFile("file0");
    cache.save(file0, Collections.singleton(issue1));

    cache.clear();
    verify(store).clear();
    assertThat(cache.read(file0)).isNull();
  }

  @Test
  public void should_flush_when_requested() throws IOException {
    LocalIssuePointer issue1 = createTestIssue("r1");
    VirtualFile file0 = createTestFile("file0");
    cache.save(file0, Collections.singleton(issue1));
    VirtualFile file1 = createTestFile("file1");
    cache.save(file1, Collections.singleton(issue1));

    cache.flushAll();

    verify(store).save(eq("file0"), any(Sonarlint.Issues.class));
    verify(store).save(eq("file1"), any(Sonarlint.Issues.class));
    verifyNoMoreInteractions(store);
  }

  @Test
  public void error_flush() throws IOException {
    doThrow(new IOException()).when(store).save(anyString(), any(Sonarlint.Issues.class));

    LocalIssuePointer issue1 = createTestIssue("r1");
    VirtualFile file0 = createTestFile("file0");
    cache.save(file0, Collections.singleton(issue1));

    exception.expect(IllegalStateException.class);
    cache.flushAll();
  }

  @Test
  public void should_flush_on_project_closed() throws IOException {
    LocalIssuePointer issue1 = createTestIssue("r1");
    VirtualFile file0 = createTestFile("file0");
    cache.save(file0, Collections.singleton(issue1));
    VirtualFile file1 = createTestFile("file1");
    cache.save(file1, Collections.singleton(issue1));

    cache.projectClosed();

    verify(store).save(eq("file0"), any(Sonarlint.Issues.class));
    verify(store).save(eq("file1"), any(Sonarlint.Issues.class));
    verifyNoMoreInteractions(store);
  }


  private LocalIssuePointer createTestIssue(String ruleKey) {
    LocalIssuePointer issue = mock(LocalIssuePointer.class);
    when(issue.getRuleKey()).thenReturn(ruleKey);
    when(issue.getAssignee()).thenReturn("assignee");
    when(issue.ruleName()).thenReturn(ruleKey);
    when(issue.severity()).thenReturn("MAJOR");
    when(issue.getMessage()).thenReturn("msg");

    return issue;
  }

  private VirtualFile createTestFile(String path) {
    VirtualFile file = mock(VirtualFile.class);
    when(file.getPath()).thenReturn("/root/" + path);
    return file;
  }
}
