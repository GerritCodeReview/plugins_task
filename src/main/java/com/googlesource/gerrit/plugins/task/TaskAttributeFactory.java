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

import com.google.gerrit.extensions.common.PluginDefinedInfo;
import com.google.gerrit.index.query.Matchable;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.ChangeQueryProcessor;
import com.google.gerrit.server.query.change.ChangeQueryProcessor.ChangeAttributeFactory;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskAttributeFactory implements ChangeAttributeFactory {
  private static final Logger log = LoggerFactory.getLogger(TaskAttributeFactory.class);

  public enum Status {
    INVALID,
    WAITING,
    READY,
    PASS,
    FAIL;
  }

  public static class TaskAttribute {
    public Boolean inProgress;
    public String name;
    public String readyHint;
    public Status status;
    public List<TaskAttribute> subTasks;

    public TaskAttribute(String name) {
      this.name = name;
    }
  }

  public static class TaskPluginAttribute extends PluginDefinedInfo {
    public List<TaskAttribute> roots = new ArrayList<>();
  }

  protected final TaskConfigFactory taskFactory;
  protected final ChangeQueryBuilder cqb;

  @Inject
  public TaskAttributeFactory(TaskConfigFactory taskFactory, ChangeQueryBuilder cqb) {
    this.taskFactory = taskFactory;
    this.cqb = cqb;
  }

  @Override
  public PluginDefinedInfo create(ChangeData c, ChangeQueryProcessor qp, String plugin) {
    Modules.MyOptions options = (Modules.MyOptions) qp.getDynamicBean(plugin);
    if (options != null && options.include) {
      try {
        return createWithExceptions(c);
      } catch (OrmException | QueryParseException e) {
        log.error("Cannot load tasks for: " + c, e);
      }
    }
    return null;
  }

  protected PluginDefinedInfo createWithExceptions(ChangeData c)
      throws OrmException, QueryParseException {
    TaskPluginAttribute a = new TaskPluginAttribute();
    try {
      LinkedList<TaskDefinition> path = new LinkedList<>();
      for (TaskDefinition def : getRootTaskDefinitions()) {
        addApplicableTasks(a.roots, c, path, def);
      }
    } catch (ConfigInvalidException | IOException e) {
      a.roots.add(invalid());
    }

    if (a.roots.isEmpty()) {
      return null;
    }
    return a;
  }

  protected List<TaskAttribute> getSubTasks(
      ChangeData c, LinkedList<TaskDefinition> path, TaskDefinition parent)
      throws OrmException, QueryParseException {
    List<TaskAttribute> subTasks = new ArrayList<>();
    for (String name : parent.subTasks) {
      try {
        TaskDefinition def = getTaskDefinition(parent, name);
        addApplicableTasks(subTasks, c, path, def);
      } catch (ConfigInvalidException | IOException e) {
        subTasks.add(invalid());
      }
    }

    if (subTasks.isEmpty()) {
      return null;
    }
    return subTasks;
  }

  protected void addApplicableTasks(
      List<TaskAttribute> tasks, ChangeData c, LinkedList<TaskDefinition> path, TaskDefinition def)
      throws OrmException, QueryParseException {
    if (path.contains(def)) { // looping definition
      tasks.add(invalid());
      return;
    }
    path.addLast(def);
    addApplicableTasksNoLoopCheck(tasks, c, path, def);
    path.removeLast();
  }

  protected void addApplicableTasksNoLoopCheck(
      List<TaskAttribute> tasks, ChangeData c, LinkedList<TaskDefinition> path, TaskDefinition def)
      throws OrmException {
    try {
      if (match(c, def.applicable)) {
        TaskAttribute task = new TaskAttribute(def.name);
        if (def.inProgress != null) {
          task.inProgress = match(c, def.inProgress);
        }
        task.subTasks = getSubTasks(c, path, def);
        task.status = getStatus(c, def, task);
        if (task.status == Status.READY) {
          task.readyHint = def.readyHint;
        }
        tasks.add(task);
      }
    } catch (QueryParseException e) {
      tasks.add(invalid()); // bad query definition
    }
  }

  protected static TaskAttribute invalid() {
    // For security reasons, do not expose the task name without knowing
    // the visibility which is derived from its applicability.
    TaskAttribute a = new TaskAttribute("UNKNOWN");
    a.status = Status.INVALID;
    return a;
  }

  protected List<TaskDefinition> getRootTaskDefinitions()
      throws ConfigInvalidException, IOException {
    return taskFactory.getRootConfig().getRootTaskDefinitions();
  }

  protected TaskDefinition getTaskDefinition(TaskDefinition parent, String name)
      throws ConfigInvalidException, IOException {
    return taskFactory.getTaskConfig(parent.branch, parent.fileName).getTaskDefinition(name);
  }

  protected Status getStatus(ChangeData c, TaskDefinition def, TaskAttribute a)
      throws OrmException, QueryParseException {
    if (def.pass == null && a.subTasks == null) {
      // A leaf without a PASS criteria is likely a missconfiguration.
      // Either someone forgot to add subtasks, or they forgot to add
      // the pass criteria.
      return Status.INVALID;
    }

    if (def.fail != null && match(c, def.fail)) {
      // A FAIL definition is meant to be a hard blocking criteria
      // (like a CodeReview -2).  Thus, if hard blocked, it is
      // irrelevant what the subtask states, or the pass criteria are.
      //
      // It is also important that FAIL be useable to indicate that
      // the task has actually executed.  Thus subtask status,
      // including a subtask FAIL should not appear as a FAIL on the
      // parent task.  This means that this is should be the only path
      // to make a task have a FAIL status.
      return Status.FAIL;
    }

    if (a.subTasks != null && !isAll(a.subTasks, Status.PASS)) {
      // It is possible for a subtask's PASS criteria to change while
      // a parent task is executing, or even after the parent task
      // completes.  This can result in the parent PASS criteria being
      // met while one or more of its subtasks no longer meets its pass
      // criteria (the subtask may now even meet a fail criteria).  We
      // never want the parent task to reflect a PASS criteria in these
      // cases, thus we can safely return here without ever evaluating
      // the task's PASS criteria.
      return Status.WAITING;
    }

    if (def.pass != null && !match(c, def.pass)) {
      // Non-leaf tasks with no PASS criteria are supported in order
      // to support "grouping tasks" (tasks with no function aside from
      // organizing tasks).  A task without a PASS criteria, cannot ever
      // be expected to execute (how would you know if it has?), thus a
      // pass criteria is required to possibly even be considered for
      // READY.
      return Status.READY;
    }

    return Status.PASS;
  }

  protected static boolean isAll(Iterable<TaskAttribute> tasks, Status state) {
    for (TaskAttribute task : tasks) {
      if (task.status != state) {
        return false;
      }
    }
    return true;
  }

  protected boolean match(ChangeData c, String query) throws OrmException, QueryParseException {
    if (query == null) {
      return true;
    }
    return ((Matchable) cqb.parse(query)).match(c);
  }
}
