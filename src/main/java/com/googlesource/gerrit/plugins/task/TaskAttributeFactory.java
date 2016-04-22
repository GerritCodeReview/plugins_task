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
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskAttributeFactory implements ChangeAttributeFactory {
  private static final Logger log = LoggerFactory.getLogger(TaskAttributeFactory.class);

  public enum Status {
    INVALID,
    WAITING;
  }

  public static class TaskAttribute {
    public String name;
    public Status status;

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
      for (TaskDefinition def : getRootTaskDefinitions()) {
        if (match(c, def.applicable)) {
          TaskAttribute root = new TaskAttribute(def.name);
          root.status = Status.WAITING;
          a.roots.add(root);
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

  protected boolean match(ChangeData c, String query) throws OrmException, QueryParseException {
    if (query == null) {
      return true;
    }
    return ((Matchable) cqb.parse(query)).match(c);
  }
}
