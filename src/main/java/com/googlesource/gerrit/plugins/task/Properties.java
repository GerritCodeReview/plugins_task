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

import com.google.common.collect.Sets;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gwtorm.server.OrmException;
import com.googlesource.gerrit.plugins.task.TaskConfig.NamesFactory;
import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
import java.lang.reflect.Field;
import java.util.Collections;
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

  protected Object definition;
  protected Map<String, String> expanded = new HashMap<>();
  protected Map<String, String> unexpanded;
  protected boolean expandingNonPropertyFields;
  protected Set<String> expanding;

  public Properties(ChangeData changeData, Task definition, Map<String, String> parentProperties)
      throws OrmException {
    expanded.putAll(parentProperties);
    expanded.put("_name", definition.name);
    Change c = changeData.change();
    expanded.put("_change_number", String.valueOf(c.getId().get()));
    expanded.put("_change_id", c.getKey().get());
    expanded.put("_change_project", c.getProject().get());
    expanded.put("_change_branch", c.getDest().get());
    expanded.put("_change_status", c.getStatus().toString());
    expanded.put("_change_topic", c.getTopic());

    unexpanded = definition.properties;
    unexpanded.putAll(definition.exported);
    expandAllUnexpanded();
    definition.properties = expanded;
    for (String property : definition.exported.keySet()) {
      definition.exported.put(property, expanded.get(property));
    }

    this.definition = definition;
    expandNonPropertyFields(Collections.emptySet());
  }

  public Properties(NamesFactory namesFactory, Map<String, String> properties) throws OrmException {
    expanded.putAll(properties);
    definition = namesFactory;
    expandNonPropertyFields(Sets.newHashSet(TaskConfig.KEY_TYPE));
  }

  protected void expandNonPropertyFields(Set<String> excludedFields) {
    expandingNonPropertyFields = true;
    for (Field field : definition.getClass().getFields()) {
      try {
        if (!excludedFields.contains(field.getName())) {
          field.setAccessible(true);
          Object o = field.get(definition);
          if (o instanceof String) {
            field.set(definition, expandLiteral((String) o));
          } else if (o instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> forceCheck = List.class.cast(o);
            expandInPlace(forceCheck);
          }
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
