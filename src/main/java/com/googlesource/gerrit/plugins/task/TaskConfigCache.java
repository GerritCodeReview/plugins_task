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
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.config.AllProjectsNameProvider;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.permissions.RefPermission;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.task.cli.PatchSetArgument;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;

public class TaskConfigCache {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  protected final GitRepositoryManager gitMgr;
  protected final PermissionBackend permissionBackend;

  protected final CurrentUser user;
  protected final AllProjectsName allProjects;
  protected final String pluginName;
  protected final Config config;

  protected final Map<BranchNameKey, PatchSetArgument> psaMasquerades = new HashMap<>();
  protected final Map<FileKey, TaskConfig> taskCfgByFile = new HashMap<>();

  @Inject
  protected TaskConfigCache(
      AllProjectsNameProvider allProjectsNameProvider,
      GitRepositoryManager gitMgr,
      PermissionBackend permissionBackend,
      CurrentUser user,
      @PluginName String pluginName,
      @GerritServerConfig Config config) {
    this.allProjects = allProjectsNameProvider.get();
    this.gitMgr = gitMgr;
    this.permissionBackend = permissionBackend;
    this.user = user;
    this.pluginName = pluginName;
    this.config = config;
  }

  public TaskConfig getRootConfig() throws ConfigInvalidException, IOException {
    return getTaskConfig(FileKey.create(getRootBranch(), TaskFileConstants.TASK_CFG));
  }

  public void masquerade(PatchSetArgument psa) {
    psaMasquerades.put(psa.change.getDest(), psa);
  }

  protected BranchNameKey getRootBranch() {
    String cfgRootProject = config.getString(pluginName, "root", "project");
    String cfgRootBranch = config.getString(pluginName, "root", "branch");
    Project.NameKey rootProject =
        cfgRootProject != null ? Project.NameKey.parse(cfgRootProject) : allProjects;
    String rootBranch = cfgRootBranch != null ? cfgRootBranch : RefNames.REFS_CONFIG;
    return BranchNameKey.create(rootProject, rootBranch);
  }

  public TaskConfig getTaskConfig(FileKey key) throws ConfigInvalidException, IOException {
    TaskConfig cfg = taskCfgByFile.get(key);
    if (cfg == null) {
      cfg = loadTaskConfig(key);
      taskCfgByFile.put(key, cfg);
    }
    return cfg;
  }

  private TaskConfig loadTaskConfig(FileKey file) throws ConfigInvalidException, IOException {
    BranchNameKey branch = file.branch();
    PatchSetArgument psa = psaMasquerades.get(branch);
    boolean visible = true; // invisible psas are filtered out by commandline
    boolean isMasqueraded = false;
    if (psa == null) {
      visible = isVisible(branch);
    } else {
      isMasqueraded = true;
      branch = BranchNameKey.create(psa.change.getProject(), psa.patchSet.refName());
    }

    Project.NameKey project = file.branch().project();
    TaskConfig cfg =
        isMasqueraded
            ? new TaskConfig(branch, file, visible, isMasqueraded)
            : new TaskConfig(file, visible, isMasqueraded);
    try (Repository git = gitMgr.openRepository(project)) {
      cfg.load(project, git);
    } catch (IOException e) {
      log.atWarning().withCause(e).log("Failed to load %s for %s", file.file(), project);
      throw e;
    } catch (ConfigInvalidException e) {
      throw e;
    }
    return cfg;
  }

  public boolean isVisible(BranchNameKey branch) {
    try {
      PermissionBackend.ForProject permissions =
          permissionBackend.user(user).project(branch.project());
      permissions.ref(branch.branch()).check(RefPermission.READ);
      return true;
    } catch (AuthException | PermissionBackendException e) {
      return false;
    }
  }
}
