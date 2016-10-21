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

import com.google.common.base.Preconditions;
import com.intellij.openapi.project.Project;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * NOT thread safe
 */
public class JobQueue {
  public static final int CAPACITY = 5;
  private final Project project;
  private final LinkedList<SonarLintJob> queue;

  public JobQueue(Project project) {
    this.project = project;
    this.queue = new LinkedList<>();
  }

  public void queue(SonarLintJob job, boolean optimize) throws NoCapacityException {
    Preconditions.checkArgument(job.module().getProject().equals(project), "job belongs to a different project");
    Preconditions.checkArgument(!job.files().isEmpty(), "no files to analyze");

    if (optimize && tryAddToExisting(job)) {
      return;
    }

    if (queue.size() >= CAPACITY) {
      throw new NoCapacityException();
    }
    queue.addLast(job);
  }

  private boolean tryAddToExisting(SonarLintJob job) {
    if (queue.isEmpty()) {
      return false;
    }

    ListIterator<SonarLintJob> it = queue.listIterator();
    while(it.hasNext()) {
      SonarLintJob j = it.next();
      if (!j.module().equals(job.module())) {
        continue;
      }


      SonarLintJob combined = new SonarLintJob(job, j);
      it.set(combined);
      return true;
    }

    return false;
  }

  public int size() {
    return queue.size();
  }

  public void queue(SonarLintJob job) throws NoCapacityException {
    queue(job, true);
  }

  /**
   * It's callers responsibility to check if there is an element in the queue
   * @return Next queued job
   * @throws NoSuchElementException if the queue is empty
   */
  public SonarLintJob get() {
    return queue.removeFirst();
  }

  public void clear() {
    queue.clear();
  }

  public static class NoCapacityException extends Exception {

  }
}
