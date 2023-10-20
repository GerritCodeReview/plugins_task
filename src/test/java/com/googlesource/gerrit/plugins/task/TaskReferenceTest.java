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

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.InternalGroup;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.account.GroupCache;
import com.google.gerrit.server.config.AllProjectsName;
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
  private static final String ALL_PROJECTS = "All-Projects";
  public static String SIMPLE = "simple";
  public static String ROOT = "task.config";
  public static String COMMON = "task/common.config";
  public static String SUB_COMMON = "task/dir/common.config";
  public static FileKey ROOT_CFG = createFileKey(ALL_PROJECTS, RefNames.REFS_CONFIG, ROOT);
  public static FileKey COMMON_CFG = createFileKey(ALL_PROJECTS, RefNames.REFS_CONFIG, COMMON);
  public static FileKey SUB_COMMON_CFG =
      createFileKey(ALL_PROJECTS, RefNames.REFS_CONFIG, SUB_COMMON);

  public static FileKey SAMPLE_PROJ_CFG = createFileKey("foo", RefNames.REFS_CONFIG, ROOT);

  public static final String TEST_USER = "testuser";
  public static final int TEST_USER_ID = 100000;
  public static final Account TEST_USER_ACCOUNT =
      Account.builder(Account.id(TEST_USER_ID), new Timestamp(0L)).build();
  public static final String TEST_USER_REF =
      "refs/users/" + String.format("%02d", TEST_USER_ID % 100) + "/" + TEST_USER_ID;
  public static final FileKey TEST_USER_ROOT_CFG = createFileKey(ALL_USERS, TEST_USER_REF, ROOT);
  public static final FileKey TEST_USER_COMMON_CFG =
      createFileKey(ALL_USERS, TEST_USER_REF, COMMON);

  public static final AccountGroup.NameKey TEST_GROUP1_NAME = AccountGroup.nameKey("testgroup");
  public static final AccountGroup.NameKey TEST_GROUP2_NAME = AccountGroup.nameKey("test group");
  public static final String TEST_GROUP1_UUID = "526d2bf882635380fbd3b72320464e342fc14533";
  public static final String TEST_GROUP2_UUID = "62aa5663241f31b9483bad66132bd5d416b2bef9";
  public static final InternalGroup TEST_GROUP1 =
      buildTestGroup(AccountGroup.id(1), TEST_GROUP1_NAME, AccountGroup.uuid(TEST_GROUP1_UUID));
  public static final InternalGroup TEST_GROUP2 =
      buildTestGroup(AccountGroup.id(2), TEST_GROUP2_NAME, AccountGroup.uuid(TEST_GROUP2_UUID));
  public static final String TEST_GROUP1_REF =
      "refs/groups/" + TEST_GROUP1_UUID.substring(0, 2) + "/" + TEST_GROUP1_UUID;
  public static final String TEST_GROUP2_REF =
      "refs/groups/" + TEST_GROUP2_UUID.substring(0, 2) + "/" + TEST_GROUP2_UUID;
  public static final FileKey TEST_GROUP1_ROOT_CFG =
      createFileKey(ALL_USERS, TEST_GROUP1_REF, ROOT);
  public static final FileKey TEST_GROUP1_COMMON_CFG =
      createFileKey(ALL_USERS, TEST_GROUP1_REF, COMMON);
  public static final FileKey TEST_GROUP2_ROOT_CFG =
      createFileKey(ALL_USERS, TEST_GROUP2_REF, ROOT);
  public static final FileKey TEST_GROUP2_COMMON_CFG =
      createFileKey(ALL_USERS, TEST_GROUP2_REF, COMMON);

  static InternalGroup buildTestGroup(
      AccountGroup.Id id, AccountGroup.NameKey nameKey, AccountGroup.UUID uuid) {
    return InternalGroup.builder()
        .setGroupUUID(uuid)
        .setNameKey(nameKey)
        .setOwnerGroupUUID(uuid)
        .setId(id)
        .setVisibleToAll(true)
        .setCreatedOn(new Timestamp(0L))
        .setMembers(ImmutableSet.of())
        .setSubgroups(ImmutableSet.of())
        .build();
  }

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
  public void testReferencingRootAllProjectsTask() throws Exception {
    String reference = "//^" + SIMPLE;
    assertEquals(createTaskKey(ROOT_CFG, SIMPLE), getTaskFromReference(SAMPLE_PROJ_CFG, reference));
  }

  @Test
  public void testReferencingAllProjectsTask() throws Exception {
    String reference = "//common.config^" + SIMPLE;
    assertEquals(
        createTaskKey(COMMON_CFG, SIMPLE), getTaskFromReference(SAMPLE_PROJ_CFG, reference));
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

  @Test
  public void testReferencingRootGroupNameWithoutSpaceTask() throws Exception {
    String reference = "%" + TEST_GROUP1_NAME.get() + "^" + SIMPLE;
    assertEquals(
        createTaskKey(TEST_GROUP1_ROOT_CFG, SIMPLE),
        getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingRootGroupNameWithSpaceTask() throws Exception {
    String reference = "%" + TEST_GROUP2_NAME.get() + "^" + SIMPLE;
    assertEquals(
        createTaskKey(TEST_GROUP2_ROOT_CFG, SIMPLE),
        getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingGroupNameWithoutSpaceTaskDir() throws Exception {
    String reference = "%" + TEST_GROUP1_NAME.get() + "/common.config^" + SIMPLE;
    assertEquals(
        createTaskKey(TEST_GROUP1_COMMON_CFG, SIMPLE),
        getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingUnknownGroupName() throws Exception {
    String reference = "%unknown^" + SIMPLE;
    assertNoSuchElementException(() -> getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingEmptyGroupName() throws Exception {
    String reference = "%^" + SIMPLE;
    assertNoSuchElementException(() -> getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingGroupNameWithSpaceTaskDir() throws Exception {
    String reference = "%" + TEST_GROUP2_NAME.get() + "/common.config^" + SIMPLE;
    assertEquals(
        createTaskKey(TEST_GROUP2_COMMON_CFG, SIMPLE),
        getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingRootGroupUUIDTask() throws Exception {
    String reference = "%%" + TEST_GROUP1_UUID + "^" + SIMPLE;
    assertEquals(
        createTaskKey(TEST_GROUP1_ROOT_CFG, SIMPLE),
        getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingGroupUUIDTaskDir() throws Exception {
    String reference = "%%" + TEST_GROUP1_UUID + "/common.config^" + SIMPLE;
    assertEquals(
        createTaskKey(TEST_GROUP1_COMMON_CFG, SIMPLE),
        getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingUnknownGroupUUID() throws Exception {
    String reference = "%%a8341ade45d83e867c24a2d37f47b410cfdbea6d^" + SIMPLE;
    assertNoSuchElementException(() -> getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  @Test
  public void testReferencingEmptyGroupUUID() throws Exception {
    String reference = "%%^" + SIMPLE;
    assertNoSuchElementException(() -> getTaskFromReference(SUB_COMMON_CFG, reference));
  }

  protected static TaskKey getTaskFromReference(FileKey file, String expression) {
    AccountCache accountCache = Mockito.mock(AccountCache.class);
    GroupCache groupCache = Mockito.mock(GroupCache.class);
    Mockito.when(accountCache.getByUsername(TEST_USER))
        .thenReturn(Optional.of(AccountState.forAccount(TEST_USER_ACCOUNT)));
    Mockito.when(groupCache.get(TEST_GROUP1_NAME)).thenReturn(Optional.of(TEST_GROUP1));
    Mockito.when(groupCache.get(TEST_GROUP2_NAME)).thenReturn(Optional.of(TEST_GROUP2));
    Mockito.when(groupCache.get(AccountGroup.uuid(TEST_GROUP1_UUID)))
        .thenReturn(Optional.of(TEST_GROUP1));
    Mockito.when(groupCache.get(AccountGroup.uuid(TEST_GROUP2_UUID)))
        .thenReturn(Optional.of(TEST_GROUP2));

    try {
      return new TaskReference(
              new TaskKey.Builder(
                  file,
                  new AllProjectsName(ALL_PROJECTS),
                  new AllUsersName(ALL_USERS),
                  accountCache,
                  groupCache),
              expression)
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
