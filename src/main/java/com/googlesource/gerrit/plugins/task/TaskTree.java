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
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.AnonymousUser;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.config.AllUsersNameProvider;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.ChangeQueryProcessor;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.task.TaskConfig.External;
import com.googlesource.gerrit.plugins.task.TaskConfig.NamesFactory;
import com.googlesource.gerrit.plugins.task.TaskConfig.NamesFactoryType;
import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
import com.googlesource.gerrit.plugins.task.TaskConfig.TasksFactory;
import com.googlesource.gerrit.plugins.task.cli.PatchSetArgument;
import com.googlesource.gerrit.plugins.task.properties.Properties;
import com.googlesource.gerrit.plugins.task.statistics.HitHashMap;
import com.googlesource.gerrit.plugins.task.statistics.HitHashMapOfCollection;
import com.googlesource.gerrit.plugins.task.statistics.StatisticsMap;
import com.googlesource.gerrit.plugins.task.statistics.StopWatch;
import com.googlesource.gerrit.plugins.task.statistics.TracksStatistics;
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
import org.eclipse.jgit.util.StringUtils;

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

  public static class Statistics {
    public Object definitionsPerSubSectionCache;
    public Object definitionsByBranchBySubSectionCache;
    public Object changesByNamesFactoryQueryCache;
    public Properties.Statistics properties;
    public transient int summaryCount;
  }

  protected static final String TASK_DIR = "task";

  protected final AccountResolver accountResolver;
  protected final AllUsersNameProvider allUsers;
  protected final CurrentUser user;
  protected final PredicateCache predicateCache;
  protected final MatchCache matchCache;
  protected final Preloader preloader;
  protected final TaskConfigFactory taskConfigFactory;
  protected final TaskExpression.Factory taskExpressionFactory;
  protected final NodeList root = new NodeList();
  protected final Provider<ChangeQueryBuilder> changeQueryBuilderProvider;
  protected final Provider<ChangeQueryProcessor> changeQueryProcessorProvider;
  protected final StatisticsMap<String, List<ChangeData>> changesByNamesFactoryQuery =
      new HitHashMap<>();
  protected final StatisticsMap<SubSectionKey, List<Task>> definitionsBySubSection =
      new HitHashMapOfCollection<>();
  protected final StatisticsMap<SubSectionKey, Map<BranchNameKey, List<Task>>>
      definitionsByBranchBySubSection = new HitHashMap<>();

  protected ChangeData changeData;
  protected Statistics statistics;

  @Inject
  public TaskTree(
      AccountResolver accountResolver,
      AllUsersNameProvider allUsers,
      AnonymousUser anonymousUser,
      CurrentUser user,
      Provider<ChangeQueryBuilder> changeQueryBuilderProvider,
      Provider<ChangeQueryProcessor> changeQueryProcessorProvider,
      PredicateCache predicateCache,
      TaskConfigFactory taskConfigFactory,
      TaskExpression.Factory taskExpressionFactory) {
    this.accountResolver = accountResolver;
    this.allUsers = allUsers;
    this.user = user != null ? user : anonymousUser;
    this.changeQueryProcessorProvider = changeQueryProcessorProvider;
    this.changeQueryBuilderProvider = changeQueryBuilderProvider;
    this.predicateCache = predicateCache;
    this.matchCache = new MatchCache(predicateCache);
    this.taskConfigFactory = taskConfigFactory;
    this.taskExpressionFactory = taskExpressionFactory;
    this.preloader = new Preloader(taskConfigFactory, taskExpressionFactory);
  }

  public void masquerade(PatchSetArgument psa) {
    preloader.masquerade(psa);
  }

  public List<Node> getRootNodes(ChangeData changeData)
      throws ConfigInvalidException, IOException, StorageException {
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

    protected List<Node> getSubNodes()
        throws ConfigInvalidException, IOException, StorageException {
      if (cachedNodes != null) {
        return refresh(cachedNodes);
      }
      return cachedNodes = loadSubNodes();
    }

    protected List<Node> loadSubNodes()
        throws ConfigInvalidException, IOException, StorageException {
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

      public List<Node> createFromPreloaded(List<Task> defs) {
        List<Node> nodes = new ArrayList<>();
        for (Task def : defs) {
          nodes.add(createFromPreloaded(def));
        }
        return nodes;
      }

      public Node createFromPreloaded(Task def) {
        return createFromPreloaded(def, (parent, definition) -> new Node(parent, definition));
      }

      public Node createFromPreloaded(Task def, ChangeData changeData) {
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

      protected Node createFromPreloaded(Task def, NodeFactory nodeFactory) {
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
      public void refreshTask() {}

      @Override
      public Task getDefinition() {
        return null;
      }
    }

    public Task task;
    public boolean isDuplicate;

    protected Properties.Statistics propertiesStatistics;
    protected final Properties properties;
    protected final TaskKey taskKey;
    protected StatisticsMap<BranchNameKey, List<Node>> nodesByBranch;
    protected boolean hasUnfilterableSubNodes = false;

    protected Node() { // Only for Invalid
      taskKey = null;
      properties = null;
    }

    public Node(NodeList parent, Task task) {
      this.parent = parent;
      taskKey = task.key();
      properties = new Properties(this, task);
      refreshTask();
    }

    public String key() {
      return String.valueOf(getChangeData().getId().get()) + TaskConfig.SEP + taskKey;
    }

    public List<Node> getSubNodes() throws IOException, StorageException, ConfigInvalidException {
      if (cachedNodes != null) {
        return refresh(cachedNodes);
      }
      List<Node> nodes = loadSubNodes();
      if (!properties.isSubNodeReloadRequired()) {
        if (!isChange()) {
          return cachedNodes = nodes;
        }
        definitionsBySubSection.computeIfAbsentTimed(
            task.key().subSection(),
            k -> nodes.stream().map(n -> n.getDefinition()).collect(toList()),
            task.isVisible);
      } else {
        hasUnfilterableSubNodes = true;
        cachedNodeByTask.clear();
        nodes.stream()
            .filter(n -> !(n instanceof Invalid) && !n.isChange())
            .forEach(n -> cachedNodeByTask.put(n.task.key(), n));
      }
      return nodes;
    }

    public List<Node> getApplicableSubNodes()
        throws IOException, StorageException, ConfigInvalidException {
      return hasUnfilterableSubNodes ? getSubNodes() : new ApplicableNodeFilter().getSubNodes();
    }

    @Override
    protected List<Node> loadSubNodes()
        throws IOException, StorageException, ConfigInvalidException {
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
    public void refreshTask() {
      this.path = new LinkedList<>(parent.path);
      String key = key();
      isDuplicate = path.contains(key);
      path.add(key);

      if (statistics != null) {
        properties.setStatisticsConsumer(
            s -> statistics.properties = (propertiesStatistics = s).sum(statistics.properties));
      }
      this.task = properties.getTask(getChangeData());

      this.duplicateKeys = new LinkedList<>(parent.duplicateKeys);
      if (task.duplicateKey != null) {
        isDuplicate |= duplicateKeys.contains(task.duplicateKey);
        duplicateKeys.add(task.duplicateKey);
      }
    }

    public Properties getParentProperties() {
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

    public Task getDefinition() {
      return properties.isTaskRefreshRequired() ? properties.origTask : task;
    }

    public boolean isChange() {
      return false;
    }

    public boolean match(String query) throws StorageException, QueryParseException {
      return matchCache.match(getChangeData(), query, task.isVisible);
    }

    public Boolean matchOrNull(String query) {
      return matchCache.matchOrNull(getChangeData(), query, task.isVisible);
    }

    protected class SubNodeAdder {
      protected List<Node> nodes = new ArrayList<>();
      protected SubNodeFactory factory = new SubNodeFactory();

      public List<Node> getSubNodes() throws IOException, StorageException, ConfigInvalidException {
        addSubTasks();
        addSubTasksFactoryTasks();
        addSubTasksFiles();
        addSubTasksExternals();
        return nodes;
      }

      protected void addSubTasks() throws IOException, StorageException {
        for (TaskKey taskKey : task.subTasks) {
          try {
            Optional<Task> def =
                preloader.getOptionalTask(
                    taskExpressionFactory.create(taskKey.file(), taskKey.task()));
            if (def.isPresent()) {
              addPreloaded(def.get());
            }
          } catch (ConfigInvalidException e) {
            addInvalidNode();
          }
        }
      }

      protected void addSubTasksFiles() {
        for (String file : task.subTasksFiles) {
          try {
            addPreloaded(
                preloader.getTasks(FileKey.create(task.key().branch(), resolveTaskFileName(file))));
          } catch (ConfigInvalidException | IOException e) {
            addInvalidNode();
          }
        }
      }

      protected void addSubTasksExternals() throws StorageException {
        for (SubSectionKey key : task.subTasksExternals) {
          try {
            External ext = taskConfigFactory.getTaskConfig(key.file()).getExternal(key);
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
          throws IOException, StorageException, ConfigInvalidException {
        for (SubSectionKey key : task.subTasksFactories) {
          TasksFactory tasksFactory =
              taskConfigFactory.getTaskConfig(key.file()).getTasksFactory(key);
          if (tasksFactory != null) {
            NamesFactory namesFactory =
                taskConfigFactory
                    .getTaskConfig(key.file())
                    .getNamesFactory(tasksFactory.namesFactory);
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
          throws IOException, StorageException {
        for (String name : namesFactory.names) {
          if (StringUtils.isEmptyOrNull(name)) {
            addInvalidNode();
          } else {
            try {
              addPreloaded(preloader.preload(task.config.new Task(tasksFactory, name)));
            } catch (ConfigInvalidException e) {
              addInvalidNode();
            }
          }
        }
      }

      protected void addChangeTypeTasks(TasksFactory tasksFactory, NamesFactory namesFactory)
          throws IOException {
        try {
          if (namesFactory.changes != null) {
            for (ChangeData changeData : query(namesFactory.changes, task.isVisible)) {
              addPreloaded(
                  preloader.preload(
                      task.config.new Task(tasksFactory, changeData.getId().toString())),
                  changeData);
            }
            return;
          }
        } catch (StorageException e) {
          log.atSevere().withCause(e).log("ERROR: running changes query: %s", namesFactory.changes);
        } catch (QueryParseException | ConfigInvalidException e) {
        }
        addInvalidNode();
      }

      public void addPreloaded(List<Task> defs) {
        nodes.addAll(factory.createFromPreloaded(defs));
      }

      public void addPreloaded(Task def, ChangeData changeData) {
        nodes.add(factory.createFromPreloaded(def, changeData));
      }

      public void addPreloaded(Task def) {
        nodes.add(factory.createFromPreloaded(def));
      }

      public void addInvalidNode() {
        nodes.add(factory.createInvalid());
      }

      protected List<Task> getPreloadedTasks(External external)
          throws ConfigInvalidException, IOException, StorageException {
        return preloader.getTasks(
            FileKey.create(resolveUserBranch(external.user), resolveTaskFileName(external.file)));
      }
    }

    public class ApplicableNodeFilter {
      protected BranchNameKey branch = getChangeData().change().getDest();
      protected SubSectionKey subSection = task.key.subSection();
      protected Map<BranchNameKey, List<Task>> definitionsByBranch =
          definitionsByBranchBySubSection.get(subSection);

      public ApplicableNodeFilter() throws StorageException {}

      public List<Node> getSubNodes() throws IOException, StorageException, ConfigInvalidException {
        if (nodesByBranch != null) {
          List<Node> nodes = nodesByBranch.get(branch);
          if (nodes != null) {
            return refresh(nodes);
          }
        }
        if (definitionsByBranch != null) {
          List<Task> branchDefinitions = definitionsByBranch.get(branch);
          if (branchDefinitions != null) {
            return new SubNodeFactory().createFromPreloaded(branchDefinitions);
          }
        }
        List<Node> nodes = Node.this.getSubNodes();
        if (isChange()
            && definitionsByBranch == null
            && definitionsByBranchBySubSection.containsKey(subSection)) {
          hasUnfilterableSubNodes = true;
        }

        if (!hasUnfilterableSubNodes && !nodes.isEmpty()) {
          Optional<List<Node>> filterable = getOptionalApplicableForBranch(nodes);
          if (filterable.isPresent()) {
            if (!isChange()) {
              if (nodesByBranch == null) {
                nodesByBranch = initStatistics(new HitHashMapOfCollection<>());
              }
              nodesByBranch.put(branch, filterable.get());
            } else {
              if (definitionsByBranch == null) {
                definitionsByBranch = initStatistics(new HitHashMap<>());
                definitionsByBranchBySubSection.put(subSection, definitionsByBranch);
              }
              definitionsByBranch.put(
                  branch,
                  filterable.get().stream().map(node -> node.getDefinition()).collect(toList()));
            }
            return filterable.get();
          }
          hasUnfilterableSubNodes = true;
          if (isChange()) {
            definitionsByBranchBySubSection.put(subSection, null);
          }
        }
        return nodes;
      }

      protected Optional<List<Node>> getOptionalApplicableForBranch(List<Node> nodes)
          throws StorageException {
        int filterable = 0;
        List<Node> applicableNodes = new ArrayList<>();
        for (Node node : nodes) {
          if (node instanceof Invalid) {
            filterable++;
          } else if (isApplicableCacheableByBranch(node)) {
            filterable++;
            try {
              if (!node.match(node.task.applicable)) {
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
          return predicateCache.isCacheableByBranch(applicable, task.isVisible);
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

  protected BranchNameKey resolveUserBranch(String user)
      throws ConfigInvalidException, IOException, StorageException {
    if (user == null) {
      throw new ConfigInvalidException("External user not defined");
    }
    Account.Id acct;
    try {
      acct = accountResolver.resolve(user).asUnique().account().id();
    } catch (UnprocessableEntityException e) {
      throw new ConfigInvalidException("Cannot resolve user: " + user);
    }
    return BranchNameKey.create(allUsers.get(), RefNames.refsUsers(acct));
  }

  @SuppressWarnings("try")
  public List<ChangeData> query(String query, boolean isVisible)
      throws StorageException, QueryParseException {
    List<ChangeData> changeDataList = changesByNamesFactoryQuery.get(query);
    if (changeDataList == null) {
      try (StopWatch stopWatch =
          changesByNamesFactoryQuery.createLoadingStopWatch(query, isVisible)) {
        changeDataList =
            changeQueryProcessorProvider
                .get()
                .query(changeQueryBuilderProvider.get().parse(query))
                .entities();
      }
      changesByNamesFactoryQuery.put(query, changeDataList);
    }
    return changeDataList;
  }

  public void initStatistics(int summaryCount) {
    statistics = new Statistics();
    statistics.summaryCount = summaryCount;
    definitionsBySubSection.initStatistics(summaryCount);
    definitionsByBranchBySubSection.initStatistics(summaryCount);
    changesByNamesFactoryQuery.initStatistics(summaryCount);
  }

  protected <T extends TracksStatistics> T initStatistics(T tracker) {
    if (statistics != null) {
      tracker.initStatistics(statistics.summaryCount);
    }
    return tracker;
  }

  public Statistics getStatistics() {
    if (statistics != null) {
      statistics.definitionsPerSubSectionCache = definitionsBySubSection.getStatistics();
      statistics.definitionsByBranchBySubSectionCache =
          definitionsByBranchBySubSection.getStatistics();
      statistics.changesByNamesFactoryQueryCache = changesByNamesFactoryQuery.getStatistics();
    }
    return statistics;
  }

  protected static List<Node> refresh(List<Node> nodes) {
    for (Node node : nodes) {
      node.refreshTask();
    }
    return nodes;
  }
}
