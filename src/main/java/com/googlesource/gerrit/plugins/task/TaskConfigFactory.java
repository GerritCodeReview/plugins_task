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

import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.AllProjectsName;
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
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskConfigFactory {
  private static final Logger log = LoggerFactory.getLogger(TaskConfigFactory.class);

  protected static final String EXTENSION = ".config";
  protected static final String DEFAULT = "task" + EXTENSION;

  protected final GitRepositoryManager gitMgr;
  protected final PermissionBackend permissionBackend;

  protected final CurrentUser user;
  protected final AllProjectsName allProjects;

  protected final Map<Branch.NameKey, PatchSetArgument> psaMasquerades = new HashMap<>();

  @Inject
  protected TaskConfigFactory(
      GitRepositoryManager gitMgr, PermissionBackend permissionBackend, CurrentUser user) {
    // Injecting AllProjectsName doesn't work here
    this.allProjects = new AllProjectsName("All-Projects");
    this.gitMgr = gitMgr;
    this.permissionBackend = permissionBackend;
    this.user = user;
  }

  public TaskConfig getRootConfig() throws ConfigInvalidException, IOException {
    return getTaskConfig(getRootBranch(), DEFAULT, true);
  }

  public void masquerade(PatchSetArgument psa) {
    psaMasquerades.put(psa.change.getDest(), psa);
  }

  protected Branch.NameKey getRootBranch() {
    return new Branch.NameKey(allProjects, "refs/meta/config");
  }

  public TaskConfig getTaskConfig(Branch.NameKey branch, String fileName, boolean isTrusted)
      throws ConfigInvalidException, IOException {
    PatchSetArgument psa = psaMasquerades.get(branch);
    boolean visible = true; // invisible psas are filtered out by commandline
    if (psa == null) {
      visible = canRead(branch);
    } else {
      isTrusted = false;
      branch = new Branch.NameKey(psa.change.getProject(), psa.patchSet.getId().toRefName());
    }

    Project.NameKey project = branch.getParentKey();
    TaskConfig cfg = new TaskConfig(branch, fileName, visible, isTrusted);
    try {
      Repository git = gitMgr.openRepository(project);
      try {
        cfg.load(project, git);
      } finally {
        git.close();
      }
    } catch (IOException e) {
      log.warn("Failed to load " + fileName + " for " + project.get(), e);
      throw e;
    } catch (ConfigInvalidException e) {
      throw e;
    }
    return cfg;
  }

  public boolean canRead(Branch.NameKey branch) {
    try {
      PermissionBackend.ForProject permissions =
          permissionBackend.user(user).project(branch.getParentKey());
      permissions.ref(branch.get()).check(RefPermission.READ);
      return true;
    } catch (AuthException | PermissionBackendException e) {
      return false;
    }
  }
}
