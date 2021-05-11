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
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.gerrit.server.AnonymousUser;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.config.AllUsersNameProvider;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.ChangeQueryProcessor;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.task.TaskConfig.External;
import com.googlesource.gerrit.plugins.task.TaskConfig.NamesFactory;
import com.googlesource.gerrit.plugins.task.TaskConfig.NamesFactoryType;
import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
import com.googlesource.gerrit.plugins.task.TaskConfig.TasksFactory;
import com.googlesource.gerrit.plugins.task.cli.PatchSetArgument;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jgit.errors.ConfigInvalidException;

/**
 * Add structure to access the task definitions from the config as a tree.
 *
 * <p>This class is a "middle" representation of the task tree. The task config is represented as a
 * lazily loaded tree, and much of the tree validity is enforced at this layer.
 */
public class TaskTree {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  protected static final String TASK_DIR = "task";

  protected final AccountResolver accountResolver;
  protected final AllUsersNameProvider allUsers;
  protected final CurrentUser user;
  protected final TaskConfigFactory taskFactory;
  protected final Root root = new Root();
  protected final Provider<ChangeQueryBuilder> changeQueryBuilderProvider;
  protected final Provider<ChangeQueryProcessor> changeQueryProcessorProvider;

  @Inject
  public TaskTree(
      AccountResolver accountResolver,
      AllUsersNameProvider allUsers,
      AnonymousUser anonymousUser,
      CurrentUser user,
      TaskConfigFactory taskFactory,
      Provider<ChangeQueryBuilder> changeQueryBuilderProvider,
      Provider<ChangeQueryProcessor> changeQueryProcessorProvider) {
    this.accountResolver = accountResolver;
    this.allUsers = allUsers;
    this.user = user != null ? user : anonymousUser;
    this.taskFactory = taskFactory;
    this.changeQueryProcessorProvider = changeQueryProcessorProvider;
    this.changeQueryBuilderProvider = changeQueryBuilderProvider;
  }

  public void masquerade(PatchSetArgument psa) {
    taskFactory.masquerade(psa);
  }

  public List<Node> getRootNodes() throws ConfigInvalidException, IOException {
    return root.getRootNodes();
  }

  protected class NodeList {
    protected LinkedList<String> path = new LinkedList<>();
    protected List<Node> nodes;
    protected Set<String> names = new HashSet<>();

    protected void addSubDefinitions(List<Task> defs, Map<String, String> parentProperties) {
      for (Task def : defs) {
        if (def != null && !path.contains(def.name) && names.add(def.name)) {
          // path check above detects looping definitions
          // names check above detects duplicate subtasks
          try {
            nodes.add(new Node(def, path, parentProperties));
            continue;
          } catch (Exception e) {
          } // bad definition, handled below
        }
        nodes.add(null);
      }
    }
  }

  protected class Root extends NodeList {
    public List<Node> getRootNodes() throws ConfigInvalidException, IOException {
      if (nodes == null) {
        nodes = new ArrayList<>();
        addSubDefinitions(getRootDefinitions(), new HashMap<String, String>());
      }
      return nodes;
    }

    protected List<Task> getRootDefinitions() throws ConfigInvalidException, IOException {
      return taskFactory.getRootConfig().getRootTasks();
    }
  }

  public class Node extends NodeList {
    public final Task definition;

    public Node(Task definition, List<String> path, Map<String, String> parentProperties)
        throws ConfigInvalidException {
      this.definition = definition;
      this.path.addAll(path);
      this.path.add(definition.name);
      Preloader.preload(definition);
      new Properties(definition, parentProperties);
    }

    public List<Node> getSubNodes() throws OrmException {
      if (nodes == null) {
        nodes = new ArrayList<>();
        addSubDefinitions();
      }
      return nodes;
    }

    protected void addSubDefinitions() throws OrmException {
      addSubDefinitions(getSubDefinitions());
      addSubDefinitions(getTasksFactoryDefinitions());
      addSubFileDefinitions();
      addExternalDefinitions();
    }

    protected void addSubDefinitions(List<Task> defs) {
      addSubDefinitions(defs, definition.properties);
    }

    protected void addSubFileDefinitions() {
      for (String file : definition.subTasksFiles) {
        try {
          addSubDefinitions(getTaskDefinitions(definition.config.getBranch(), file));
        } catch (ConfigInvalidException | IOException e) {
          nodes.add(null);
        }
      }
    }

    protected void addExternalDefinitions() throws OrmException {
      for (String external : definition.subTasksExternals) {
        try {
          External ext = definition.config.getExternal(external);
          if (ext == null) {
            nodes.add(null);
          } else {
            addSubDefinitions(getTaskDefinitions(ext));
          }
        } catch (ConfigInvalidException | IOException e) {
          nodes.add(null);
        }
      }
    }

    protected List<Task> getSubDefinitions() {
      List<Task> defs = new ArrayList<>();
      for (String name : definition.subTasks) {
        try {
          Task def = definition.config.getTaskOptional(name);
          if (def != null) {
            defs.add(def);
          }
        } catch (ConfigInvalidException e) {
          defs.add(null);
        }
      }
      return defs;
    }

    protected List<Task> getTasksFactoryDefinitions() {
      List<Task> taskList = new ArrayList<>();
      for (String taskFactoryName : definition.subTasksFactories) {
        TasksFactory tasksFactory = definition.config.getTasksFactory(taskFactoryName);
        if (tasksFactory != null) {
          NamesFactory namesFactory = definition.config.getNamesFactory(tasksFactory.namesFactory);
          if (namesFactory != null && namesFactory.type != null) {
            switch (NamesFactoryType.getNamesFactoryType(namesFactory.type)) {
              case STATIC:
                getStaticTypeTasksDefinitions(tasksFactory, namesFactory, taskList);
                continue;
              case CHANGE:
                getChangesTypeTaskDefinitions(tasksFactory, namesFactory, taskList);
                continue;
            }
          }
        }
        taskList.add(null);
      }
      return taskList;
    }

    protected void getStaticTypeTasksDefinitions(
        TasksFactory tasksFactory, NamesFactory namesFactory, List<Task> taskList) {
      for (String name : namesFactory.names) {
        taskList.add(definition.config.createTask(tasksFactory, name));
      }
    }

    protected void getChangesTypeTaskDefinitions(
        TasksFactory tasksFactory, NamesFactory namesFactory, List<Task> taskList) {
      try {
        if (namesFactory.changes != null) {
          List<ChangeData> changeDataList =
              changeQueryProcessorProvider
                  .get()
                  .query(changeQueryBuilderProvider.get().parse(namesFactory.changes)).entities();
          for (ChangeData changeData : changeDataList) {
            taskList.add(definition.config.createTask(tasksFactory, changeData.getId().toString()));
          }
          return;
        }
      } catch (OrmException e) {
        log.atSevere().withCause(e).log("ERROR: running changes query: " + namesFactory.changes);
      } catch (QueryParseException e) {
      }
      taskList.add(null);
    }

    protected List<Task> getTaskDefinitions(External external)
        throws ConfigInvalidException, IOException, OrmException {
      return getTaskDefinitions(resolveUserBranch(external.user), external.file);
    }

    protected List<Task> getTaskDefinitions(Branch.NameKey branch, String file)
        throws ConfigInvalidException, IOException {
      return taskFactory
          .getTaskConfig(branch, resolveTaskFileName(file), definition.isTrusted)
          .getTasks();
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
  }
}
