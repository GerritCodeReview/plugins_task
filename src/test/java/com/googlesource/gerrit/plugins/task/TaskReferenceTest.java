// Copyright (C) 2022 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.task;

import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Project;
import java.util.NoSuchElementException;
import junit.framework.TestCase;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.Test;

public class TaskReferenceTest extends TestCase {
  public static String SIMPLE = "simple";
  public static String ROOT = "task.config";
  public static String COMMON = "task/common.config";
  public static String SUB_COMMON = "task/dir/common.config";
  public static FileKey ROOT_CFG = createFileKey("project", "branch", ROOT);
  public static FileKey COMMON_CFG = createFileKey("project", "branch", COMMON);
  public static FileKey SUB_COMMON_CFG = createFileKey("project", "branch", SUB_COMMON);

  @Test
  public void testReferencingTaskFromSameFile() {
    assertEquals(createTaskKey(ROOT_CFG, SIMPLE), getTaskFromReference(ROOT_CFG, SIMPLE));
  }

  @Test
  public void testReferencingTaskFromRootConfig() {
    String reference = "^" + SIMPLE;
    assertEquals(createTaskKey(ROOT_CFG, SIMPLE), getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingRelativeTaskFromRootConfig() {
    String reference = " dir/common.config^" + SIMPLE;
    assertEquals(createTaskKey(SUB_COMMON_CFG, SIMPLE), getTaskFromReference(ROOT_CFG, reference));
  }

  @Test
  public void testReferencingAbsoluteTaskFromRootConfig() {
    String reference = " /common.config^" + SIMPLE;
    assertEquals(createTaskKey(COMMON_CFG, SIMPLE), getTaskFromReference(ROOT_CFG, reference));
  }

  @Test
  public void testReferencingRelativeDirTask() {
    String reference = " dir/common.config^" + SIMPLE;
    assertEquals(
        createTaskKey(SUB_COMMON_CFG, SIMPLE), getTaskFromReference(COMMON_CFG, reference));
  }

  @Test
  public void testReferencingRelativeFileTask() {
    String reference = "common.config^" + SIMPLE;
    assertEquals(createTaskKey(COMMON_CFG, SIMPLE), getTaskFromReference(COMMON_CFG, reference));
  }

  @Test
  public void testReferencingAbsoluteTask() {
    String reference = " /common.config^" + SIMPLE;
    assertEquals(
        createTaskKey(COMMON_CFG, SIMPLE), getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testMultipleUpchars() {
    String reference = " ^ /common.config^" + SIMPLE;
    assertNoSuchElementException(() -> getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testEmptyReference() {
    String empty = "";
    assertNoSuchElementException(() -> getTaskFromReference(SUB_COMMON_CFG, empty));
  }

  protected static TaskKey getTaskFromReference(FileKey file, String expression) {
    try {
      return new TaskReference(file, expression).getTaskKey();
    } catch (ConfigInvalidException e) {
      throw new NoSuchElementException();
    }
  }

  protected static TaskKey createTaskKey(FileKey file, String task) {
    return TaskKey.create(file, task);
  }

  protected static FileKey createFileKey(String project, String branch, String file) {
    return FileKey.create(BranchNameKey.create(Project.NameKey.parse(project), branch), file);
  }

  protected static void assertNoSuchElementException(Runnable f) {
    try {
      f.run();
      assertTrue(false);
    } catch (NoSuchElementException e) {
      assertTrue(true);
    }
  }
}
