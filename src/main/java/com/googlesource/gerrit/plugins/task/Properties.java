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
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.server.query.change.ChangeData;
import com.googlesource.gerrit.plugins.task.TaskConfig.NamesFactory;
import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Use to expand properties like ${_name} in the text of various definitions. */
public class Properties {
  /** Use to expand properties specifically for Tasks. */
  public static class Task extends Expander {
    public static final Task EMPTY_PARENT = new Task();

    public Task() {
      super(Collections.emptyMap());
    }

    public Task(ChangeData changeData, TaskConfig.Task definition, Task parentProperties)
        throws StorageException {
      super(parentProperties.forDescendants());
      valueByName.putAll(getInternalProperties(definition, changeData));
      new RecursiveExpander(valueByName).expand(definition.getAllProperties());

      definition.setExpandedProperties(valueByName);

      expandFieldValues(definition, Collections.emptySet());
    }

    protected Map<String, String> forDescendants() {
      return new HashMap<>(valueByName);
    }
  }

  /** Use to expand properties specifically for NamesFactories. */
  public static class NamesFactory extends Expander {
    public NamesFactory(TaskConfig.NamesFactory namesFactory, Task properties) {
      super(properties.valueByName);
      expandFieldValues(namesFactory, Sets.newHashSet(TaskConfig.KEY_TYPE));
    }
  }

  protected static Map<String, String> getInternalProperties(
      TaskConfig.Task definition, ChangeData changeData) throws StorageException {
    Map<String, String> properties = new HashMap<>();

    properties.put("_name", definition.name);

    Change c = changeData.change();
    properties.put("_change_number", String.valueOf(c.getId().get()));
    properties.put("_change_id", c.getKey().get());
    properties.put("_change_project", c.getProject().get());
    properties.put("_change_branch", c.getDest().get());
    properties.put("_change_status", c.getStatus().toString());
    properties.put("_change_topic", c.getTopic());

    return properties;
  }

  /**
   * Use to expand properties whose values may contain other references to properties.
   *
   * <p>Using a recursive expansion approach makes order of evaluation unimportant as long as there
   * are no looping definitions.
   *
   * <p>Given some property name/value asssociations defined like this:
   *
   * <p><code>
   * valueByName.put("obstacle", "fence");
   * valueByName.put("action", "jumped over the ${obstacle}");
   * </code>
   *
   * <p>a String like: <code>"The brown fox ${action}."</code>
   *
   * <p>will expand to: <code>"The brown fox jumped over the fence."</code>
   */
  protected static class RecursiveExpander {
    protected final Expander expander;
    protected Map<String, String> unexpandedByName;
    protected Set<String> expanding;

    public RecursiveExpander(Map<String, String> valueByName) {
      expander =
          new Expander(valueByName) {
            @Override
            protected String getValueForName(String name) {
              expandUnexpanded(name); // recursive call
              return super.getValueForName(name);
            }
          };
    }

    public void expand(Map<String, String> unexpandedByName) {
      this.unexpandedByName = unexpandedByName;

      // Copy keys to allow out of order removals during iteration
      for (String unexpanedName : new ArrayList<>(unexpandedByName.keySet())) {
        expanding = new HashSet<>();
        expandUnexpanded(unexpanedName);
      }
    }

    protected void expandUnexpanded(String name) {
      if (!expanding.add(name)) {
        throw new RuntimeException("Looping property definitions.");
      }
      String value = unexpandedByName.remove(name);
      if (value != null) {
        expander.valueByName.put(name, expander.expandText(value));
      }
    }
  }

  /**
   * Use to expand properties like ${property} in Strings into their values.
   *
   * <p>Given some property name/value asssociations defined like this:
   *
   * <p><code>
   * valueByName.put("animal", "fox");
   * valueByName.put("bar", "foo");
   * valueByName.put("obstacle", "fence");
   * </code>
   *
   * <p>a String like: <code>"The brown ${animal} jumped over the ${obstacle}."</code>
   *
   * <p>will expand to: <code>"The brown fox jumped over the fence."</code>
   */
  protected static class Expander {
    // "${_name}" -> group(1) = "_name"
    protected static final Pattern PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public final Map<String, String> valueByName;

    public Expander(Map<String, String> valueByName) {
      this.valueByName = valueByName;
    }

    /** Expand all properties in the Strings in the object's Fields (except the exclude ones) */
    protected void expandFieldValues(Object object, Set<String> excludedFieldNames) {
      for (Field field : object.getClass().getFields()) {
        try {
          if (!excludedFieldNames.contains(field.getName())) {
            field.setAccessible(true);
            Object o = field.get(object);
            if (o instanceof String) {
              field.set(object, expandText((String) o));
            } else if (o instanceof List) {
              @SuppressWarnings("unchecked")
              List<String> forceCheck = List.class.cast(o);
              expandElements(forceCheck);
            }
          }
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }

    /** Expand all properties in the Strings in the List */
    public void expandElements(List<String> list) {
      if (list != null) {
        for (ListIterator<String> it = list.listIterator(); it.hasNext(); ) {
          it.set(expandText(it.next()));
        }
      }
    }

    /** Expand all properties (${property_name} -> property_value) in the given text */
    public String expandText(String text) {
      if (text == null) {
        return null;
      }
      StringBuffer out = new StringBuffer();
      Matcher m = PATTERN.matcher(text);
      while (m.find()) {
        m.appendReplacement(out, Matcher.quoteReplacement(getValueForName(m.group(1))));
      }
      m.appendTail(out);
      return out.toString();
    }

    /** Get the replacement value for the property identified by name */
    protected String getValueForName(String name) {
      String value = valueByName.get(name);
      return value == null ? "" : value;
    }
  }
}
