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
  public static final Properties EMPTY_PARENT = new Properties();

  protected final Properties parentProperties;
  protected final Task origTask;
  protected final CopyOnWrite<Task> task;
  protected Expander expander;
  protected Loader loader;
  protected boolean init = true;
  protected boolean isTaskRefreshNeeded;
  protected boolean isSubNodeReloadRequired;

  public Properties() {
    this(null, null);
    expander = new Expander(n -> "");
  }

  public Properties(Task origTask, Properties parentProperties) {
    this.origTask = origTask;
    this.parentProperties = parentProperties;
    task = new CopyOnWrite<>(origTask, t -> origTask.config.new Task(t));
  }

  /** Use to expand properties specifically for Tasks. */
  public Task getTask(ChangeData changeData) throws OrmException {
    loader = new Loader(changeData);
    expander = new Expander(n -> loader.load(n));
    if (isTaskRefreshNeeded || init) {
      Map<String, String> exported = expander.expand(origTask.exported);
      if (exported != origTask.exported) {
        task.getForWrite().exported = exported;
      }

      expander.expand(task, Collections.emptySet());

      if (init) {
        init = false;
        isTaskRefreshNeeded = loader.isNonTaskDefinedPropertyLoaded();
      }
    }
    return task.getForRead();
  }

  // To detect NamesFactories dependent on non task defined properties, the checking must be
  // done after subnodes are fully loaded, which unfortunately happens after getTask() is
  // called, therefore this must be called after all subnodes have been loaded.
  public void expansionComplete() {
    isSubNodeReloadRequired = loader.isNonTaskDefinedPropertyLoaded();
  }

  public boolean isSubNodeReloadRequired() {
    return isSubNodeReloadRequired;
  }

  /** Use to expand properties specifically for NamesFactories. */
  public NamesFactory getNamesFactory(NamesFactory namesFactory) {
    return expander.expand(
        namesFactory,
        nf -> namesFactory.config.new NamesFactory(nf),
        Sets.newHashSet(TaskConfig.KEY_TYPE));
  }

  protected class Loader {
    protected final ChangeData changeData;
    protected final Function<String, String> inheritedMapper;
    protected Change change;
    protected boolean isInheritedPropertyLoaded;

    public Loader(ChangeData changeData) {
      this.changeData = changeData;
      if (parentProperties == null || parentProperties.expander == null) {
        inheritedMapper = n -> "";
      } else {
        inheritedMapper = n -> parentProperties.expander.getValueForName(n);
      }
    }

    public boolean isNonTaskDefinedPropertyLoaded() {
      return change != null || isInheritedPropertyLoaded;
    }

    public String load(String name) {
      if (name.startsWith("_")) {
        return internal(name);
      }
      String value = origTask.exported.get(name);
      if (value == null) {
        value = origTask.properties.get(name);
        if (value == null) {
          value = inheritedMapper.apply(name);
          if (!value.isEmpty()) {
            isInheritedPropertyLoaded = true;
          }
        }
      }
      return value;
    }

    protected String internal(String name) {
      if ("_name".equals(name)) {
        return origTask.name();
      }
      String changeProp = name.replace("_change_", "");
      if (changeProp != name) {
        try {
          return change(changeProp);
        } catch (OrmException e) {
          throw new RuntimeException(e);
        }
      }
      return "";
    }

    protected String change(String changeProp) throws OrmException {
      switch (changeProp) {
        case "number":
          return String.valueOf(change().getId().get());
        case "id":
          return change().getKey().get();
        case "project":
          return change().getProject().get();
        case "branch":
          return change().getDest().get();
        case "status":
          return change().getStatus().toString();
        case "topic":
          return change().getTopic();
        default:
          return "";
      }
    }

    protected Change change() {
      if (change == null) {
        try {
          change = changeData.change();
        } catch (OrmException e) {
          throw new RuntimeException(e);
        }
      }
      return change;
    }
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
    protected final Function<String, String> loadingFunction;
    protected final Map<String, String> valueByName = new HashMap<>();
    protected final Set<String> expanding = new HashSet<>();

    public Expander(Function<String, String> loadingFunction) {
      this.loadingFunction = loadingFunction;
    }

    /**
     * Expand all properties (${property_name} -> property_value) in the given text. Returns same
     * object if no expansions occured.
     */
    public Map<String, String> expand(Map<String, String> map) {
      if (map != null) {
        boolean hasProperty = false;
        Map<String, String> expandedMap = new HashMap<>(map.size());
        for (Map.Entry<String, String> e : map.entrySet()) {
          String name = e.getKey();
          String value = e.getValue();
          String expanded = getValueForName(name);
          hasProperty = hasProperty || value != expanded;
          expandedMap.put(name, expanded);
        }
        return hasProperty ? Collections.unmodifiableMap(expandedMap) : map;
      }
      return null;
    }

    @Override
    public String getValueForName(String name) {
      String value = valueByName.get(name);
      if (value != null) {
        return value;
      }
      value = loadingFunction.apply(name);
      if (value == null) {
        value = "";
      } else if (!value.isEmpty()) {
        if (!expanding.add(name)) {
          throw new RuntimeException("Looping property definitions.");
        }
        value = expandText(value);
        expanding.remove(name);
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

    /**
     * Get the replacement value for the property identified by name
     *
     * @param name of the property to get the replacement value for
     * @return the replacement value. Since the expandText() method alwyas needs a String to replace
     *     '${property-name}' reference with, even when the property does not exist, this will never
     *     return null, instead it will returns the empty string if the property is not found.
     */
    protected abstract String getValueForName(String name);
  }
}
