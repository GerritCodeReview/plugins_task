// Copyright (C) 2019 The Android Open Source Project
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

import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Use to expand properties like ${_name} for a Task Definition. */
public class Properties {
  // "${_name}" -> group(1) = "_name"
  protected static final Pattern PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

  protected Map<String, String> expanded = new HashMap<>();

  public Properties(Task definition) {
    expanded.put("_name", definition.name);

    for (Field field : Task.class.getFields()) {
      try {
        field.setAccessible(true);
        Object o = field.get(definition);
        if (o instanceof String) {
          field.set(definition, expandLiteral((String) o));
        } else if (o instanceof List) {
          expandInPlace((List<String>) o);
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected void expandInPlace(List<String> list) {
    if (list != null) {
      for (ListIterator<String> it = list.listIterator(); it.hasNext(); ) {
        it.set(expandLiteral(it.next()));
      }
    }
  }

  protected String expandLiteral(String literal) {
    if (literal == null) {
      return null;
    }
    StringBuffer out = new StringBuffer();
    Matcher m = PATTERN.matcher(literal);
    while (m.find()) {
      m.appendReplacement(out, Matcher.quoteReplacement(getExpandedValue(m.group(1))));
    }
    m.appendTail(out);
    return out.toString();
  }

  protected String getExpandedValue(String property) {
    String value = expanded.get(property);
    return value == null ? "" : value;
  }
}
