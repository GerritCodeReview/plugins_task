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

import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.config.AllUsersName;
import java.sql.Timestamp;
import java.util.NoSuchElementException;
import java.util.Optional;
import junit.framework.TestCase;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.Test;
import org.mockito.Mockito;

public class TaskReferenceTest extends TestCase {
  private static final String ALL_USERS = "All-Users";

  public static String SIMPLE = "simple";
  public static String ROOT = "task.config";
  public static String COMMON = "task/common.config";
  public static String SUB_COMMON = "task/dir/common.config";
  public static FileKey ROOT_CFG = createFileKey("project", "branch", ROOT);
  public static FileKey COMMON_CFG = createFileKey("project", "branch", COMMON);
  public static FileKey SUB_COMMON_CFG = createFileKey("project", "branch", SUB_COMMON);

  public static final String TEST_USER = "testuser";
  public static final int TEST_USER_ID = 100000;
  public static final Account TEST_USER_ACCOUNT =
      Account.builder(Account.id(TEST_USER_ID), new Timestamp(0L)).build();
  public static final String TEST_USER_REF =
      "refs/users/" + String.format("%02d", TEST_USER_ID % 100) + "/" + TEST_USER_ID;
  public static final FileKey TEST_USER_ROOT_CFG = createFileKey(ALL_USERS, TEST_USER_REF, ROOT);
  public static final FileKey TEST_USER_COMMON_CFG =
      createFileKey(ALL_USERS, TEST_USER_REF, COMMON);

  @Test
  public void testReferencingTaskFromSameFile() throws Exception {
    assertEquals(createTaskKey(ROOT_CFG, SIMPLE), getTaskFromReference(ROOT_CFG, SIMPLE));
  }

  @Test
  public void testReferencingTaskFromRootConfig() throws Exception {
    String reference = "^" + SIMPLE;
    assertEquals(createTaskKey(ROOT_CFG, SIMPLE), getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingRelativeTaskFromRootConfig() throws Exception {
    String reference = " dir/common.config^" + SIMPLE;
    assertEquals(createTaskKey(SUB_COMMON_CFG, SIMPLE), getTaskFromReference(ROOT_CFG, reference));
  }

  @Test
  public void testReferencingAbsoluteTaskFromRootConfig() throws Exception {
    String reference = " /common.config^" + SIMPLE;
    assertEquals(createTaskKey(COMMON_CFG, SIMPLE), getTaskFromReference(ROOT_CFG, reference));
  }

  @Test
  public void testReferencingRelativeDirTask() throws Exception {
    String reference = " dir/common.config^" + SIMPLE;
    assertEquals(
        createTaskKey(SUB_COMMON_CFG, SIMPLE), getTaskFromReference(COMMON_CFG, reference));
  }

  @Test
  public void testReferencingRelativeFileTask() throws Exception {
    String reference = "common.config^" + SIMPLE;
    assertEquals(createTaskKey(COMMON_CFG, SIMPLE), getTaskFromReference(COMMON_CFG, reference));
  }

  @Test
  public void testReferencingAbsoluteTask() throws Exception {
    String reference = " /common.config^" + SIMPLE;
    assertEquals(
        createTaskKey(COMMON_CFG, SIMPLE), getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingRootUserTask() throws Exception {
    String reference = "@" + TEST_USER + "^" + SIMPLE;
    assertEquals(
        createTaskKey(TEST_USER_ROOT_CFG, SIMPLE), getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingUserTaskDir() throws Exception {
    String reference = "@" + TEST_USER + "/common.config^" + SIMPLE;
    assertEquals(
        createTaskKey(TEST_USER_COMMON_CFG, SIMPLE),
        getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testMultipleUpchars() throws Exception {
    String reference = " ^ /common.config^" + SIMPLE;
    assertNoSuchElementException(() -> getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testEmptyReference() throws Exception {
    String empty = "";
    assertNoSuchElementException(() -> getTaskFromReference(SUB_COMMON_CFG, empty));
  }

  protected static TaskKey getTaskFromReference(FileKey file, String expression) {
    AccountCache accountCache = Mockito.mock(AccountCache.class);
    Mockito.when(accountCache.getByUsername(TEST_USER))
        .thenReturn(Optional.of(AccountState.forAccount(TEST_USER_ACCOUNT)));
    try {
      return new TaskReference(
              new TaskKey.Builder(file, new AllUsersName(ALL_USERS), accountCache), expression)
          .getTaskKey();
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

  protected static void assertNoSuchElementException(Executable f) throws Exception {
    try {
      f.run();
      assertTrue(false);
    } catch (NoSuchElementException e) {
      assertTrue(true);
    }
  }

  @FunctionalInterface
  interface Executable {
    void run() throws Exception;
  }
}
