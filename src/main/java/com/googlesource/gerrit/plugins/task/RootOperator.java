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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.index.query.PostFilterPredicate;
import com.google.gerrit.index.query.Predicate;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;

public class RootOperator implements ChangeQueryBuilder.ChangeOperatorFactory {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public static final String ROOT = "root";
  public static final String KEY_STATUS = "status";
  public static final String KEY_HAS_SUB_TASKS = "hasSubTasks";

  public enum HasSubTasks {
    TRUE,
    FALSE;
  }

  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      bind(ChangeQueryBuilder.ChangeOperatorFactory.class)
          .annotatedWith(Exports.named(ROOT))
          .to(RootOperator.class);
    }
  }

  public class RootPredicate extends PostFilterPredicate<ChangeData> {
    protected String root;
    protected TaskPluginDefinedInfoFactory.Status status;
    protected HasSubTasks hasSubTasks;

    public RootPredicate(
        String value,
        String root,
        TaskPluginDefinedInfoFactory.Status status,
        HasSubTasks hasSubTasks) {
      super(ROOT, value);
      this.root = root;
      this.status = status;
      this.hasSubTasks = hasSubTasks;
    }

    @Override
    public boolean match(ChangeData cd) {
      try {
        for (TaskTree.Node rootNode :
            taskTreeFactoryProvider.get().create(taskConfigCache).getRootNodes(cd)) {
          if (rootNode.task.name().equals(root)) {
            TaskAttributeFactory.Options options =
                TaskAttributeFactory.Options.builder().setOnlyApplicable(true).build();
            Optional<TaskPluginDefinedInfoFactory.TaskAttribute> attribute =
                factory.create(rootNode, null, options).createTaskAttribute();
            if (attribute.isPresent()) {
              if (status != null && !attribute.get().status.equals(status)) {
                return false;
              }
              if (hasSubTasks != null
                  && (hasSubTasks.equals(HasSubTasks.TRUE) && attribute.get().subTasks.isEmpty()
                      || hasSubTasks.equals(HasSubTasks.FALSE)
                          && !attribute.get().subTasks.isEmpty())) {
                return false;
              }
              return true;
            }
            break;
          }
        }
        return false;
      } catch (ConfigInvalidException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getCost() {
      return 10;
    }
  }

  private final Provider<TaskTree.Factory> taskTreeFactoryProvider;
  private final TaskConfigCache taskConfigCache;
  protected final TaskAttributeFactory.Factory factory;

  @Inject
  RootOperator(
      Provider<TaskTree.Factory> taskTreeFactoryProvider,
      TaskConfigCache taskConfigCache,
      TaskAttributeFactory.Factory factory) {
    this.taskTreeFactoryProvider = taskTreeFactoryProvider;
    this.taskConfigCache = taskConfigCache;
    this.factory = factory;
  }

  @Override
  public Predicate<ChangeData> create(ChangeQueryBuilder builder, String value)
      throws QueryParseException {
    PredicateArgs args = new PredicateArgs(value);
    String root;
    TaskPluginDefinedInfoFactory.Status status = null;
    HasSubTasks hasSubTasks = null;

    if (args.positional.size() != 1) {
      throw new IllegalArgumentException("Exactly one root task name must be provided");
    }
    root = args.positional.get(0);

    for (Map.Entry<String, String> pair : args.keyValue.entrySet()) {
      if (pair.getKey().equals(KEY_STATUS)) {
        status = TaskPluginDefinedInfoFactory.Status.valueOf(pair.getValue().toUpperCase());
        continue;
      }
      if (pair.getKey().equals(KEY_HAS_SUB_TASKS)) {
        hasSubTasks = HasSubTasks.valueOf(pair.getValue().toUpperCase());
        continue;
      }
      throw new IllegalArgumentException(
          String.format("Unrecognized key-value arg %s:%s", pair.getKey(), pair.getValue()));
    }
    return new RootOperator.RootPredicate(value, root, status, hasSubTasks);
  }
}
