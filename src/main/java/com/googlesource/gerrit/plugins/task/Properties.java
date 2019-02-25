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
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Use to expand properties like ${_name} for a Task Definition. */
public class Properties {
  // "${_name}" -> group(1) = "_name"
  protected static final Pattern PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

  protected Task definition;
  protected Map<String, String> expanded = new HashMap<>();
  protected Map<String, String> unexpanded;
  protected boolean expandingNonPropertyFields;
  protected Set<String> expanding;

  public Properties(Task definition, Map<String, String> parentProperties) {
    expanded.putAll(parentProperties);
    expanded.put("_name", definition.name);

    unexpanded = definition.properties;
    unexpanded.putAll(definition.exported);
    expandAllUnexpanded();
    definition.properties = expanded;

    if (definition.exported.isEmpty()) {
      definition.exported = null;
    } else {
      for (String property : definition.exported.keySet()) {
        definition.exported.put(property, expanded.get(property));
      }
    }

    this.definition = definition;
    expandNonPropertyFields();
  }

  protected void expandNonPropertyFields() {
    expandingNonPropertyFields = true;
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

  protected void expandAllUnexpanded() {
    String property;
    // A traditional iterator won't work because the recursive expansion may end up
    // expanding more than one property per iteration behind the iterator's back.
    while ((property = getFirstUnexpandedProperty()) != null) {
      expanding = new HashSet<>();
      expandProperty(property);
    }
  }

  protected void expandProperty(String property) {
    if (!expanding.add(property)) {
      throw new RuntimeException("Looping property definitions.");
    }
    String value = unexpanded.remove(property);
    if (value != null) {
      expanded.put(property, expandLiteral(value));
    }
  }

  protected String getFirstUnexpandedProperty() {
    for (String property : unexpanded.keySet()) {
      return property;
    }
    return null;
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
    if (!expandingNonPropertyFields) {
      expandProperty(property); // recursive call
    }
    String value = expanded.get(property);
    return value == null ? "" : value;
  }
}
