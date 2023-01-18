// Copyright (C) 2022 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.task.properties;

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.server.query.change.ChangeData;
import com.googlesource.gerrit.plugins.task.StopWatch;
import com.googlesource.gerrit.plugins.task.TaskConfig;
import com.googlesource.gerrit.plugins.task.TaskConfig.NamesFactory;
import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
import com.googlesource.gerrit.plugins.task.TaskTree;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/** Use to expand properties like ${_name} in the text of various definitions. */
public class Properties {
  public static class Statistics {
    public long getTaskNanoseconds;
    public long copierNanoseconds;
    public Matcher.Statistics matcher;

    public static void setNanoseconds(Statistics stats, long nanos) {
      if (stats != null) {
        stats.getTaskNanoseconds = nanos;
      }
    }

    public Statistics sum(Statistics other) {
      if (other == null) {
        return this;
      }
      Statistics statistics = new Statistics();
      statistics.getTaskNanoseconds = getTaskNanoseconds + other.getTaskNanoseconds;
      statistics.copierNanoseconds = copierNanoseconds + other.copierNanoseconds;
      statistics.matcher = matcher == null ? other.matcher : matcher.sum(other.matcher);
      return statistics;
    }
  }

  public static final Properties EMPTY =
      new Properties() {
        @Override
        protected Function<String, String> getParentMapper() {
          return n -> "";
        }
      };

  public final Task origTask;
  protected final TaskTree.Node node;
  protected final CopyOnWrite<Task> task;
  protected Statistics statistics;
  protected Consumer<Statistics> statisticsConsumer;
  protected Consumer<Matcher.Statistics> matcherStatisticsConsumer;
  protected Expander expander;
  protected Loader loader;
  protected boolean init = true;
  protected boolean isTaskRefreshRequired;
  protected boolean isApplicableRefreshRequired;
  protected boolean isSubNodeReloadRequired;

  public Properties() {
    this(null, null);
    expander = new Expander(n -> "");
  }

  public Properties(TaskTree.Node node, Task origTask) {
    this.node = node;
    this.origTask = origTask;
    task = new CopyOnWrite.CloneOnWrite<>(origTask);
  }

  /** Use to expand properties specifically for Tasks. */
  @SuppressWarnings("try")
  public Task getTask(ChangeData changeData) throws StorageException {
    try (StopWatch stopWatch =
        StopWatch.createEnabled(statistics != null)
            .setNanosConsumer(l -> Statistics.setNanoseconds(statistics, l))) {
      loader = new Loader(origTask, changeData, getParentMapper());
      expander = new Expander(n -> loader.load(n));
      expander.setStatisticsConsumer(matcherStatisticsConsumer);
      if (isTaskRefreshRequired || init) {
        expander.expand(task, TaskConfig.KEY_APPLICABLE);
        isApplicableRefreshRequired = loader.isNonTaskDefinedPropertyLoaded();

        expander.expand(task, ImmutableSet.of(TaskConfig.KEY_APPLICABLE, TaskConfig.KEY_NAME));

        Map<String, String> exported = expander.expand(origTask.exported);
        if (exported != origTask.exported) {
          task.getForWrite().exported = exported;
        }

        if (init) {
          init = false;
          isTaskRefreshRequired = loader.isNonTaskDefinedPropertyLoaded();
        }
      }
    }
    if (statisticsConsumer != null) {
      statisticsConsumer.accept(statistics);
    }
    return task.getForRead();
  }

  public void setStatisticsConsumer(Consumer<Statistics> statisticsConsumer) {
    if (statisticsConsumer != null) {
      this.statisticsConsumer = statisticsConsumer;
      statistics = new Statistics();
      matcherStatisticsConsumer = s -> statistics.matcher = s;
      task.setNanosecondsConsumer(ns -> statistics.copierNanoseconds = ns);
    }
  }

  // To detect NamesFactories dependent on non task defined properties, the checking must be
  // done after subnodes are fully loaded, which unfortunately happens after getTask() is
  // called, therefore this must be called after all subnodes have been loaded.
  public void expansionComplete() {
    isSubNodeReloadRequired = loader.isNonTaskDefinedPropertyLoaded();
  }

  public boolean isApplicableRefreshRequired() {
    return isApplicableRefreshRequired;
  }

  public boolean isTaskRefreshRequired() {
    return isTaskRefreshRequired;
  }

  public boolean isSubNodeReloadRequired() {
    return isSubNodeReloadRequired;
  }

  /** Use to expand properties specifically for NamesFactories. */
  public NamesFactory getNamesFactory(NamesFactory namesFactory) {
    return expander.expand(namesFactory, ImmutableSet.of(TaskConfig.KEY_TYPE));
  }

  protected Function<String, String> getParentMapper() {
    return n -> node.getParentProperties().expander.getValueForName(n);
  }
}
