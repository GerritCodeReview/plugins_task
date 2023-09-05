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
import com.google.gerrit.extensions.api.access.PluginPermission;
import com.google.gerrit.extensions.common.PluginDefinedInfo;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.DynamicOptions.BeanProvider;
import com.google.gerrit.server.change.ChangePluginDefinedInfoFactory;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
import com.googlesource.gerrit.plugins.task.TaskTree.Node;
import com.googlesource.gerrit.plugins.task.cli.PatchSetArgument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jgit.errors.ConfigInvalidException;

public class TaskAttributeFactory implements ChangePluginDefinedInfoFactory {
  public static final TaskPath MISSING_VIEW_PATH_CAPABILITY =
          new TaskPath(
                  String.format(
                          "Can't perform operation, need %s capability", ViewPathsCapability.VIEW_PATHS));
  public enum Status {
    INVALID,
    UNKNOWN,
    DUPLICATE,
    WAITING,
    READY,
    PASS,
    FAIL;
  }

  public static class Statistics {
    public long numberOfChanges;
    public long numberOfChangeNodes;
    public long numberOfDuplicates;
    public long numberOfNodes;
    public long numberOfTaskPluginAttributes;
    public Object predicateCache;
    public Object matchCache;
    public Preloader.Statistics preloader;
    public TaskTree.Statistics treeCaches;
  }

  public static class TaskAttribute {
    public static class Statistics {
      public boolean isApplicableRefreshRequired;
      public boolean isSubNodeReloadRequired;
      public boolean isTaskRefreshNeeded;
      public Boolean hasUnfilterableSubNodes;
      public Object nodesByBranchCache;
      public Object properties;
    }

    public Boolean applicable;
    public Map<String, String> exported;
    public Boolean hasPass;
    public String hint;
    public Boolean inProgress;
    public TaskPath path;
    public String name;
    public Integer change;
    public Status status;
    public List<TaskAttribute> subTasks;
    public Long evaluationMilliSeconds;
    public Statistics statistics;

    public TaskAttribute(String name) {
      this.name = name;
    }
  }

  public static class TaskPluginAttribute extends PluginDefinedInfo {
    public List<TaskAttribute> roots = new ArrayList<>();
    public Statistics queryStatistics;
  }

  protected final String pluginName;
  protected final TaskTree definitions;
  protected final PredicateCache predicateCache;
  protected final boolean hasViewPathsCapability;
  protected final TaskPath.Factory taskPathFactory;

  protected Modules.MyOptions options;
  protected TaskPluginAttribute lastTaskPluginAttribute;
  protected Statistics statistics;

  @Inject
  public TaskAttributeFactory(
      String pluginName,
      TaskTree definitions,
      PredicateCache predicateCache,
      PermissionBackend permissionBackend,
      TaskPath.Factory taskPathFactory) {
    this.pluginName = pluginName;
    this.definitions = definitions;
    this.predicateCache = predicateCache;
    this.hasViewPathsCapability =
        permissionBackend
            .currentUser()
            .testOrFalse(new PluginPermission(this.pluginName, ViewPathsCapability.VIEW_PATHS));
    this.taskPathFactory = taskPathFactory;
  }

  @Override
  public Map<Change.Id, PluginDefinedInfo> createPluginDefinedInfos(
      Collection<ChangeData> cds, BeanProvider beanProvider, String plugin) {
    Map<Change.Id, PluginDefinedInfo> pluginInfosByChange = new HashMap<>();
    options = (Modules.MyOptions) beanProvider.getDynamicBean(plugin);
    if (options.all || options.onlyApplicable || options.onlyInvalid) {
      initStatistics();
      for (PatchSetArgument psa : options.patchSetArguments) {
        definitions.masquerade(psa);
      }
      cds.forEach(cd -> pluginInfosByChange.put(cd.getId(), createWithExceptions(cd)));
      if (lastTaskPluginAttribute != null) {
        lastTaskPluginAttribute.queryStatistics = getStatistics(pluginInfosByChange);
      }
    }
    return pluginInfosByChange;
  }

  protected PluginDefinedInfo createWithExceptions(ChangeData c) {
    TaskPluginAttribute a = new TaskPluginAttribute();
    try {
      for (Node node : definitions.getRootNodes(c)) {
        if (node instanceof Node.Invalid) {
          a.roots.add(invalid());
        } else {
          new AttributeFactory(node).create().ifPresent(t -> a.roots.add(t));
        }
      }
    } catch (ConfigInvalidException | IOException | StorageException e) {
      a.roots.add(invalid());
    }

    if (a.roots.isEmpty()) {
      return null;
    }
    lastTaskPluginAttribute = a;
    return a;
  }

  protected class AttributeFactory {
    public Node node;
    protected Task task;
    protected TaskAttribute attribute;

    protected AttributeFactory(Node node) {
      this.node = node;
      this.task = node.task;
      attribute = new TaskAttribute(task.name());
      if (options.includeStatistics) {
        statistics.numberOfNodes++;
        if (node.isChange()) {
          statistics.numberOfChangeNodes++;
        }
        if (node.isDuplicate) {
          statistics.numberOfDuplicates++;
        }
        attribute.statistics = new TaskAttribute.Statistics();
        attribute.statistics.properties = node.propertiesStatistics;
      }
    }

