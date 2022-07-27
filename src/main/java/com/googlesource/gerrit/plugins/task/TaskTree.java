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

import static java.util.stream.Collectors.toList;

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
  protected final Map<SubSectionKey, List<Task>> definitionsBySubSection = new HashMap<>();

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
      if (cachedNodes != null) {
        return refresh(cachedNodes);
      }
      return cachedNodes = loadSubNodes();
    }

    protected List<Node> loadSubNodes() throws ConfigInvalidException, IOException, OrmException {
      return new SubNodeFactory().createFromPreloaded(preloader.getRootTasks());
    }

    public ChangeData getChangeData() {
      return TaskTree.this.changeData;
    }

    protected boolean isTrusted() {
      return true;
    }

    protected class SubNodeFactory {
      protected Set<String> names = new HashSet<>();

      public List<Node> createFromPreloaded(List<Task> defs)
          throws ConfigInvalidException, OrmException {
        List<Node> nodes = new ArrayList<>();
        for (Task def : defs) {
          nodes.add(createFromPreloaded(def));
        }
        return nodes;
      }

      public Node createFromPreloaded(Task def) throws ConfigInvalidException, OrmException {
        return createFromPreloaded(def, (parent, definition) -> new Node(parent, definition));
      }

      public Node createFromPreloaded(Task def, ChangeData changeData)
          throws ConfigInvalidException, OrmException {
        return createFromPreloaded(
            def,
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

      protected Node createFromPreloaded(Task def, NodeFactory nodeFactory)
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
              return node;
            }
          } catch (Exception e) {
          }
        }
        return createInvalid();
      }

      protected Node createInvalid() {
        return new Node().new Invalid();
      }
    }
  }

  public class Node extends NodeList {
    public class Invalid extends Node {
      @Override
      public void refreshTask() throws ConfigInvalidException, OrmException {}
    }

    public Task task;
    public boolean isDuplicate;

    protected final Properties properties;
    protected final TaskKey taskKey;
    protected Map<Branch.NameKey, List<Node>> nodesByBranch;
    protected boolean hasUnfilterableSubNodes = false;

    protected Node() { // Only for Invalid
      taskKey = null;
      properties = null;
    }

    public Node(NodeList parent, Task task) throws ConfigInvalidException, OrmException {
      this.parent = parent;
      taskKey = task.key();
      properties = new Properties(this, task);
      refreshTask();
    }

    public String key() {
      return String.valueOf(getChangeData().getId().get()) + TaskConfig.SEP + taskKey;
    }

    public List<Node> getSubNodes() throws ConfigInvalidException, IOException, OrmException {
      if (cachedNodes != null) {
        return refresh(cachedNodes);
      }
      List<Node> nodes = loadSubNodes();
      if (!properties.isSubNodeReloadRequired()) {
        if (!isChange()) {
          return cachedNodes = nodes;
        }
        definitionsBySubSection.computeIfAbsent(
            task.key().subSection(), k -> nodes.stream().map(n -> n.task).collect(toList()));
      } else {
        hasUnfilterableSubNodes = true;
        cachedNodeByTask.clear();
        nodes.stream()
            .filter(n -> !(n instanceof Invalid) && !n.isChange())
            .forEach(n -> cachedNodeByTask.put(n.task.key(), n));
      }
      return nodes;
    }

    public List<Node> getSubNodes(MatchCache matchCache)
        throws ConfigInvalidException, IOException, OrmException {
      if (hasUnfilterableSubNodes) {
        return getSubNodes();
      }
      return new ApplicableNodeFilter(matchCache).getSubNodes();
    }

    @Override
    protected List<Node> loadSubNodes() throws ConfigInvalidException, IOException, OrmException {
      List<Task> cachedDefinitions = definitionsBySubSection.get(task.key().subSection());
      if (cachedDefinitions != null) {
        return new SubNodeFactory().createFromPreloaded(cachedDefinitions);
      }
      List<Node> nodes = new SubNodeAdder().getSubNodes();
      properties.expansionComplete();
      return nodes;
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
    }

    protected Properties getParentProperties() {
      return (parent instanceof Node) ? ((Node) parent).properties : Properties.EMPTY;
    }

    @Override
    protected boolean isTrusted() {
      return parent.isTrusted() && !task.isMasqueraded;
    }

    @Override
    public ChangeData getChangeData() {
      return parent.getChangeData();
    }

    public boolean isChange() {
      return false;
    }

    protected class SubNodeAdder {
      protected List<Node> nodes = new ArrayList<>();
      protected SubNodeFactory factory = new SubNodeFactory();

      public List<Node> getSubNodes() throws ConfigInvalidException, IOException, OrmException {
        addSubTasks();
        addSubTasksFactoryTasks();
        addSubTasksFiles();
        addSubTasksExternals();
        return nodes;
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
              namesFactory = properties.getNamesFactory(namesFactory);
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
                  changeData);
            }
            return;
          }
        } catch (OrmException e) {
          log.atSevere().withCause(e).log("ERROR: running changes query: " + namesFactory.changes);
        } catch (QueryParseException e) {
        }
        addInvalidNode();
      }

      public void addPreloaded(List<Task> defs) throws ConfigInvalidException, OrmException {
        nodes.addAll(factory.createFromPreloaded(defs));
      }

      public void addPreloaded(Task def, ChangeData changeData)
          throws ConfigInvalidException, OrmException {
        nodes.add(factory.createFromPreloaded(def, changeData));
      }

      public void addPreloaded(Task def) throws ConfigInvalidException, OrmException {
        nodes.add(factory.createFromPreloaded(def));
      }

      public void addInvalidNode() {
        nodes.add(factory.createInvalid());
      }

      protected List<Task> getPreloadedTasks(External external)
          throws ConfigInvalidException, IOException, OrmException {
        return preloader.getTasks(
            FileKey.create(resolveUserBranch(external.user), resolveTaskFileName(external.file)));
      }
    }

    public class ApplicableNodeFilter {
      protected MatchCache matchCache;
      protected PredicateCache pcache;
      protected Branch.NameKey branch = getChangeData().change().getDest();

      public ApplicableNodeFilter(MatchCache matchCache)
          throws ConfigInvalidException, IOException, OrmException {
        this.matchCache = matchCache;
        this.pcache = matchCache.predicateCache;
      }

      public List<Node> getSubNodes() throws ConfigInvalidException, IOException, OrmException {
        if (nodesByBranch != null) {
          List<Node> nodes = nodesByBranch.get(branch);
          if (nodes != null) {
            return refresh(nodes);
          }
        }

        List<Node> nodes = Node.this.getSubNodes();
        if (!hasUnfilterableSubNodes && !nodes.isEmpty()) {
          Optional<List<Node>> filterable = getOptionalApplicableForBranch(nodes);
          if (filterable.isPresent()) {
            if (nodesByBranch == null) {
              nodesByBranch = new HashMap<>();
            }
            nodesByBranch.put(branch, filterable.get());
            return filterable.get();
          }
          hasUnfilterableSubNodes = true;
        }
        return nodes;
      }

      protected Optional<List<Node>> getOptionalApplicableForBranch(List<Node> nodes)
          throws ConfigInvalidException, IOException, OrmException {
        int filterable = 0;
        List<Node> applicableNodes = new ArrayList<>();
        for (Node node : nodes) {
          if (node instanceof Invalid) {
            filterable++;
          } else if (isApplicableCacheableByBranch(node)) {
            filterable++;
            try {
              if (!matchCache.match(node.task.applicable)) {
                // Correctness will not be affected if more nodes are added than necessary
                // (i.e. if isApplicableCacheableByBranch() does not realize a Node is cacheable
                // based on its Branch), but it is incorrect to filter out a Node now that could
                // later be applicable when a property, other than its Change's destination, is
                // altered.
                continue;
              }
            } catch (QueryParseException e) {
            }
          }
          applicableNodes.add(node);
        }
        // Simple heuristic to determine whether storing the filtered nodes is worth it. There
        // is minor evidence to suggest that storing a large list actually hurts performance.
        return (filterable > nodes.size() / 2) ? Optional.of(applicableNodes) : Optional.empty();
      }

      protected boolean isApplicableCacheableByBranch(Node node) {
        String applicable = node.task.applicable;
        if (node.properties.isApplicableRefreshRequired()) {
          return false;
        }
        try {
          return pcache.isCacheableByBranch(applicable);
        } catch (QueryParseException e) {
          return false;
        }
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

  protected static List<Node> refresh(List<Node> nodes)
      throws ConfigInvalidException, OrmException {
    for (Node node : nodes) {
      node.refreshTask();
    }
    return nodes;
  }
}
