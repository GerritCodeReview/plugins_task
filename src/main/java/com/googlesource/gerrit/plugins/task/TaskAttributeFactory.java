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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.common.PluginDefinedInfo;
import com.google.gerrit.index.query.Predicate;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.DynamicOptions.BeanProvider;
import com.google.gerrit.server.change.ChangeAttributeFactory;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
import com.googlesource.gerrit.plugins.task.TaskTree.Node;
import com.googlesource.gerrit.plugins.task.cli.PatchSetArgument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.errors.ConfigInvalidException;

public class TaskAttributeFactory implements ChangeAttributeFactory {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public enum Status {
    INVALID,
    UNKNOWN,
    WAITING,
    READY,
    PASS,
    FAIL;
  }

  public static class TaskAttribute {
    public Boolean applicable;
    public Boolean hasPass;
    public String hint;
    public Boolean inProgress;
    public String name;
    public Status status;
    public List<TaskAttribute> subTasks;

    public TaskAttribute(String name) {
      this.name = name;
    }
  }

  public static class TaskPluginAttribute extends PluginDefinedInfo {
    public List<TaskAttribute> roots = new ArrayList<>();
  }

  protected final TaskTree definitions;
  protected final ChangeQueryBuilder cqb;

  protected final Map<String, Predicate<ChangeData>> predicatesByQuery = new HashMap<>();

  protected Modules.MyOptions options;

  @Inject
  public TaskAttributeFactory(TaskTree definitions, ChangeQueryBuilder cqb) {
    this.definitions = definitions;
    this.cqb = cqb;
  }

  @Override
  public PluginDefinedInfo create(ChangeData c, BeanProvider beanProvider, String plugin) {
    options = (Modules.MyOptions) beanProvider.getDynamicBean(plugin);
    if (options.all || options.onlyApplicable || options.onlyInvalid) {
      for (PatchSetArgument psa : options.patchSetArguments) {
        definitions.masquerade(psa);
      }
      try {
        return createWithExceptions(c);
      } catch (StorageException e) {
        log.atSevere().withCause(e).log("Cannot load tasks for: %s", c);
      }
    }
    return null;
  }

  protected PluginDefinedInfo createWithExceptions(ChangeData c) {
    TaskPluginAttribute a = new TaskPluginAttribute();
    try {
      for (Node node : definitions.getRootNodes()) {
        addApplicableTasks(a.roots, c, node);
      }
    } catch (ConfigInvalidException | IOException e) {
      a.roots.add(invalid());
    }

    if (a.roots.isEmpty()) {
      return null;
    }
    return a;
  }

  protected void addApplicableTasks(List<TaskAttribute> tasks, ChangeData c, Node node) {
    try {
      Task def = node.definition;
      boolean applicable = match(c, def.applicable);
      if (!def.isVisible) {
        if (!def.isTrusted || (!applicable && !options.onlyApplicable)) {
          tasks.add(unknown());
          return;
        }
      }

      if (applicable || !options.onlyApplicable) {
        TaskAttribute task = new TaskAttribute(def.name);
        task.hasPass = def.pass != null || def.fail != null;
        task.subTasks = getSubTasks(c, node);
        task.status = getStatus(c, def, task);
        if (options.onlyInvalid && !isValidQueries(c, def)) {
          task.status = Status.INVALID;
        }
        boolean groupApplicable = task.status != null;

        if (groupApplicable || !options.onlyApplicable) {
          if (!options.onlyInvalid || task.status == Status.INVALID || task.subTasks != null) {
            if (!options.onlyApplicable) {
              task.applicable = applicable;
            }
            if (def.inProgress != null) {
              task.inProgress = matchOrNull(c, def.inProgress);
            }
            task.hint = getHint(task.status, def);
            tasks.add(task);
          }
        }
      }
    } catch (QueryParseException e) {
      tasks.add(invalid()); // bad applicability query
    }
  }

  protected List<TaskAttribute> getSubTasks(ChangeData c, Node node) {
    List<TaskAttribute> subTasks = new ArrayList<>();
    for (Node subNode : node.getSubNodes()) {
      if (subNode == null) {
        subTasks.add(invalid());
      } else {
        addApplicableTasks(subTasks, c, subNode);
      }
    }
    if (subTasks.isEmpty()) {
      return null;
    }
    return subTasks;
  }

