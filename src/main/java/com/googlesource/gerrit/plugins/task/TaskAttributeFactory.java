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
import com.google.gerrit.entities.Change;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.common.PluginDefinedInfo;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.DynamicOptions.BeanProvider;
import com.google.gerrit.server.change.ChangePluginDefinedInfoFactory;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
import com.googlesource.gerrit.plugins.task.TaskTree.Node;
import com.googlesource.gerrit.plugins.task.cli.PatchSetArgument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;

public class TaskAttributeFactory implements ChangePluginDefinedInfoFactory {
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
    public Map<String, String> exported;
    public Boolean hasPass;
    public String hint;
    public Boolean inProgress;
    public String name;
    public Status status;
    public List<TaskAttribute> subTasks;
    public Long evaluationMilliSeconds;

    public TaskAttribute(String name) {
      this.name = name;
    }
  }

  public static class TaskPluginAttribute extends PluginDefinedInfo {
    public List<TaskAttribute> roots = new ArrayList<>();
  }

  protected final TaskTree definitions;
  protected final PredicateCache predicateCache;

  protected Modules.MyOptions options;

  @Inject
  public TaskAttributeFactory(TaskTree definitions, PredicateCache predicateCache) {
    this.definitions = definitions;
    this.predicateCache = predicateCache;
  }

  @Override
  public Map<Change.Id, PluginDefinedInfo> createPluginDefinedInfos(
          Collection<ChangeData> cds, BeanProvider beanProvider, String plugin) {
    Map<Change.Id, PluginDefinedInfo> pluginInfosByChange = new HashMap<>();
    options = (Modules.MyOptions) beanProvider.getDynamicBean(plugin);
    if (options.all || options.onlyApplicable || options.onlyInvalid) {
      for (PatchSetArgument psa : options.patchSetArguments) {
        definitions.masquerade(psa);
      }
      cds.forEach(cd -> pluginInfosByChange.put(cd.getId(), createWithExceptions(cd)));
    }
    return pluginInfosByChange;
  }

  protected PluginDefinedInfo createWithExceptions(ChangeData c) {
    TaskPluginAttribute a = new TaskPluginAttribute();
    try {
      for (Node node : definitions.getRootNodes(c)) {
        if (node == null) {
          a.roots.add(invalid());
        } else {
          new AttributeFactory(node).create().ifPresent(t -> a.roots.add(t));
        }
      }
    } catch (ConfigInvalidException | IOException e) {
      a.roots.add(invalid());
    }

    if (a.roots.isEmpty()) {
      return null;
    }
    return a;
  }

  protected class AttributeFactory {
    public Node node;
    public MatchCache matchCache;
    protected Task task;
    protected TaskAttribute attribute;

    protected AttributeFactory(Node node) {
      this(node, new MatchCache(predicateCache, node.getChangeData()));
    }

    protected AttributeFactory(Node node, MatchCache matchCache) {
      this.node = node;
      this.matchCache = matchCache;
      this.task = node.task;
      this.attribute = new TaskAttribute(task.name);
    }

    public Optional<TaskAttribute> create() {
      try {
        if (options.evaluationTime) {
          attribute.evaluationMilliSeconds = millis();
        }

        boolean applicable = matchCache.match(task.applicable);
        if (!task.isVisible) {
          if (!task.isTrusted || (!applicable && !options.onlyApplicable)) {
            return Optional.of(unknown());
          }
        }

        if (applicable || !options.onlyApplicable) {
          attribute.hasPass = task.pass != null || task.fail != null;
          attribute.subTasks = getSubTasks();
          attribute.status = getStatus();
          if (options.onlyInvalid && !isValidQueries()) {
            attribute.status = Status.INVALID;
          }
          boolean groupApplicable = attribute.status != null;

          if (groupApplicable || !options.onlyApplicable) {
            if (!options.onlyInvalid
                || attribute.status == Status.INVALID
                || attribute.subTasks != null) {
              if (!options.onlyApplicable) {
                attribute.applicable = applicable;
              }
              if (task.inProgress != null) {
                attribute.inProgress = matchCache.matchOrNull(task.inProgress);
              }
              attribute.hint = getHint(attribute.status, task);
              attribute.exported = task.exported.isEmpty() ? null : task.exported;

              if (options.evaluationTime) {
                attribute.evaluationMilliSeconds = millis() - attribute.evaluationMilliSeconds;
              }
              return Optional.of(attribute);
            }
          }
        }
      } catch (QueryParseException | RuntimeException e) {
        return Optional.of(invalid()); // bad applicability query
      }
      return Optional.empty();
    }

    protected Status getStatusWithExceptions() throws StorageException, QueryParseException {
      if (isAllNull(task.pass, task.fail, attribute.subTasks)) {
        // A leaf def has no defined subdefs.
        boolean hasDefinedSubtasks =
            !(task.subTasks.isEmpty()
                && task.subTasksFiles.isEmpty()
                && task.subTasksExternals.isEmpty()
                && task.subTasksFactories.isEmpty());
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
        if (matchCache.match(task.fail)) {
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
      }

      if (attribute.subTasks != null && !isAll(attribute.subTasks, Status.PASS)) {
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

      if (task.pass != null && !matchCache.match(task.pass)) {
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

    protected Status getStatus() {
      try {
        return getStatusWithExceptions();
      } catch (QueryParseException | RuntimeException e) {
        return Status.INVALID;
      }
    }

    protected List<TaskAttribute> getSubTasks() throws StorageException {
      List<TaskAttribute> subTasks = new ArrayList<>();
      for (Node subNode : node.getSubNodes()) {
        if (subNode == null) {
          subTasks.add(invalid());
        } else {
          new AttributeFactory(subNode, matchCache).create().ifPresent(t -> subTasks.add(t));
        }
      }
      if (subTasks.isEmpty()) {
        return null;
      }
      return subTasks;
    }

    protected boolean isValidQueries() {
      try {
        matchCache.match(task.inProgress);
        matchCache.match(task.fail);
        matchCache.match(task.pass);
        return true;
      } catch (QueryParseException | RuntimeException e) {
        return false;
      }
    }
  }

  protected long millis() {
    return System.nanoTime() / 1000000;
  }

  protected TaskAttribute invalid() {
    // For security reasons, do not expose the task name without knowing
    // the visibility which is derived from its applicability.
    TaskAttribute a = unknown();
    a.status = Status.INVALID;
    return a;
  }

  protected TaskAttribute unknown() {
    TaskAttribute a = new TaskAttribute("UNKNOWN");
    a.status = Status.UNKNOWN;
    return a;
  }

  protected String getHint(Status status, Task def) {
    if (status == Status.READY) {
      return def.readyHint;
    } else if (status == Status.FAIL) {
      return def.failHint;
    }
    return null;
  }

  public static boolean isAllNull(Object... vals) {
    for (Object val : vals) {
      if (val != null) {
        return false;
      }
    }
    return true;
  }

  protected static boolean isAll(Iterable<TaskAttribute> atts, Status state) {
    for (TaskAttribute att : atts) {
      if (att.status != state) {
        return false;
      }
    }
    return true;
  }
}
