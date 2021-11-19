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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Use to expand properties like ${_name} in the text of various definitions. */
public class Properties {
  /** Use to expand properties specifically for Tasks. */
  public static class Task extends Expander {
    public static final Task EMPTY_PARENT = new Task();

    public Task() {}

    public Task(ChangeData changeData, TaskConfig.Task definition, Task parentProperties)
        throws OrmException {
      putAll(parentProperties.getAll());
      putAll(getInternalProperties(definition, changeData));
      putAll(definition.getAllProperties());

      definition.setExpandedProperties(getAll());

      expandFieldValues(definition, Collections.emptySet());
    }

    /** Use to expand properties specifically for NamesFactories. */
    public NamesFactory getNamesFactory(NamesFactory namesFactory) {
      return expand(
          namesFactory,
          nf -> namesFactory.config.new NamesFactory(nf),
          Sets.newHashSet(TaskConfig.KEY_TYPE));
    }
  }

  protected static Map<String, String> getInternalProperties(
      TaskConfig.Task definition, ChangeData changeData) throws OrmException {
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
  protected static class Expander extends AbstractExpander {
    protected final Map<String, String> valueByName = new HashMap<>();
    protected final Map<String, String> unexpandedByName = new HashMap<>();
    protected final Set<String> expanding = new HashSet<>();

    public void putAll(Map<String, String> unexpandedByName) {
      this.unexpandedByName.putAll(unexpandedByName);
    }

    public Map<String, String> getAll() {
      // Copying keys enables out of order removals during iteration
      for (String name : new ArrayList<>(unexpandedByName.keySet())) {
        getValueForName(name);
      }
      return Collections.unmodifiableMap(valueByName);
    }

    @Override
    public String getValueForName(String name) {
      if (!expanding.add(name)) {
        throw new RuntimeException("Looping property definitions.");
      }
      String value = unexpandedByName.remove(name);
      if (value != null) {
        value = expandText(value);
        expanding.remove(name);
      } else {
        expanding.remove(name);
        value = valueByName.get(name);
        if (value != null) {
          return value;
        }
        value = "";
      }
      valueByName.put(name, value);
      return value;
    }
  }

  /**
   * Use to expand properties like ${property} in Strings into their values.
   *
   * <p>Given some property name/value associations like this:
   *
   * <p><code>
   * "animal" -> "fox"
   * "bar" -> "foo"
   * "obstacle" -> "fence"
   * </code>
   *
   * <p>a String like: <code>"The brown ${animal} jumped over the ${obstacle}."</code>
   *
   * <p>will expand to: <code>"The brown fox jumped over the fence."</code> This class is meant to
   * be used as a building block for other full featured expanders and thus must be overriden to
   * provide the name/value associations via the getValueForName() method.
   */
  protected abstract static class AbstractExpander {
    // "${_name}" -> group(1) = "_name"
    protected static final Pattern PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /** Expand all properties in the Strings in the object's Fields (except the exclude ones) */
    protected <T> T expandFieldValues(T object, Set<String> excludedFieldNames) {
      // Fake the CopyOnWrite for backwards compatibility.
      return expand(object, o -> o, excludedFieldNames);
    }

    /**
     * Returns expanded object if property found in the Strings in the object's Fields (except the
     * excluded ones). Returns same object if no expansions occured.
     */
    public <T> T expand(T object, Function<T, T> copier, Set<String> excludedFieldNames) {
      return expand(new CopyOnWrite<>(object, copier), excludedFieldNames);
    }

    /**
     * Returns expanded object if property found in the Strings in the object's Fields (except the
     * excluded ones). Returns same object if no expansions occured.
     */
    public <T> T expand(CopyOnWrite<T> cow, Set<String> excludedFieldNames) {
      for (Field field : cow.getOriginal().getClass().getFields()) {
        try {
          if (!excludedFieldNames.contains(field.getName())) {
            field.setAccessible(true);
            Object o = field.get(cow.getOriginal());
            if (o instanceof String) {
              String expanded = expandText((String) o);
              if (expanded != o) {
                field.set(cow.getForWrite(), expanded);
              }
            } else if (o instanceof List) {
              @SuppressWarnings("unchecked")
              List<String> forceCheck = List.class.cast(o);
              List<String> expanded = expand(forceCheck);
              if (expanded != o) {
                field.set(cow.getForWrite(), expanded);
              }
            }
          }
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
      return cow.getForRead();
    }

    /**
     * Returns expanded unmodifiable List if property found. Returns same object if no expansions
     * occured.
     */
    public List<String> expand(List<String> list) {
      if (list != null) {
        boolean hasProperty = false;
        List<String> expandedList = new ArrayList<>(list.size());
        for (String value : list) {
          String expanded = expandText(value);
          hasProperty = hasProperty || value != expanded;
          expandedList.add(expanded);
        }
        return hasProperty ? Collections.unmodifiableList(expandedList) : list;
      }
      return null;
    }

    /**
     * Expand all properties (${property_name} -> property_value) in the given text . Returns same
     * object if no expansions occured.
     */
    public String expandText(String text) {
      if (text == null) {
        return null;
      }
      StringBuffer out = new StringBuffer();
      Matcher m = PATTERN.matcher(text);
      if (!m.find()) {
        return text;
      }
      do {
        m.appendReplacement(out, Matcher.quoteReplacement(getValueForName(m.group(1))));
      } while (m.find());
      m.appendTail(out);
      return out.toString();
    }

    /** Get the replacement value for the property identified by name */
    protected abstract String getValueForName(String name);
  }
}
