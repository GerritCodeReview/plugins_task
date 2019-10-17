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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.errors.ConfigInvalidException;

/** Task Configuration file living in git */
public class TaskConfig extends AbstractVersionedMetaData {
  protected class Section extends Container {
    public TaskConfig config;

    public Section() {
      this.config = TaskConfig.this;
    }
  }

  public class Task extends Section {
    public String applicable;
    public Map<String, String> exported;
    public String fail;
    public String failHint;
    public String inProgress;
    public String name;
    public String pass;
    public String preloadTask;
    public Map<String, String> properties;
    public String readyHint;
    public List<String> subTasks;
    public List<String> subTasksExternals;
    public List<String> subTasksFiles;

    public boolean isVisible;
    public boolean isTrusted;

    public Task(SubSection s, boolean isVisible, boolean isTrusted) {
      this.isVisible = isVisible;
      this.isTrusted = isTrusted;
      applicable = getString(s, KEY_APPLICABLE, null);
      exported = getProperties(s, KEY_EXPORT_PREFIX);
      fail = getString(s, KEY_FAIL, null);
      failHint = getString(s, KEY_FAIL_HINT, null);
      inProgress = getString(s, KEY_IN_PROGRESS, null);
      name = s.subSection;
      pass = getString(s, KEY_PASS, null);
      preloadTask = getString(s, KEY_PRELOAD_TASK, null);
      properties = getProperties(s, KEY_PROPERTIES_PREFIX);
      readyHint = getString(s, KEY_READY_HINT, null);
      subTasks = getStringList(s, KEY_SUBTASK);
      subTasksExternals = getStringList(s, KEY_SUBTASKS_EXTERNAL);
      subTasksFiles = getStringList(s, KEY_SUBTASKS_FILE);
    }
  }

  public class External extends Section {
    public String name;
    public String file;
    public String user;

    public External(SubSection s) {
      name = s.subSection;
      file = getString(s, KEY_FILE, null);
      user = getString(s, KEY_USER, null);
    }
  }

  protected static final Pattern OPTIONAL_TASK_PATTERN =
      Pattern.compile("([^ |]*( *[^ |])*) *\\| *");

  protected static final String SECTION_EXTERNAL = "external";
  protected static final String SECTION_ROOT = "root";
  protected static final String SECTION_TASK = "task";
  protected static final String KEY_APPLICABLE = "applicable";
  protected static final String KEY_EXPORT_PREFIX = "export-";
  protected static final String KEY_FAIL = "fail";
  protected static final String KEY_FAIL_HINT = "fail-hint";
  protected static final String KEY_FILE = "file";
  protected static final String KEY_IN_PROGRESS = "in-progress";
  protected static final String KEY_NAME = "name";
  protected static final String KEY_PASS = "pass";
  protected static final String KEY_PRELOAD_TASK = "preload-task";
  protected static final String KEY_PROPERTIES_PREFIX = "set-";
  protected static final String KEY_READY_HINT = "ready-hint";
  protected static final String KEY_SUBTASK = "subtask";
  protected static final String KEY_SUBTASKS_EXTERNAL = "subtasks-external";
  protected static final String KEY_SUBTASKS_FILE = "subtasks-file";
  protected static final String KEY_USER = "user";

  public boolean isVisible;
  public boolean isTrusted;

  public TaskConfig(BranchNameKey branch, String fileName, boolean isVisible, boolean isTrusted) {
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

  /* returs null only if optional and not found */
  public Task getTaskOptional(String name) throws ConfigInvalidException {
    int end = 0;
    Matcher m = OPTIONAL_TASK_PATTERN.matcher(name);
    while (m.find()) {
      end = m.end();
      Task task = getTaskOrNull(m.group(1));
      if (task != null) {
        return task;
      }
    }

    String last = name.substring(end);
    if (!"".equals(last)) { // Last entry was not optional
      Task task = getTaskOrNull(last);
      if (task != null) {
        return task;
      }
      throw new ConfigInvalidException("task not defined");
    }
    return null;
  }

  /* returns null if not found */
  protected Task getTaskOrNull(String name) {
    SubSection subSection = new SubSection(SECTION_TASK, name);
    return getNames(subSection).isEmpty() ? null : new Task(subSection, isVisible, isTrusted);
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
    return valueByName;
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
    return Arrays.asList(cfg.getStringList(s.section, s.subSection, key));
  }

  protected static class SubSection {
    public final String section;
    public final String subSection;

    protected SubSection(String section, String subSection) {
      this.section = section;
      this.subSection = subSection;
    }
  }
}
