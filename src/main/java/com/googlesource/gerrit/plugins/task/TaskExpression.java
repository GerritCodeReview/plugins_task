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

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.errors.ConfigInvalidException;

/**
 * A TaskExpression represents a config string pointing to an expression which includes zero or more
 * task references separated by a '|', and potentially termintated by a '|'. If the expression is
 * not terminated by a '|' it indicates that task resolution of at least one task is required. Task
 * selection priority is from left to right. This can be expressed as:
 *
 * <pre>
 * TASK_EXPR = TASK_REFERENCE [ WHITE_SPACE * '|' [ WHITE_SPACE * TASK_EXPR ] ]
 * </pre>
 *
 * <a href="file:../../../../../../antlr4/com/googlesource/gerrit/plugins/task/TaskReference.g4">See
 * this for Task Reference</a>
 *
 * <p>Example expressions to prioritized names and requirements:
 *
 * <ul>
 *   <li>
 *       <pre> "simple"            -> ("simple")                       required</pre>
 *   <li>
 *       <pre> "world | peace"     -> ("world", "peace")               required</pre>
 *   <li>
 *       <pre> "shadenfreud |"     -> ("shadenfreud")                  optional</pre>
 *   <li>
 *       <pre> "foo | bar |"       -> ("foo", "bar")                   optional</pre>
 * </ul>
 */
public class TaskExpression implements Iterable<TaskKey> {
  public interface Factory {
    TaskExpression create(FileKey key, String expression);
  }

  protected static final Pattern EXPRESSION_PATTERN = Pattern.compile("([^ |]+[^|]*)(\\|)?");
  protected final TaskExpressionKey key;
  protected final TaskReference.Factory taskReferenceFactory;

  @Inject
  public TaskExpression(
      TaskReference.Factory taskReferenceFactory,
      @Assisted FileKey key,
      @Assisted String expression) {
    this.key = TaskExpressionKey.create(key, expression);
    this.taskReferenceFactory = taskReferenceFactory;
  }

  @Override
  public Iterator<TaskKey> iterator() {
    return new Iterator<TaskKey>() {
      Matcher m = EXPRESSION_PATTERN.matcher(key.expression());
      Boolean hasNext;
      boolean optional;

      @Override
      public boolean hasNext() {
        if (hasNext == null) {
          hasNext = m.find();
          if (hasNext) {
            optional = m.group(2) != null;
          }
        }
        if (!hasNext && !optional) {
          return true; // fake it so next() throws an Exception
        }
        return hasNext;
      }

      @Override
      public TaskKey next() {
        // Can't use @SuppressWarnings("ReturnValueIgnored") on method call
        boolean ignored = hasNext(); // in case next() was (re)called w/o calling hasNext()
        if (!hasNext) {
          throw new NoSuchElementException("No more names, yet expression was not optional");
        }
        hasNext = null;
        try {
          return taskReferenceFactory.create(key.file(), m.group(1)).getTaskKey();
        } catch (ConfigInvalidException e) {
          throw new RuntimeConfigInvalidException(e);
        }
      }
    };
  }
}
