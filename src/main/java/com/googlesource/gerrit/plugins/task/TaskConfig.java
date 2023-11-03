// Copyright (C) 2016 The Android Open Source Project
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

import com.google.gerrit.common.Container;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.server.git.meta.AbstractVersionedMetaData;
import com.googlesource.gerrit.plugins.task.util.Copier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Task Configuration file living in git */
public class TaskConfig extends AbstractVersionedMetaData {
  public enum NamesFactoryType {
    CHANGE,
    STATIC;

    public static NamesFactoryType getNamesFactoryType(String str) {
      for (NamesFactoryType type : NamesFactoryType.values()) {
        if (type.name().equalsIgnoreCase(str)) return type;
      }
      return null;
    }
  }

  protected class SubSection extends Container {
    public TaskConfig config;
    public final SubSectionKey subSection;

    public SubSection(SubSectionKey s) {
      this.config = TaskConfig.this;
      this.subSection = s;
    }
  }

  public class TaskBase extends SubSection {
    public String applicable;
    public String duplicateKey;
    public Map<String, String> exported;
    public String fail;
    public String failHint;
    public String inProgress;
    public String pass;
    public String preloadTask;
    public Map<String, String> properties;
    public String readyHint;
    public List<ConfigSourcedValue> subTasks;
    public List<String> subTasksExternals;
    public List<String> subTasksFactories;
    public List<String> subTasksFiles;

    public boolean isVisible;
    public boolean isMasqueraded;

    public TaskBase(SubSectionKey s, boolean isVisible, boolean isMasqueraded) {
      super(s);
      this.isVisible = isVisible;
      this.isMasqueraded = isMasqueraded;
      applicable = getString(s, KEY_APPLICABLE, null);
      duplicateKey = getString(s, KEY_DUPLICATE_KEY, null);
      exported = getProperties(s, KEY_EXPORT_PREFIX);
      fail = getString(s, KEY_FAIL, null);
      failHint = getString(s, KEY_FAIL_HINT, null);
      inProgress = getString(s, KEY_IN_PROGRESS, null);
      pass = getString(s, KEY_PASS, null);
      preloadTask = getString(s, KEY_PRELOAD_TASK, null);
      properties = getProperties(s, KEY_PROPERTIES_PREFIX);
      readyHint = getString(s, KEY_READY_HINT, null);
      subTasks =
          getStringList(s, KEY_SUBTASK).stream()
              .map(subTask -> new ConfigSourcedValue(s.file(), subTask))
              .collect(Collectors.toList());
      subTasksExternals = getStringList(s, KEY_SUBTASKS_EXTERNAL);
      subTasksFactories = getStringList(s, KEY_SUBTASKS_FACTORY);
      subTasksFiles = getStringList(s, KEY_SUBTASKS_FILE);
    }

    protected TaskBase(TaskBase base) {
      this(base.subSection);
      Copier.shallowCopyDeclaredFields(TaskBase.class, base, this, false);
    }

    protected TaskBase(SubSectionKey s) {
      super(s);
    }
  }

  public class Task extends TaskBase implements Cloneable {
    public final TaskKey key;

    public Task(SubSectionKey s, boolean isVisible, boolean isMasqueraded) {
      super(s, isVisible, isMasqueraded);
      key = TaskKey.create(s);
    }

    public Task(TasksFactory tasks, String name) {
      super(tasks);
      key = TaskKey.create(tasks.subSection, name);
    }

    public Task(SubSectionKey s) {
      super(s);
      key = TaskKey.create(s);
    }

    protected Map<String, String> getAllProperties() {
      Map<String, String> all = new HashMap<>(properties);
      all.putAll(exported);
      return all;
    }

    public String name() {
      return key.task();
    }

    public FileKey file() {
      return key.subSection().file();
    }

    public TaskKey key() {
      return key;
    }
  }

  public class TasksFactory extends TaskBase {
    public String namesFactory;

    public TasksFactory(SubSectionKey s, boolean isVisible, boolean isMasqueraded) {
      super(s, isVisible, isMasqueraded);
      namesFactory = getString(s, KEY_NAMES_FACTORY, null);
    }
  }

  public class NamesFactory extends SubSection implements Cloneable {
    public String changes;
    public List<String> names;
    public String type;

    public NamesFactory(SubSectionKey s) {
      super(s);
      changes = getString(s, KEY_CHANGES, null);
      names = getStringList(s, KEY_NAME);
      type = getString(s, KEY_TYPE, null);
    }
  }

  public class External extends SubSection {
    public String name;
    public String file;
    public String user;

    public External(SubSectionKey s) {
      super(s);
      name = s.subSection();
      file = getString(s, KEY_FILE, null);
      user = getString(s, KEY_USER, null);
    }
  }

  public static final String SEP = "\0";

