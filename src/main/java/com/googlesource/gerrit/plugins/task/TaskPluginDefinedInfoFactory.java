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

import com.google.gerrit.entities.Change;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.common.PluginDefinedInfo;
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
import org.eclipse.jgit.errors.ConfigInvalidException;

public class TaskPluginDefinedInfoFactory implements ChangePluginDefinedInfoFactory {
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
  protected final TaskPath.Factory taskPathFactory;
  protected final TaskConfigCache taskConfigCache;
  protected Modules.MyOptions options;
  protected TaskPluginAttribute lastTaskPluginAttribute;
  protected Statistics statistics;
  protected final TaskAttributeFactory.Factory factory;

  @Inject
  public TaskPluginDefinedInfoFactory(
      String pluginName,
      TaskTree.Factory taskTreeFactory,
      PredicateCache predicateCache,
      TaskPath.Factory taskPathFactory,
      TaskConfigCache taskConfigCache,
      TaskAttributeFactory.Factory factory) {
    this.pluginName = pluginName;
    this.definitions = taskTreeFactory.create(taskConfigCache);
    this.predicateCache = predicateCache;
    this.taskPathFactory = taskPathFactory;
    this.taskConfigCache = taskConfigCache;
    this.factory = factory;
  }

  @Override
  public Map<Change.Id, PluginDefinedInfo> createPluginDefinedInfos(
      Collection<ChangeData> cds, BeanProvider beanProvider, String plugin) {
    Map<Change.Id, PluginDefinedInfo> pluginInfosByChange = new HashMap<>();
    options = (Modules.MyOptions) beanProvider.getDynamicBean(plugin);
    if (options.all || options.onlyApplicable || options.onlyInvalid) {
      initStatistics();
      for (PatchSetArgument psa : options.patchSetArguments) {
        taskConfigCache.masquerade(psa);
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
      for (Node root : definitions.getRootNodes(c)) {
        if (root instanceof Node.Invalid) {
          a.roots.add(invalid());
        } else {
          if (options.shouldFilterRoot(root.task.name())) {
            continue;
          }
          factory
              .create(
                  root,
                  statistics,
                  TaskAttributeFactory.Options.builder()
                      .setOnlyApplicable(options.onlyApplicable)
                      .setOnlyInvalid(options.onlyInvalid)
                      .setIncludePaths(options.includePaths)
                      .setEvaluationTime(options.evaluationTime)
                      .setIncludeStatistics(options.includeStatistics)
                      .build())
              .createTaskAttribute()
              .ifPresent(t -> a.roots.add(t));
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
}