    public Optional<TaskAttribute> create() {
      try {
        if (options.evaluationTime) {
          attribute.evaluationMilliSeconds = millis();
        }

        boolean applicable;
        try {
          applicable = node.match(task.applicable);
        } catch (QueryParseException e) {
          return Optional.of(invalid());
        }
        if (!task.isVisible) {
          if (!node.isTrusted() || (!applicable && !options.onlyApplicable)) {
            return Optional.of(unknown());
          }
        }

        if (applicable || !options.onlyApplicable) {
          if (node.isChange()) {
            attribute.change = node.getChangeData().getId().get();
          }
          attribute.hasPass = !node.isDuplicate && (task.pass != null || task.fail != null);
          if (!node.isDuplicate) {
            attribute.subTasks = getSubTasks();
          }
          attribute.status = getStatus();
          if (options.onlyInvalid && !isValidQueries()) {
            attribute.status = Status.INVALID;
          }
          if (options.includePaths) {
            if (hasViewPathsCapability) {
              attribute.path = taskPathFactory.create(node.taskKey);
            } else {
              attribute.path = MISSING_VIEW_PATH_CAPABILITY;
            }
          }
          boolean groupApplicable = attribute.status != null;

          if (groupApplicable || !options.onlyApplicable) {
            if (!options.onlyInvalid
                || attribute.status == Status.INVALID
                || attribute.subTasks != null) {
              if (!options.onlyApplicable) {
                attribute.applicable = applicable;
              }
              if (!node.isDuplicate) {
                if (task.inProgress != null) {
                  attribute.inProgress = node.matchOrNull(task.inProgress);
                }
                attribute.exported = task.exported.isEmpty() ? null : task.exported;
              }
              attribute.hint = getHint(attribute.status, task);

              if (options.evaluationTime) {
                attribute.evaluationMilliSeconds = millis() - attribute.evaluationMilliSeconds;
              }
              addStatistics(attribute.statistics);
              return Optional.of(attribute);
            }
          }
        }
      } catch (IOException | RuntimeException e) {
        return Optional.of(invalid()); // bad applicability query
      }
      return Optional.empty();
    }

    protected TaskAttribute invalid() {
      TaskAttribute invalid = TaskAttributeFactory.invalid();
      if (task.isVisible) {
        invalid.name = task.name();
      }
      return invalid;
    }

    public void addStatistics(TaskAttribute.Statistics statistics) {
      if (statistics != null) {
        statistics.isApplicableRefreshRequired = node.properties.isApplicableRefreshRequired();
        statistics.isSubNodeReloadRequired = node.properties.isSubNodeReloadRequired();
        statistics.isTaskRefreshNeeded = node.properties.isTaskRefreshRequired();
        if (!statistics.isSubNodeReloadRequired) {
          statistics.hasUnfilterableSubNodes = node.hasUnfilterableSubNodes;
        }
        if (node.nodesByBranch != null) {
          statistics.nodesByBranchCache = node.nodesByBranch.getStatistics();
        }
      }
    }

    protected Status getStatusWithExceptions() throws StorageException, QueryParseException {
      if (node.isDuplicate) {
        return Status.DUPLICATE;
      }
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
        if (node.match(task.fail)) {
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

      if (attribute.subTasks != null
          && !isAll(attribute.subTasks, EnumSet.of(Status.PASS, Status.DUPLICATE))) {
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

      if (task.pass != null && !node.match(task.pass)) {
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

    protected List<TaskAttribute> getSubTasks() throws IOException, StorageException {
      List<TaskAttribute> subTasks = new ArrayList<>();
      for (Node subNode :
          options.onlyApplicable ? node.getApplicableSubNodes() : node.getSubNodes()) {
        if (subNode instanceof Node.Invalid) {
          subTasks.add(TaskAttributeFactory.invalid());
        } else {
          new AttributeFactory(subNode).create().ifPresent(t -> subTasks.add(t));
        }
      }
      if (subTasks.isEmpty()) {
        return null;
      }
      return subTasks;
    }

    protected boolean isValidQueries() {
      try {
        node.match(task.inProgress);
        node.match(task.fail);
        node.match(task.pass);
        return true;
      } catch (QueryParseException | RuntimeException e) {
        return false;
      }
    }
  }

  protected long millis() {
    return System.nanoTime() / 1000000;
  }

  public void initStatistics() {
    if (options.includeStatistics) {
      statistics = new Statistics();
      definitions.predicateCache.initStatistics(options.summaryCount);
      definitions.matchCache.initStatistics(options.summaryCount);
      definitions.preloader.initStatistics(options.summaryCount);
      definitions.initStatistics(options.summaryCount);
    }
  }

  public Statistics getStatistics(Map<Change.Id, PluginDefinedInfo> pluginInfosByChange) {
    if (statistics != null) {
      statistics.numberOfChanges = pluginInfosByChange.size();
      statistics.numberOfTaskPluginAttributes =
          pluginInfosByChange.values().stream().filter(tpa -> tpa != null).count();
      statistics.predicateCache = definitions.predicateCache.getStatistics();
      statistics.matchCache = definitions.matchCache.getStatistics();
      statistics.preloader = definitions.preloader.getStatistics();
      statistics.treeCaches = definitions.getStatistics();
    }
    return statistics;
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

  protected static String getHint(Status status, Task def) {
    if (status != null) {
      switch (status) {
        case READY:
          return def.readyHint;
        case FAIL:
          return def.failHint;
        case DUPLICATE:
          return "Duplicate task is non blocking and empty to break the loop";
        default:
      }
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

  protected static boolean isAll(Iterable<TaskAttribute> atts, Set<Status> states) {
    for (TaskAttribute att : atts) {
      if (!states.contains(att.status)) {
        return false;
      }
    }
    return true;
  }
}