  public static final String SECTION_EXTERNAL = "external";
  public static final String SECTION_NAMES_FACTORY = "names-factory";
  public static final String SECTION_ROOT = "root";
  public static final String SECTION_TASK = TaskKey.CONFIG_SECTION;
  public static final String SECTION_TASKS_FACTORY = TaskKey.CONFIG_TASKS_FACTORY;
  public static final String KEY_APPLICABLE = "applicable";
  public static final String KEY_CHANGES = "changes";
  public static final String KEY_DUPLICATE_KEY = "duplicate-key";
  public static final String KEY_EXPORT_PREFIX = "export-";
  public static final String KEY_FAIL = "fail";
  public static final String KEY_FAIL_HINT = "fail-hint";
  public static final String KEY_FILE = "file";
  public static final String KEY_IN_PROGRESS = "in-progress";
  public static final String KEY_NAME = "name";
  public static final String KEY_NAMES_FACTORY = "names-factory";
  public static final String KEY_PASS = "pass";
  public static final String KEY_PRELOAD_TASK = "preload-task";
  public static final String KEY_PROPERTIES_PREFIX = "set-";
  public static final String KEY_READY_HINT = "ready-hint";
  public static final String KEY_SUBTASK = "subtask";
  public static final String KEY_SUBTASKS_EXTERNAL = "subtasks-external";
  public static final String KEY_SUBTASKS_FACTORY = "subtasks-factory";
  public static final String KEY_SUBTASKS_FILE = "subtasks-file";
  public static final String KEY_TYPE = "type";
  public static final String KEY_USER = "user";

  protected final FileKey file;
  public boolean isVisible;
  public boolean isMasqueraded;

  public TaskConfig(FileKey file, boolean isVisible, boolean isMasqueraded) {
    this(file.branch(), file, isVisible, isMasqueraded);
  }

  public TaskConfig(
      BranchNameKey masqueraded, FileKey file, boolean isVisible, boolean isMasqueraded) {
    super(masqueraded, file.file());
    this.file = file;
    this.isVisible = isVisible;
    this.isMasqueraded = isMasqueraded;
  }

  protected List<Task> getTasks(String type) {
    List<Task> tasks = new ArrayList<>();
    // No need to get a task with no name (what would we call it?)
    for (String task : cfg.getSubsections(type)) {
      tasks.add(new Task(subSectionKey(type, task), isVisible, isMasqueraded));
    }
    return tasks;
  }

  public List<External> getExternals() {
    List<External> externals = new ArrayList<>();
    // No need to get an external with no name (what would we call it?)
    for (String external : cfg.getSubsections(SECTION_EXTERNAL)) {
      externals.add(getExternal(external));
    }
    return externals;
  }

  protected Optional<Task> getOptionalTask(String name) {
    SubSectionKey subSection = subSectionKey(SECTION_TASK, name);
    return getNames(subSection).isEmpty()
        ? Optional.empty()
        : Optional.of(new Task(subSection, isVisible, isMasqueraded));
  }

  public TasksFactory getTasksFactory(String name) {
    return new TasksFactory(subSectionKey(SECTION_TASKS_FACTORY, name), isVisible, isMasqueraded);
  }

  public NamesFactory getNamesFactory(String name) {
    return new NamesFactory(subSectionKey(SECTION_NAMES_FACTORY, name));
  }

  public External getExternal(String name) {
    return getExternal(subSectionKey(SECTION_EXTERNAL, name));
  }

  protected External getExternal(SubSectionKey s) {
    return new External(s);
  }

  protected Map<String, String> getProperties(SubSectionKey s, String prefix) {
    Map<String, String> valueByName = new HashMap<>();
    for (Map.Entry<String, String> e :
        getStringByName(s, getMatchingNames(s, prefix + ".+")).entrySet()) {
      String name = e.getKey();
      valueByName.put(name.substring(prefix.length()), e.getValue());
    }
    return Collections.unmodifiableMap(valueByName);
  }

  protected Map<String, String> getStringByName(SubSectionKey s, Iterable<String> names) {
    Map<String, String> valueByName = new HashMap<>();
    for (String name : names) {
      valueByName.put(name, getString(s, name));
    }
    return Collections.unmodifiableMap(valueByName);
  }

  protected Set<String> getMatchingNames(SubSectionKey s, String match) {
    Set<String> matched = new HashSet<>();
    for (String name : getNames(s)) {
      if (name.matches(match)) {
        matched.add(name);
      }
    }
    return Collections.unmodifiableSet(matched);
  }

  protected Set<String> getNames(SubSectionKey s) {
    return cfg.getNames(s.section(), s.subSection());
  }

  protected String getString(SubSectionKey s, String key, String def) {
    String v = getString(s, key);
    return v != null ? v : def;
  }

  protected String getString(SubSectionKey s, String key) {
    return cfg.getString(s.section(), s.subSection(), key);
  }

  protected List<String> getStringList(SubSectionKey s, String key) {
    List<String> stringList = Arrays.asList(cfg.getStringList(s.section(), s.subSection(), key));
    stringList.replaceAll(str -> str == null ? "" : str);
    return Collections.unmodifiableList(stringList);
  }

  protected SubSectionKey subSectionKey(String section, String subSection) {
    return SubSectionKey.create(file, section, subSection);
  }
}
