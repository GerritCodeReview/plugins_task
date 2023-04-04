// Copyright (C) 2021 The Android Open Source Project
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
import java.util.Iterator;
import java.util.NoSuchElementException;
import junit.framework.TestCase;

/*
 * <ul>
 *   <li><code> "simple"        -> ("simple")         required</code>
 *   <li><code> "world | peace" -> ("world", "peace") required</code>
 *   <li><code> "shadenfreud |" -> ("shadenfreud")    optional</code>
 *   <li><code> "foo | bar |"   -> ("foo", "bar")     optional</code>
 * </ul>
 */
public class TaskExpressionTest extends TestCase {
  public static String SIMPLE = "simple";
  public static String WORLD = "world";
  public static String PEACE = "peace";
  public static FileKey file = createFileKey("foo", "bar", "baz");

  public static TaskKey SIMPLE_TASK = TaskKey.create(file, SIMPLE);
  public static TaskKey WORLD_TASK = TaskKey.create(file, WORLD);
  public static TaskKey PEACE_TASK = TaskKey.create(file, PEACE);

  public void testBlank() {
    TaskExpression exp = getTaskExpression("");
    Iterator<TaskKey> it = exp.iterator();
    assertTrue(it.hasNext());
    assertNoSuchElementException(it);
  }

  public void testRequiredSingleName() {
    TaskExpression exp = getTaskExpression(SIMPLE);
    Iterator<TaskKey> it = exp.iterator();
    assertTrue(it.hasNext());
    assertEquals(it.next(), SIMPLE_TASK);
    assertTrue(it.hasNext());
    assertNoSuchElementException(it);
  }

  public void testOptionalSingleName() {
    TaskExpression exp = getTaskExpression(SIMPLE + "|");
    Iterator<TaskKey> it = exp.iterator();
    assertTrue(it.hasNext());
    assertEquals(it.next(), SIMPLE_TASK);
    assertFalse(it.hasNext());
  }

  public void testRequiredTwoNames() {
    TaskExpression exp = getTaskExpression(WORLD + "|" + PEACE);
    Iterator<TaskKey> it = exp.iterator();
    assertTrue(it.hasNext());
    assertEquals(it.next(), WORLD_TASK);
    assertTrue(it.hasNext());
    assertEquals(it.next(), PEACE_TASK);
    assertTrue(it.hasNext());
    assertNoSuchElementException(it);
  }

  public void testOptionalTwoNames() {
    TaskExpression exp = getTaskExpression(WORLD + "|" + PEACE + "|");
    Iterator<TaskKey> it = exp.iterator();
    assertTrue(it.hasNext());
    assertEquals(it.next(), WORLD_TASK);
    assertTrue(it.hasNext());
    assertEquals(it.next(), PEACE_TASK);
    assertFalse(it.hasNext());
  }

  public void testBlankSpaces() {
    TaskExpression exp = getTaskExpression("  ");
    Iterator<TaskKey> it = exp.iterator();
    assertTrue(it.hasNext());
    assertNoSuchElementException(it);
  }

  public void testRequiredSingleNameLeadingSpaces() {
    TaskExpression exp = getTaskExpression("  " + SIMPLE);
    Iterator<TaskKey> it = exp.iterator();
    assertTrue(it.hasNext());
    assertEquals(it.next(), SIMPLE_TASK);
    assertTrue(it.hasNext());
    assertNoSuchElementException(it);
  }

  public void testRequiredSingleNameTrailingSpaces() {
    TaskExpression exp = getTaskExpression(SIMPLE + "  ");
    Iterator<TaskKey> it = exp.iterator();
    assertTrue(it.hasNext());
    assertEquals(it.next(), SIMPLE_TASK);
    assertTrue(it.hasNext());
    assertNoSuchElementException(it);
  }

  public void testOptionalSingleNameLeadingSpaces() {
    TaskExpression exp = getTaskExpression("  " + SIMPLE + "|");
    Iterator<TaskKey> it = exp.iterator();
    assertTrue(it.hasNext());
    assertEquals(it.next(), SIMPLE_TASK);
    assertFalse(it.hasNext());
  }

  public void testOptionalSingleNameTrailingSpaces() {
    TaskExpression exp = getTaskExpression(SIMPLE + "|  ");
    Iterator<TaskKey> it = exp.iterator();
    assertTrue(it.hasNext());
    assertEquals(it.next(), SIMPLE_TASK);
    assertFalse(it.hasNext());
  }

  public void testOptionalSingleNameMiddleSpaces() {
    TaskExpression exp = getTaskExpression(SIMPLE + "  |");
    Iterator<TaskKey> it = exp.iterator();
    assertTrue(it.hasNext());
    assertEquals(it.next(), SIMPLE_TASK);
    assertFalse(it.hasNext());
  }

  public void testRequiredTwoNamesMiddleSpaces() {
    TaskExpression exp = getTaskExpression(WORLD + "  |  " + PEACE);
    Iterator<TaskKey> it = exp.iterator();
    assertTrue(it.hasNext());
    assertEquals(it.next(), WORLD_TASK);
    assertTrue(it.hasNext());
    assertEquals(it.next(), PEACE_TASK);
    assertTrue(it.hasNext());
    assertNoSuchElementException(it);
  }

  public void testDifferentKeyOnDifferentFile() {
    TaskExpression exp = getTaskExpression(createFileKey("foo", "bar", "baz"), SIMPLE);
    TaskExpression otherExp = getTaskExpression(createFileKey("foo", "bar", "other"), SIMPLE);
    assertFalse(exp.key.equals(otherExp.key));
  }

  public void testDifferentKeyOnDifferentBranch() {
    TaskExpression exp = getTaskExpression(createFileKey("foo", "bar", "baz"), SIMPLE);
    TaskExpression otherExp = getTaskExpression(createFileKey("foo", "other", "baz"), SIMPLE);
    assertFalse(exp.key.equals(otherExp.key));
  }

  public void testDifferentKeyOnDifferentProject() {
    TaskExpression exp = getTaskExpression(createFileKey("foo", "bar", "baz"), SIMPLE);
    TaskExpression otherExp = getTaskExpression(createFileKey("other", "bar", "baz"), SIMPLE);
    assertFalse(exp.key.equals(otherExp.key));
  }

  public void testDifferentKeyOnDifferentExpression() {
    TaskExpression exp = getTaskExpression(SIMPLE);
    TaskExpression otherExp = getTaskExpression(PEACE);
    assertFalse(exp.key.equals(otherExp.key));
  }

  protected static void assertNoSuchElementException(Iterator<TaskKey> it) {
    try {
      it.next();
      assertTrue(false);
    } catch (NoSuchElementException e) {
      assertTrue(true);
    }
  }

  protected TaskExpression getTaskExpression(String expression) {
    return getTaskExpression(file, expression);
  }

  protected TaskExpression getTaskExpression(FileKey file, String expression) {
    return new TaskExpression(file, expression);
  }

  protected static FileKey createFileKey(String project, String branch, String file) {
    return FileKey.create(BranchNameKey.create(Project.NameKey.parse(project), branch), file);
  }
}