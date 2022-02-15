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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A TaskExpression represents a config string pointing to an expression which includes zero or more
 * task names separated by a '|', and potentially termintated by a '|'. If the expression is not
 * terminated by a '|' it indicates that task resolution of at least one task is required. Task
 * selection priority is from left to right. This can be expressed as: <code>
 * EXPR = [ TASK_NAME '|' ] TASK_NAME [ '|' ]</code>
 *
 * <p>Example expressions to prioritized names and requirements:
 *
 * <ul>
 *   <li><code> "simple"        -> ("simple")         required</code>
 *   <li><code> "world | peace" -> ("world", "peace") required</code>
 *   <li><code> "shadenfreud |" -> ("shadenfreud")    optional</code>
 *   <li><code> "foo | bar |"   -> ("foo", "bar")     optional</code>
 * </ul>
 */
public class TaskExpression implements Iterable<String> {
  protected static final Pattern EXPRESSION_PATTERN = Pattern.compile("([^ |]+[^|]*)(\\|)?");
  protected final TaskExpressionKey key;

  public TaskExpression(FileKey key, String expression) {
    this.key = TaskExpressionKey.create(key, expression);
  }

  @Override
  public Iterator<String> iterator() {
    return new Iterator<String>() {
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
      public String next() {
        // Can't use @SuppressWarnings("ReturnValueIgnored") on method call
        boolean ignored = hasNext(); // in case next() was (re)called w/o calling hasNext()
        if (!hasNext) {
          throw new NoSuchElementException("No more names, yet expression was not optional");
        }
        hasNext = null;
        return m.group(1).trim();
      }
    };
  }
}
