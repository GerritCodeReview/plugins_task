// Copyright (C) 2024 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.acceptance.GitUtil.fetch;
import static com.googlesource.gerrit.plugins.task.TaskFileConstants.TASK_CFG;

import com.google.gerrit.acceptance.AbstractDaemonTest;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.UseSsh;
import com.google.gerrit.entities.RefNames;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.junit.Test;

@UseSsh
public class TaskIncludeStatisticsTest extends AbstractDaemonTest {

  @Test
  public void testIncludeStatisticsDoesNotResultInError() throws Exception {
    try (AutoCloseable task = installPlugin("task", Modules.Module.class)) {
      TestRepository<InMemoryRepository> repo = cloneProject(allProjects);
      fetch(repo, RefNames.REFS_CONFIG + ":meta-config");
      repo.reset("meta-config");
      createCommitAndPush(repo, RefNames.REFS_CONFIG, "Update task config", TASK_CFG, getConfig());

      PushOneCommit.Result change = createChange();
      String sshOutput =
          adminSshSession.exec(
              String.format(
                  "gerrit query change:%s --task--applicable --task--include-statistics",
                  change.getChange().getId().get()));
      adminSshSession.assertSuccess();

      assertThat(sshOutput).contains("test root");
      assertThat(sshOutput).doesNotContain("Something went wrong in plugin: task");
    }
  }

  public String getConfig() {
    return "[root \"test root\"]\n" + "  applicable = is:open\n" + "  pass = True";
  }
}