  protected static TaskAttribute invalid() {
    // For security reasons, do not expose the task name without knowing
    // the visibility which is derived from its applicability.
    TaskAttribute a = unknown();
    a.status = Status.INVALID;
    return a;
  }

  protected static TaskAttribute unknown() {
    TaskAttribute a = new TaskAttribute("UNKNOWN");
    a.status = Status.UNKNOWN;
    return a;
  }

  protected boolean isValidQueries(ChangeData c, Task task) {
    try {
      match(c, task.inProgress);
      match(c, task.fail);
      match(c, task.pass);
      return true;
    } catch (StorageException | QueryParseException e) {
      return false;
    }
  }

  protected Status getStatus(ChangeData c, Task task, TaskAttribute a) {
    try {
      return getStatusWithExceptions(c, task, a);
    } catch (QueryParseException e) {
      return Status.INVALID;
    }
  }

  protected Status getStatusWithExceptions(ChangeData c, Task task, TaskAttribute a)
      throws QueryParseException {
    if (isAllNull(task.pass, task.fail, a.subTasks)) {
      // A leaf task has no defined subtasks.
      boolean hasDefinedSubtasks =
          !(task.subTasks.isEmpty()
              && task.subTasksFiles.isEmpty()
              && task.subTasksExternals.isEmpty());
      if (hasDefinedSubtasks) {
        // Remove 'Grouping" tasks (tasks with subtasks but no PASS
        // or FAIL criteria) from the output if none of their subtasks
        // are applicable.  i.e. grouping tasks only really apply if at
        // least one of their subtasks apply.
        return null;
      }
      // A leaf configuration without a PASS or FAIL criteria is a
      // missconfiguration.  Either someone forgot to add subtasks, or
      // they forgot to add a PASS or FAIL criteria.
      return Status.INVALID;
    }

    if (task.fail != null) {
      if (match(c, task.fail)) {
        // A FAIL definition is meant to be a hard blocking criteria
        // (like a CodeReview -2).  Thus, if hard blocked, it is
        // irrelevant what the subtask states, or the PASS criteria are.
        //
        // It is also important that FAIL be useable to indicate that
        // the task has actually executed.  Thus subtask status,
        // including a subtask FAIL should not appear as a FAIL on the
        // parent task.  This means that this is should be the only path
        // to make a task have a FAIL status.
        return Status.FAIL;
      }
      if (task.pass == null) {
        // A task with a FAIL but no PASS criteria is a PASS-FAIL task
        // (they are never "READY").  It didn't fail, so pass.
        return Status.PASS;
      }
    }

    if (a.subTasks != null && !isAll(a.subTasks, Status.PASS)) {
      // It is possible for a subtask's PASS criteria to change while
      // a parent task is executing, or even after the parent task
      // completes.  This can result in the parent PASS criteria being
      // met while one or more of its subtasks no longer meets its PASS
      // criteria (the subtask may now even meet a FAIL criteria).  We
      // never want the parent task to reflect a PASS criteria in these
      // cases, thus we can safely return here without ever evaluating
      // the task's PASS criteria.
      return Status.WAITING;
    }

    if (task.pass != null && !match(c, task.pass)) {
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

  protected String getHint(Status status, Task task) {
    if (status == Status.READY) {
      return task.readyHint;
    } else if (status == Status.FAIL) {
      return task.failHint;
    }
    return null;
  }

  protected static boolean isAll(Iterable<TaskAttribute> tasks, Status state) {
    for (TaskAttribute task : tasks) {
      if (task.status != state) {
        return false;
      }
    }
    return true;
  }

  protected boolean match(ChangeData c, String query) throws QueryParseException {
    if (query == null || query.equalsIgnoreCase("true")) {
      return true;
    }
    Predicate<ChangeData> pred = predicatesByQuery.get(query);
    if (pred == null) {
      pred = cqb.parse(query);
      predicatesByQuery.put(query, pred);
    }
    return pred.asMatchable().match(c);
  }

  protected Boolean matchOrNull(ChangeData c, String query) {
    if (query != null) {
      try {
        if (query.equalsIgnoreCase("true")) {
          return true;
        }
        return cqb.parse(query).asMatchable().match(c);
      } catch (StorageException | QueryParseException e) {
      }
    }
    return null;
  }

  protected static boolean isAllNull(Object... vals) {
    for (Object val : vals) {
      if (val != null) {
        return false;
      }
    }
    return true;
  }
}
