// Copyright (C) 2024 The Android Open Source Project
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

import static com.googlesource.gerrit.plugins.task.TaskPluginDefinedInfoFactory.unknown;

import com.google.auto.value.AutoValue;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.api.access.PluginPermission;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.googlesource.gerrit.plugins.task.TaskPluginDefinedInfoFactory.Statistics;
import com.googlesource.gerrit.plugins.task.TaskPluginDefinedInfoFactory.Status;
import com.googlesource.gerrit.plugins.task.TaskPluginDefinedInfoFactory.TaskAttribute;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;

public class TaskAttributeFactory {
  public static final TaskPath MISSING_VIEW_PATH_CAPABILITY =
      new TaskPath(
          String.format(
              "Can't perform operation, need %s capability", ViewPathsCapability.VIEW_PATHS));
  private final Options options;

  public TaskTree.Node node;
  protected TaskConfig.Task task;
  protected TaskAttribute attribute;
  TaskPath.Factory taskPathFactory;
  protected final boolean hasViewPathsCapability;
  protected Statistics statistics;
  protected final TaskAttributeFactory.Factory factory;

  @AutoValue
  public abstract static class Options {
    public abstract boolean onlyApplicable();

    public abstract boolean onlyInvalid();

    public abstract boolean includePaths();

    public abstract boolean evaluationTime();

    public abstract boolean includeStatistics();

    static Builder builder() {
      return new AutoValue_TaskAttributeFactory_Options.Builder()
          .setOnlyApplicable(false)
          .setOnlyInvalid(false)
          .setIncludePaths(false)
          .setEvaluationTime(false)
          .setIncludeStatistics(false);
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setOnlyApplicable(boolean onlyApplicable);

      abstract Builder setOnlyInvalid(boolean onlyInvalid);

      abstract Builder setIncludePaths(boolean includePaths);

      abstract Builder setEvaluationTime(boolean evaluationTime);

      abstract Builder setIncludeStatistics(boolean includeStatistics);

      abstract Options build();
    }
  }

  public interface Factory {
    TaskAttributeFactory create(TaskTree.Node node, Statistics statistics, Options options);
  }

  @AssistedInject
  public TaskAttributeFactory(
      PermissionBackend permissionBackend,
      String pluginName,
      TaskPath.Factory taskPathFactory,
      TaskAttributeFactory.Factory factory,
      @Assisted TaskTree.Node node,
      @Nullable @Assisted Statistics statistics,
      @Assisted Options options) {
    this.hasViewPathsCapability =
        permissionBackend
            .currentUser()
            .testOrFalse(new PluginPermission(pluginName, ViewPathsCapability.VIEW_PATHS));
    this.taskPathFactory = taskPathFactory;
    this.factory = factory;
    this.statistics = statistics;
    this.options = options;
    this.node = node;
    this.task = node.task;
    attribute = new TaskAttribute(task.name());
    if (options.includeStatistics()) {
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

  public TaskAttributeFactory create(TaskTree.Node node) {
    return factory.create(node, statistics, options);
  }

  public Optional<TaskAttribute> createTaskAttribute() {
    try {
      if (options.evaluationTime()) {
        attribute.evaluationMilliSeconds = millis();
      }

      boolean applicable;
      try {
        applicable = node.match(task.applicable);
      } catch (QueryParseException e) {
        return Optional.of(invalid());
      }
      if (!task.isVisible) {
        if (!node.isTrusted() || (!applicable && !options.onlyApplicable())) {
          return Optional.of(unknown());
        }
      }

      if (applicable || !options.onlyApplicable()) {
        if (node.isChange()) {
          attribute.change = node.getChangeData().getId().get();
        }
        attribute.hasPass = !node.isDuplicate && (task.pass != null || task.fail != null);
        if (!node.isDuplicate) {
          attribute.subTasks = getSubTasks();
        }
        attribute.status = node.getStatus(attribute);
        if (options.onlyInvalid() && !isValidQueries()) {
          attribute.status = Status.INVALID;
        }
        if (options.includePaths()) {
          if (hasViewPathsCapability) {
            attribute.path = taskPathFactory.create(node.taskKey);
          } else {
            attribute.path = MISSING_VIEW_PATH_CAPABILITY;
          }
        }
        boolean groupApplicable = attribute.status != null;

        if (groupApplicable || !options.onlyApplicable()) {
          if (!options.onlyInvalid()
              || attribute.status == Status.INVALID
              || attribute.subTasks != null) {
            if (!options.onlyApplicable()) {
              attribute.applicable = applicable;
            }
            if (!node.isDuplicate) {
              if (task.inProgress != null) {
                attribute.inProgress = node.matchOrNull(task.inProgress);
              }
              attribute.exported = task.exported.isEmpty() ? null : task.exported;
            }
            attribute.hint = getHint(attribute.status, task);

            if (options.evaluationTime()) {
              attribute.evaluationMilliSeconds = millis() - attribute.evaluationMilliSeconds;
            }
            addStatistics(attribute.statistics);
            return Optional.of(attribute);
          }
        }
      }
    } catch (IOException | RuntimeException | ConfigInvalidException e) {
      return Optional.of(invalid()); // bad applicability query
    }
    return Optional.empty();
  }

  protected TaskAttribute invalid() {
    TaskAttribute invalid = invalid();
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

  protected List<TaskAttribute> getSubTasks()
      throws IOException, StorageException, ConfigInvalidException {
    List<TaskAttribute> subTasks = new ArrayList<>();
    List<TaskTree.Node> subNodes;
    if (options.onlyApplicable()) {
      subNodes = node.getApplicableSubNodes();
    } else {
      subNodes = node.getSubNodes();
    }
    for (TaskTree.Node subNode : subNodes) {
      if (subNode instanceof TaskTree.Node.Invalid) {
        subTasks.add(invalid());
      } else {
        create(subNode).createTaskAttribute().ifPresent(t -> subTasks.add(t));
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

  protected static long millis() {
    return System.nanoTime() / 1000000;
  }

  protected static String getHint(Status status, TaskConfig.Task def) {
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
}
