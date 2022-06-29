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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  protected final Preloader preloader;
  protected final NodeList root = new NodeList();
  protected final Provider<ChangeQueryBuilder> changeQueryBuilderProvider;
  protected final Provider<ChangeQueryProcessor> changeQueryProcessorProvider;

  protected ChangeData changeData;

  @Inject
  public TaskTree(
      AccountResolver accountResolver,
      AllUsersNameProvider allUsers,
      AnonymousUser anonymousUser,
      CurrentUser user,
      Provider<ChangeQueryBuilder> changeQueryBuilderProvider,
      Provider<ChangeQueryProcessor> changeQueryProcessorProvider,
      Preloader preloader) {
    this.accountResolver = accountResolver;
    this.allUsers = allUsers;
    this.user = user != null ? user : anonymousUser;
    this.changeQueryProcessorProvider = changeQueryProcessorProvider;
    this.changeQueryBuilderProvider = changeQueryBuilderProvider;
    this.preloader = preloader;
  }

  public void masquerade(PatchSetArgument psa) {
    preloader.masquerade(psa);
  }

  public List<Node> getRootNodes(ChangeData changeData)
      throws ConfigInvalidException, IOException, OrmException {
    this.changeData = changeData;
    root.path = Collections.emptyList();
    root.duplicateKeys = Collections.emptyList();
    return root.getSubNodes();
  }

  protected class NodeList {
    protected NodeList parent = null;
    protected Collection<String> path;
    protected Collection<String> duplicateKeys;
    protected Map<TaskKey, Node> cachedNodeByTask = new HashMap<>();
    protected List<Node> cachedNodes;

    public List<Node> getSubNodes() throws ConfigInvalidException, IOException, OrmException {
      if (cachedNodes == null) {
        return loadSubNodes();
      }
      refreshSubNodes();
      return cachedNodes;
    }

    protected List<Node> loadSubNodes() throws ConfigInvalidException, IOException, OrmException {
      return cachedNodes = new SubNodeAdder().getSubNodes();
    }

    public void refreshSubNodes() throws ConfigInvalidException, OrmException {
      if (cachedNodes != null) {
        for (Node node : cachedNodes) {
          if (node != null) {
            node.refreshTask();
          }
        }
      }
    }

    public ChangeData getChangeData() {
      return parent == null ? TaskTree.this.changeData : parent.getChangeData();
    }

    protected Properties getProperties() {
      return Properties.EMPTY_PARENT;
    }

    protected boolean isTrusted() {
      return true;
    }

    protected class SubNodeAdder {
      protected List<Node> nodes = new ArrayList<>();
      protected Set<String> names = new HashSet<>();

      public List<Node> getSubNodes() throws ConfigInvalidException, IOException, OrmException {
        addSubNodes();
        return nodes;
      }

      protected void addSubNodes() throws ConfigInvalidException, IOException, OrmException {
        addPreloaded(preloader.getRootTasks());
      }

      protected void addPreloaded(List<Task> defs) throws ConfigInvalidException, OrmException {
        for (Task def : defs) {
          addPreloaded(def);
        }
      }

      protected void addPreloaded(Task def) throws ConfigInvalidException, OrmException {
        addPreloaded(def, (parent, definition) -> new Node(parent, definition));
      }

      protected void addPreloaded(Task def, NodeFactory nodeFactory)
          throws ConfigInvalidException, OrmException {
        if (def != null) {
          try {
            Node node = cachedNodeByTask.get(def.key());
            boolean isRefreshNeeded = node != null;
            if (node == null) {
              node = nodeFactory.create(NodeList.this, def);
            }

            if (names.add(def.name())) {
              // names check above detects duplicate subtasks
              if (isRefreshNeeded) {
                node.refreshTask();
              }
              nodes.add(node);
              return;
            }
          } catch (Exception e) {
          }
        }
        addInvalidNode();
      }

      protected void addInvalidNode() {
        nodes.add(null); // null node indicates invalid
      }
    }
  }

  public class Node extends NodeList {
    public Task task;
    public boolean isDuplicate;

    protected final Properties properties;
    protected final TaskKey taskKey;

    public Node(NodeList parent, Task task) throws ConfigInvalidException, OrmException {
      this.parent = parent;
      taskKey = task.key();
      properties = new Properties(task, parent.getProperties());
      refreshTask();
    }

    public String key() {
      return String.valueOf(getChangeData().getId().get()) + TaskConfig.SEP + taskKey;
    }

    @Override
    protected List<Node> loadSubNodes() throws ConfigInvalidException, IOException, OrmException {
      cachedNodes = new SubNodeAdder().getSubNodes();
      properties.expansionComplete();
      return cachedNodes;
    }

    /* The task needs to be refreshed before a node is used, however
    subNode refreshing can wait until they are fetched since they may
    not be needed. */
    public void refreshTask() throws ConfigInvalidException, OrmException {
      this.path = new LinkedList<>(parent.path);
      String key = key();
      isDuplicate = path.contains(key);
      path.add(key);

      this.task = properties.getTask(getChangeData());

      this.duplicateKeys = new LinkedList<>(parent.duplicateKeys);
      if (task.duplicateKey != null) {
        isDuplicate |= duplicateKeys.contains(task.duplicateKey);
        duplicateKeys.add(task.duplicateKey);
      }

      if (cachedNodes != null && properties.isSubNodeReloadRequired()) {
        cachedNodeByTask.clear();
        cachedNodes.stream()
            .filter(n -> n != null && !n.isChange())
            .forEach(n -> cachedNodeByTask.put(n.task.key(), n));
        cachedNodes = null;
      }
    }

    @Override
    protected Properties getProperties() {
      return properties;
    }

    @Override
    protected boolean isTrusted() {
      return parent.isTrusted() && !task.isMasqueraded;
    }

    public boolean isChange() {
      return false;
    }

    protected class SubNodeAdder extends NodeList.SubNodeAdder {
      @Override
      protected void addSubNodes() throws ConfigInvalidException, IOException, OrmException {
        addSubTasks();
        addSubTasksFactoryTasks();
        addSubTasksFiles();
        addSubTasksExternals();
      }

      protected void addSubTasks() throws ConfigInvalidException, IOException, OrmException {
        for (String expression : task.subTasks) {
          try {
            Optional<Task> def =
                preloader.getOptionalTask(new TaskExpression(task.file(), expression));
            if (def.isPresent()) {
              addPreloaded(def.get());
            }
          } catch (ConfigInvalidException e) {
            addInvalidNode();
          }
        }
      }

      protected void addSubTasksFiles() throws ConfigInvalidException, OrmException {
        for (String file : task.subTasksFiles) {
          try {
            addPreloaded(
                preloader.getTasks(FileKey.create(task.key().branch(), resolveTaskFileName(file))));
          } catch (ConfigInvalidException | IOException e) {
            addInvalidNode();
          }
        }
      }

      protected void addSubTasksExternals() throws ConfigInvalidException, OrmException {
        for (String external : task.subTasksExternals) {
          try {
            External ext = task.config.getExternal(external);
            if (ext == null) {
              addInvalidNode();
            } else {
              addPreloaded(getPreloadedTasks(ext));
            }
          } catch (ConfigInvalidException | IOException e) {
            addInvalidNode();
          }
        }
      }

      protected void addSubTasksFactoryTasks()
          throws ConfigInvalidException, IOException, OrmException {
        for (String tasksFactoryName : task.subTasksFactories) {
          TasksFactory tasksFactory = task.config.getTasksFactory(tasksFactoryName);
          if (tasksFactory != null) {
            NamesFactory namesFactory = task.config.getNamesFactory(tasksFactory.namesFactory);
            if (namesFactory != null && namesFactory.type != null) {
              namesFactory = getProperties().getNamesFactory(namesFactory);
              switch (NamesFactoryType.getNamesFactoryType(namesFactory.type)) {
                case STATIC:
                  addStaticTypeTasks(tasksFactory, namesFactory);
                  continue;
                case CHANGE:
                  addChangeTypeTasks(tasksFactory, namesFactory);
                  continue;
              }
            }
          }
          addInvalidNode();
        }
      }

      protected void addStaticTypeTasks(TasksFactory tasksFactory, NamesFactory namesFactory)
          throws ConfigInvalidException, IOException, OrmException {
        for (String name : namesFactory.names) {
          addPreloaded(preloader.preload(task.config.new Task(tasksFactory, name)));
        }
      }

      protected void addChangeTypeTasks(TasksFactory tasksFactory, NamesFactory namesFactory)
          throws ConfigInvalidException, IOException, OrmException {
        try {
          if (namesFactory.changes != null) {
            List<ChangeData> changeDataList =
                changeQueryProcessorProvider
                    .get()
                    .query(changeQueryBuilderProvider.get().parse(namesFactory.changes))
                    .entities();
            for (ChangeData changeData : changeDataList) {
              addPreloaded(
                  preloader.preload(
                      task.config.new Task(tasksFactory, changeData.getId().toString())),
                  (parent, definition) ->
                      new Node(parent, definition) {
                        @Override
                        public ChangeData getChangeData() {
                          return changeData;
                        }

                        @Override
                        public boolean isChange() {
                          return true;
                        }
                      });
            }
            return;
          }
        } catch (OrmException e) {
          log.atSevere().withCause(e).log("ERROR: running changes query: " + namesFactory.changes);
        } catch (QueryParseException e) {
        }
        addInvalidNode();
      }

      protected List<Task> getPreloadedTasks(External external)
          throws ConfigInvalidException, IOException, OrmException {
        return preloader.getTasks(
            FileKey.create(resolveUserBranch(external.user), resolveTaskFileName(external.file)));
      }
    }
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
