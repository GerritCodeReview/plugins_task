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
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.config.AllUsersNameProvider;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.ChangeQueryProcessor;
import com.google.gerrit.server.query.change.ChangeQueryProcessor.ChangeAttributeFactory;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.task.TaskConfig.External;
import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

  protected static final String TASK_DIR = "task";

  protected final AccountResolver accountResolver;
  protected final AllUsersNameProvider allUsers;
  protected final TaskConfigFactory taskFactory;
  protected final ChangeQueryBuilder cqb;

  @Inject
  public TaskAttributeFactory(
      AccountResolver accountResolver,
      AllUsersNameProvider allUsers,
      TaskConfigFactory taskFactory,
      ChangeQueryBuilder cqb) {
    this.accountResolver = accountResolver;
    this.allUsers = allUsers;
    this.taskFactory = taskFactory;
    this.cqb = cqb;
  }

  @Override
  public PluginDefinedInfo create(ChangeData c, ChangeQueryProcessor qp, String plugin) {
    Modules.MyOptions options = (Modules.MyOptions) qp.getDynamicBean(plugin);
    if (options != null && options.include) {
      try {
        return createWithExceptions(c);
      } catch (OrmException e) {
        log.error("Cannot load tasks for: " + c, e);
      }
    }
    return null;
  }

  protected PluginDefinedInfo createWithExceptions(ChangeData c) throws OrmException {
    TaskPluginAttribute a = new TaskPluginAttribute();
    try {
      LinkedList<Task> path = new LinkedList<>();
      for (Task task : getRootTasks()) {
        addApplicableTasks(a.roots, c, path, task);
      }
    } catch (ConfigInvalidException | IOException e) {
      a.roots.add(invalid());
    }

    if (a.roots.isEmpty()) {
      return null;
    }
    return a;
  }

  protected void addApplicableTasks(
      List<TaskAttribute> tasks, ChangeData c, LinkedList<Task> path, Task def)
      throws OrmException {
    if (path.contains(def)) { // looping definition
      tasks.add(invalid());
      return;
    }
    path.addLast(def);
    addApplicableTasksNoLoopCheck(tasks, c, path, def);
    path.removeLast();
  }

  protected void addApplicableTasksNoLoopCheck(
      List<TaskAttribute> tasks, ChangeData c, LinkedList<Task> path, Task def)
      throws OrmException {
    try {
      if (match(c, def.applicable)) {
        TaskAttribute task = new TaskAttribute(def.name);
        if (def.inProgress != null) {
          task.inProgress = match(c, def.inProgress);
        }
        task.subTasks = getSubTasks(c, path, def);
        task.status = getStatus(c, def, task);
        if (task.status != null) { // task still applies
          if (task.status == Status.READY) {
            task.readyHint = def.readyHint;
          }
          tasks.add(task);
        }
      }
    } catch (QueryParseException e) {
      tasks.add(invalid()); // bad applicability query
    }
  }

  protected List<TaskAttribute> getSubTasks(ChangeData c, LinkedList<Task> path, Task parent)
      throws OrmException {
    List<Task> tasks = getSubTasks(parent);

    List<TaskAttribute> subTasks = new ArrayList<>();
    for (String file : parent.subTasksFiles) {
      try {
        tasks.addAll(getTasks(parent.config.getBranch(), resolveTaskFileName(file)));
      } catch (ConfigInvalidException | IOException e) {
        subTasks.add(invalid());
      }
    }
    for (String external : parent.subTasksExternals) {
      try {
        External ext = parent.config.getExternal(external);
        if (ext == null) {
          subTasks.add(invalid());
        } else {
          tasks.addAll(getTasks(ext));
        }
      } catch (ConfigInvalidException | IOException e) {
        subTasks.add(invalid());
      }
    }

    for (Task task : tasks) {
      addApplicableTasks(subTasks, c, path, task);
    }

    if (subTasks.isEmpty()) {
      return null;
    }
    return subTasks;
  }

  protected static TaskAttribute invalid() {
    // For security reasons, do not expose the task name without knowing
    // the visibility which is derived from its applicability.
    TaskAttribute a = new TaskAttribute("UNKNOWN");
    a.status = Status.INVALID;
    return a;
  }

  protected List<Task> getRootTasks() throws ConfigInvalidException, IOException {
    return taskFactory.getRootConfig().getRootTasks();
  }

  protected List<Task> getSubTasks(Task parent) {
    List<Task> tasks = new ArrayList<>();
    for (String name : parent.subTasks) {
      tasks.add(parent.config.getTask(name));
    }
    return tasks;
  }

  protected List<Task> getTasks(External external)
      throws ConfigInvalidException, IOException, OrmException {
    return getTasks(resolveUserBranch(external.user), resolveTaskFileName(external.file));
  }

  protected List<Task> getTasks(Branch.NameKey branch, String file)
      throws ConfigInvalidException, IOException {
    return taskFactory.getTaskConfig(branch, file).getTasks();
  }

  protected String resolveTaskFileName(String file) throws ConfigInvalidException {
    if (file == null) {
      throw new ConfigInvalidException("External file not defined");
    }
    Path p = Paths.get(TASK_DIR, file);
    if (!p.startsWith(TASK_DIR)) {
      throw new ConfigInvalidException("task file not under " + TASK_DIR + " directory: " + file);
    }
    return p.toString();
  }

  protected Branch.NameKey resolveUserBranch(String user)
      throws ConfigInvalidException, IOException, OrmException {
    if (user == null) {
      throw new ConfigInvalidException("External user not defined");
    }
    Account acct = accountResolver.find(user);
    if (acct == null) {
      throw new ConfigInvalidException("Cannot resolve user: " + user);
    }
    return new Branch.NameKey(allUsers.get(), RefNames.refsUsers(acct.getId()));
  }

  protected Status getStatus(ChangeData c, Task task, TaskAttribute a) throws OrmException {
    try {
      return getStatusWithExceptions(c, task, a);
    } catch (QueryParseException e) {
      return Status.INVALID;
    }
  }

  protected Status getStatusWithExceptions(ChangeData c, Task task, TaskAttribute a)
      throws OrmException, QueryParseException {
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

  protected static boolean isAll(Iterable<TaskAttribute> tasks, Status state) {
    for (TaskAttribute task : tasks) {
      if (task.status != state) {
        return false;
      }
    }
    return true;
  }

  protected boolean match(ChangeData c, String query) throws OrmException, QueryParseException {
    if (query == null || query.equalsIgnoreCase("true")) {
      return true;
    }
    return ((Matchable) cqb.parse(query)).match(c);
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
