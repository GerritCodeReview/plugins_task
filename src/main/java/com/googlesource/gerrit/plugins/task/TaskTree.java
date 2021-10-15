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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

  @FunctionalInterface
  public interface NodeFactory {
    Node create(NodeList parent, Task definition) throws Exception;
  }

  protected static final String TASK_DIR = "task";

  protected final AccountResolver accountResolver;
  protected final AllUsersNameProvider allUsers;
  protected final CurrentUser user;
  protected final TaskConfigFactory taskFactory;
  protected final Root root = new Root();
  protected final Provider<ChangeQueryBuilder> changeQueryBuilderProvider;
  protected final Provider<ChangeQueryProcessor> changeQueryProcessorProvider;

  protected ChangeData changeData;

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

  public List<Node> getRootNodes(ChangeData changeData) throws ConfigInvalidException, IOException {
    this.changeData = changeData;
    return root.getRootNodes();
  }

  protected class NodeList {
    protected NodeList parent = null;
    protected LinkedList<String> path = new LinkedList<>();
    protected List<Node> nodes;
    protected Set<String> names = new HashSet<>();

    protected void addSubDefinitions(List<Task> defs) {
      for (Task def : defs) {
        addSubDefinition(def);
      }
    }

    protected void addSubDefinition(Task def) {
      addSubDefinition(def, (parent, definition) -> new Node(parent, definition));
    }

    protected void addSubDefinition(Task def, NodeFactory nodeFactory) {
      Node node = null;
      if (def != null && !path.contains(def.name) && names.add(def.name)) {
        // path check above detects looping definitions
        // names check above detects duplicate subtasks
        try {
          node = nodeFactory.create(this, def);
        } catch (Exception e) {
          // null node indicates invalid
        }
      }
      nodes.add(node);
    }

    public ChangeData getChangeData() {
      return parent == null ? TaskTree.this.changeData : parent.getChangeData();
    }

    protected Properties.Task getProperties() {
      return Properties.Task.EMPTY_PARENT;
    }
  }

  protected class Root extends NodeList {
    public List<Node> getRootNodes() throws ConfigInvalidException, IOException {
      if (nodes == null) {
        nodes = new ArrayList<>();
        addSubDefinitions(getRootDefinitions());
      }
      return nodes;
    }

    protected List<Task> getRootDefinitions() throws ConfigInvalidException, IOException {
      return taskFactory.getRootConfig().getRootTasks();
    }
  }

  public class Node extends NodeList {
    public final Task task;
    protected final Properties.Task properties;

    public Node(NodeList parent, Task definition) throws ConfigInvalidException, OrmException {
      this.parent = parent;
      this.task = definition;
      this.path.addAll(parent.path);
      this.path.add(definition.name);
      Preloader.preload(definition);
      properties = new Properties.Task(getChangeData(), definition, parent.getProperties());
    }

    public List<Node> getSubNodes() throws OrmException {
      if (nodes == null) {
        nodes = new ArrayList<>();
        addSubDefinitions();
      }
      return nodes;
    }

    protected void addSubDefinitions() throws OrmException {
      addSubTaskDefinitions();
      addSubTasksFactoryDefinitions();
      addSubFileDefinitions();
      addExternalDefinitions();
    }

    protected void addSubTaskDefinitions() {
      for (String name : task.subTasks) {
        try {
          Task def = task.config.getTaskOptional(name);
          if (def != null) {
            addSubDefinition(def);
          }
        } catch (ConfigInvalidException e) {
          addSubDefinition(null);
        }
      }
    }

    protected void addSubFileDefinitions() {
      for (String file : task.subTasksFiles) {
        try {
          addSubDefinitions(getTaskDefinitions(task.config.getBranch(), file));
        } catch (ConfigInvalidException | IOException e) {
          addSubDefinition(null);
        }
      }
    }

    protected void addExternalDefinitions() throws OrmException {
      for (String external : task.subTasksExternals) {
        try {
          External ext = task.config.getExternal(external);
          if (ext == null) {
            addSubDefinition(null);
          } else {
            addSubDefinitions(getTaskDefinitions(ext));
          }
        } catch (ConfigInvalidException | IOException e) {
          addSubDefinition(null);
        }
      }
    }

    protected void addSubTasksFactoryDefinitions() throws OrmException {
      for (String taskFactoryName : task.subTasksFactories) {
        TasksFactory tasksFactory = task.config.getTasksFactory(taskFactoryName);
        if (tasksFactory != null) {
          NamesFactory namesFactory = task.config.getNamesFactory(tasksFactory.namesFactory);
          if (namesFactory != null && namesFactory.type != null) {
            new Properties.NamesFactory(namesFactory, getProperties());
            switch (NamesFactoryType.getNamesFactoryType(namesFactory.type)) {
              case STATIC:
                addStaticTypeTaskDefinitions(tasksFactory, namesFactory);
                continue;
              case CHANGE:
                addChangesTypeTaskDefinitions(tasksFactory, namesFactory);
                continue;
            }
          }
        }
        addSubDefinition(null);
      }
    }

    protected void addStaticTypeTaskDefinitions(
        TasksFactory tasksFactory, NamesFactory namesFactory) {
      for (String name : namesFactory.names) {
        addSubDefinition(task.config.createTask(tasksFactory, name));
      }
    }

    protected void addChangesTypeTaskDefinitions(
        TasksFactory tasksFactory, NamesFactory namesFactory) {
      try {
        if (namesFactory.changes != null) {
          List<ChangeData> changeDataList =
              changeQueryProcessorProvider
                  .get()
                  .query(changeQueryBuilderProvider.get().parse(namesFactory.changes))
                  .entities();
          for (ChangeData changeData : changeDataList) {
            addSubDefinition(
                task.config.createTask(tasksFactory, changeData.getId().toString()),
                (parent, definition) ->
                    new ChangeNodeFactory(changeData).new ChangeNode(parent, definition));
          }
          return;
        }
      } catch (OrmException e) {
        log.atSevere().withCause(e).log("ERROR: running changes query: " + namesFactory.changes);
      } catch (QueryParseException e) {
      }
      addSubDefinition(null);
    }

    protected List<Task> getTaskDefinitions(External external)
        throws ConfigInvalidException, IOException, OrmException {
      return getTaskDefinitions(resolveUserBranch(external.user), external.file);
    }

    protected List<Task> getTaskDefinitions(Branch.NameKey branch, String file)
        throws ConfigInvalidException, IOException {
      return taskFactory
          .getTaskConfig(branch, resolveTaskFileName(file), task.isTrusted)
          .getTasks();
    }

    @Override
    protected Properties.Task getProperties() {
      return properties;
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

  public class ChangeNodeFactory {
    public class ChangeNode extends Node {
      public ChangeNode(NodeList parent, Task definition)
          throws ConfigInvalidException, OrmException {
        super(parent, definition);
      }

      public ChangeData getChangeData() {
        return ChangeNodeFactory.this.changeData;
      }
    }

    protected ChangeData changeData;

    public ChangeNodeFactory(ChangeData changeData) {
      this.changeData = changeData;
    }
  }
}
