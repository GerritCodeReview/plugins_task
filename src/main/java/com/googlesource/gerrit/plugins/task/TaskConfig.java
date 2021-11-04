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
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.server.git.meta.AbstractVersionedMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.errors.ConfigInvalidException;

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

  protected class Section extends Container {
    public TaskConfig config;
    public SubSection subSection;

    public Section(SubSection s) {
      this.config = TaskConfig.this;
      this.subSection = s;
    }

    public String key() {
      return config.key() + SEP + subSection.key();
    }
  }

  public class TaskBase extends Section {
    public String applicable;
    public Map<String, String> exported;
    public String fail;
    public String failHint;
    public String inProgress;
    public String pass;
    public String preloadTask;
    public Map<String, String> properties;
    public String readyHint;
    public List<String> subTasks;
    public List<String> subTasksExternals;
    public List<String> subTasksFactories;
    public List<String> subTasksFiles;

    public boolean isVisible;
    public boolean isTrusted;

    public TaskBase(SubSection s, boolean isVisible, boolean isTrusted) {
      super(s);
      this.isVisible = isVisible;
      this.isTrusted = isTrusted;
      applicable = getString(s, KEY_APPLICABLE, null);
      exported = getProperties(s, KEY_EXPORT_PREFIX);
      fail = getString(s, KEY_FAIL, null);
      failHint = getString(s, KEY_FAIL_HINT, null);
      inProgress = getString(s, KEY_IN_PROGRESS, null);
      pass = getString(s, KEY_PASS, null);
      preloadTask = getString(s, KEY_PRELOAD_TASK, null);
      properties = getProperties(s, KEY_PROPERTIES_PREFIX);
      readyHint = getString(s, KEY_READY_HINT, null);
      subTasks = getStringList(s, KEY_SUBTASK);
      subTasksExternals = getStringList(s, KEY_SUBTASKS_EXTERNAL);
      subTasksFactories = getStringList(s, KEY_SUBTASKS_FACTORY);
      subTasksFiles = getStringList(s, KEY_SUBTASKS_FILE);
    }

    protected TaskBase(TaskBase base) {
      super(base.subSection);
      Copier.deepCopyDeclaredFields(TaskBase.class, base, this, false);
    }
  }

  public class Task extends TaskBase {
    public String name;

    public Task(SubSection s, boolean isVisible, boolean isTrusted) {
      super(s, isVisible, isTrusted);
      name = s.subSection;
    }

    public Task(Task task) {
      super(task);
      Copier.deepCopyDeclaredFields(Task.class, task, this, false);
    }

    public Task(TasksFactory tasks, String name) {
      super(tasks);
      this.name = name;
    }

    protected Map<String, String> getAllProperties() {
      Map<String, String> all = new HashMap<>(properties);
      all.putAll(exported);
      return all;
    }

    public String key() {
      // name is needed to differentiate Tasks from the same TasksFactory
      return super.key() + SEP + name;
    }
  }

  public class TasksFactory extends TaskBase {
    public String namesFactory;

    public TasksFactory(SubSection s, boolean isVisible, boolean isTrusted) {
      super(s, isVisible, isTrusted);
      namesFactory = getString(s, KEY_NAMES_FACTORY, null);
    }
  }

  public class NamesFactory extends Section {
    public String changes;
    public List<String> names;
    public String type;

    public NamesFactory(SubSection s) {
      super(s);
      changes = getString(s, KEY_CHANGES, null);
      names = getStringList(s, KEY_NAME);
      type = getString(s, KEY_TYPE, null);
    }

    public NamesFactory(NamesFactory n) {
      super(n.subSection);
      Copier.deepCopyDeclaredFields(NamesFactory.class, n, this, false);
    }
  }

  public class External extends Section {
    public String name;
    public String file;
    public String user;

    public External(SubSection s) {
      super(s);
      name = s.subSection;
      file = getString(s, KEY_FILE, null);
      user = getString(s, KEY_USER, null);
    }
  }

  public static final String SEP = "\0";

  protected static final Pattern OPTIONAL_TASK_PATTERN =
      Pattern.compile("([^ |]*( *[^ |])*) *\\| *");

  protected static final String SECTION_EXTERNAL = "external";
  protected static final String SECTION_NAMES_FACTORY = "names-factory";
  protected static final String SECTION_ROOT = "root";
  protected static final String SECTION_TASK = "task";
  protected static final String SECTION_TASKS_FACTORY = "tasks-factory";
  protected static final String KEY_APPLICABLE = "applicable";
  protected static final String KEY_CHANGES = "changes";
  protected static final String KEY_EXPORT_PREFIX = "export-";
  protected static final String KEY_FAIL = "fail";
  protected static final String KEY_FAIL_HINT = "fail-hint";
  protected static final String KEY_FILE = "file";
  protected static final String KEY_IN_PROGRESS = "in-progress";
  protected static final String KEY_NAME = "name";
  protected static final String KEY_NAMES_FACTORY = "names-factory";
  protected static final String KEY_PASS = "pass";
  protected static final String KEY_PRELOAD_TASK = "preload-task";
  protected static final String KEY_PROPERTIES_PREFIX = "set-";
  protected static final String KEY_READY_HINT = "ready-hint";
  protected static final String KEY_SUBTASK = "subtask";
  protected static final String KEY_SUBTASKS_EXTERNAL = "subtasks-external";
  protected static final String KEY_SUBTASKS_FACTORY = "subtasks-factory";
  protected static final String KEY_SUBTASKS_FILE = "subtasks-file";
  protected static final String KEY_TYPE = "type";
  protected static final String KEY_USER = "user";

  public boolean isVisible;
  public boolean isTrusted;

  public TaskConfig(Branch.NameKey branch, String fileName, boolean isVisible, boolean isTrusted) {
    super(branch, fileName);
    this.isVisible = isVisible;
    this.isTrusted = isTrusted;
  }

  public List<Task> getRootTasks() {
    return getTasks(SECTION_ROOT);
  }

  public List<Task> getTasks() {
    return getTasks(SECTION_TASK);
  }

  protected List<Task> getTasks(String type) {
    List<Task> tasks = new ArrayList<>();
    // No need to get a task with no name (what would we call it?)
    for (String task : cfg.getSubsections(type)) {
      tasks.add(new Task(new SubSection(type, task), isVisible, isTrusted));
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

  /**
   * Get a Task for this expression.
   *
   * @param expression A task expression represents a config string pointing to an expression which
   *     includes zero or more task names separated by a '|', and potentially termintated by a '|'.
   *     If the expression is not terminated by a '|' it indicates that task resolution of at least
   *     one task is required. Task selection priority is from left to right. This can be expressed
   *     as: <code>EXPR = [ TASK_NAME '|' ] TASK_NAME [ '|' ]</code>
   *     <p>Example expressions to prioritized names and requirements:
   *     <ul>
   *       <li><code> "simple"        -> ("simple")         required</code>
   *       <li><code> "world | peace" -> ("world", "peace") required</code>
   *       <li><code> "shadenfreud |" -> ("shadenfreud")    optional</code>
   *       <li><code> "foo | bar |"   -> ("foo", "bar")     optional</code>
   *     </ul>
   *
   * @return Optional<Task> which is empty if the expression is optional and no tasks are resolved
   * @throws ConfigInvalidException if the expression requires a task and no tasks are resolved
   */
  public Optional<Task> getOptionalTaskForExpression(String expression)
      throws ConfigInvalidException {
    int end = 0;
    Matcher m = OPTIONAL_TASK_PATTERN.matcher(expression);
    while (m.find()) {
      end = m.end();
      Optional<Task> task = getOptionalTask(m.group(1));
      if (task.isPresent()) {
        return task;
      }
    }

    String last = expression.substring(end);
    if (!"".equals(last)) { // expression was not optional
      Optional<Task> task = getOptionalTask(last);
      if (task.isPresent()) {
        return task;
      }
      throw new ConfigInvalidException("task not defined");
    }
    return Optional.empty();
  }

  protected Optional<Task> getOptionalTask(String name) {
    SubSection subSection = new SubSection(SECTION_TASK, name);
    return getNames(subSection).isEmpty()
        ? Optional.empty()
        : Optional.of(new Task(subSection, isVisible, isTrusted));
  }

  public TasksFactory getTasksFactory(String name) {
    return new TasksFactory(new SubSection(SECTION_TASKS_FACTORY, name), isVisible, isTrusted);
  }

  public NamesFactory getNamesFactory(String name) {
    return new NamesFactory(new SubSection(SECTION_NAMES_FACTORY, name));
  }

  public External getExternal(String name) {
    return getExternal(new SubSection(SECTION_EXTERNAL, name));
  }

  protected External getExternal(SubSection s) {
    return new External(s);
  }

  protected Map<String, String> getProperties(SubSection s, String prefix) {
    Map<String, String> valueByName = new HashMap<>();
    for (Map.Entry<String, String> e :
        getStringByName(s, getMatchingNames(s, prefix + ".+")).entrySet()) {
      String name = e.getKey();
      valueByName.put(name.substring(prefix.length()), e.getValue());
    }
    return Collections.unmodifiableMap(valueByName);
  }

  protected Map<String, String> getStringByName(SubSection s, Iterable<String> names) {
    Map<String, String> valueByName = new HashMap<>();
    for (String name : names) {
      valueByName.put(name, getString(s, name));
    }
    return valueByName;
  }

  protected Set<String> getMatchingNames(SubSection s, String match) {
    Set<String> matched = new HashSet<>();
    for (String name : getNames(s)) {
      if (name.matches(match)) {
        matched.add(name);
      }
    }
    return matched;
  }

  protected Set<String> getNames(SubSection s) {
    return cfg.getNames(s.section, s.subSection);
  }

  protected String getString(SubSection s, String key, String def) {
    String v = getString(s, key);
    return v != null ? v : def;
  }

  protected String getString(SubSection s, String key) {
    return cfg.getString(s.section, s.subSection, key);
  }

  protected List<String> getStringList(SubSection s, String key) {
    return Collections.unmodifiableList(
        Arrays.asList(cfg.getStringList(s.section, s.subSection, key)));
  }

  public String key() {
    return branch.getParentKey() + SEP + branch.get() + SEP + fileName;
  }

  protected static class SubSection {
    public final String section;
    public final String subSection;

    protected SubSection(String section, String subSection) {
      this.section = section;
      this.subSection = subSection;
    }

    public String key() {
      return section + SEP + subSection;
    }
  }
}
