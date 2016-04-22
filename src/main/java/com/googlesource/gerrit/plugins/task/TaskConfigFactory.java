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

import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskConfigFactory {
  private static final Logger log = LoggerFactory.getLogger(TaskConfigFactory.class);

  protected static final String EXTENSION = ".config";
  protected static final String DEFAULT = "task" + EXTENSION;

  protected final GitRepositoryManager gitMgr;
  protected final AllProjectsName allProjects;

  @Inject
  protected TaskConfigFactory(AllProjectsName allProjects, GitRepositoryManager gitMgr) {
    this.allProjects = allProjects;
    this.gitMgr = gitMgr;
  }

  public TaskConfig getRootConfig() throws ConfigInvalidException, IOException {
    return getTaskConfig(getRootBranch(), DEFAULT);
  }

  protected Branch.NameKey getRootBranch() {
    return new Branch.NameKey(allProjects, "refs/meta/config");
  }

  public TaskConfig getTaskConfig(Branch.NameKey branch, String fileName)
      throws ConfigInvalidException, IOException {
    TaskConfig cfg = new TaskConfig(branch, fileName);
    Project.NameKey project = branch.getParentKey();
    try (Repository git = gitMgr.openRepository(project)) {
      cfg.load(project, git);
    } catch (IOException e) {
      log.warn("Failed to load " + fileName + " for " + project.get(), e);
      throw e;
    } catch (ConfigInvalidException e) {
      throw e;
    }
    return cfg;
  }
}
