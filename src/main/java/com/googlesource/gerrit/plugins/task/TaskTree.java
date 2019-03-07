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

import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.gerrit.server.AnonymousUser;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.config.AllUsersNameProvider;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.task.TaskConfig.External;
import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.errors.ConfigInvalidException;

/**
 * Add structure to access the task definitions from the config as a tree.
 *
 * <p>This class is a "middle" representation of the task tree. The task config is represented as a
 * lazily loaded tree, and much of the tree validity is enforced at this layer.
 */
public class TaskTree {
  protected static final String TASK_DIR = "task";
  protected static final Pattern OPTIONAL_TASK_PATTERN =
      Pattern.compile("([^ |]*( *[^ |])*) *\\| *");

  protected final AccountResolver accountResolver;
  protected final AllUsersNameProvider allUsers;
  protected final CurrentUser user;
  protected final TaskConfigFactory taskFactory;
  protected final Root root = new Root();

  @Inject
  public TaskTree(
      AccountResolver accountResolver,
      AllUsersNameProvider allUsers,
      AnonymousUser anonymousUser,
      CurrentUser user,
      TaskConfigFactory taskFactory) {
    this.accountResolver = accountResolver;
    this.allUsers = allUsers;
    this.user = user != null ? user : anonymousUser;
    this.taskFactory = taskFactory;
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

    protected void addSubDefinitions(List<Task> tasks, Map<String, String> parentProperties) {
      for (Task task : tasks) {
        if (task != null && !path.contains(task.name) && names.add(task.name)) {
          // path check above detects looping definitions
          // names check above detects duplicate subtasks
          try {
            nodes.add(new Node(task, path, parentProperties));
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
        addSubDefinitions(getRootTasks(), new HashMap<String, String>());
      }
      return nodes;
    }

    protected List<Task> getRootTasks() throws ConfigInvalidException, IOException {
      return taskFactory.getRootConfig().getRootTasks();
    }
  }

  public class Node extends NodeList {
    public final Task definition;

    public Node(Task definition, List<String> path, Map<String, String> parentProperties) {
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
      addSubDefinitions(getSubTasks());
      addSubFileDefinitions();
      addExternalDefinitions();
    }

    protected void addSubDefinitions(List<Task> tasks) {
      addSubDefinitions(tasks, definition.properties);
    }

    protected void addSubFileDefinitions() {
      for (String file : definition.subTasksFiles) {
        try {
          addSubDefinitions(getTasks(definition.config.getBranch(), file));
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
            addSubDefinitions(getTasks(ext));
          }
        } catch (ConfigInvalidException | IOException e) {
          nodes.add(null);
        }
      }
    }

    protected List<Task> getSubTasks() {
      List<Task> tasks = new ArrayList<>();
      for (String subTask : definition.subTasks) {
        addSubTaskTo(subTask, tasks);
      }
      return tasks;
    }

    protected void addSubTaskTo(String subTaskEntry, List<Task> tasks) {
      int end = 0;
      Matcher m = OPTIONAL_TASK_PATTERN.matcher(subTaskEntry);
      while (m.find()) {
        end = m.end();
        Task subTask = definition.config.getTask(m.group(1));
        if (subTask != null) {
          tasks.add(subTask);
          return;
        }
      }
      String last = subTaskEntry.substring(end);
      if (!"".equals(last)) { // Last entry was not optional
        tasks.add(definition.config.getTask(subTaskEntry.substring(end)));
      }
    }

    protected List<Task> getTasks(External external)
        throws ConfigInvalidException, IOException, OrmException {
      return getTasks(resolveUserBranch(external.user), external.file);
    }

    protected List<Task> getTasks(Branch.NameKey branch, String file)
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
