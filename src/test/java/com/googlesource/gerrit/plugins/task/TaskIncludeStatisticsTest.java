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
import com.google.gerrit.common.Nullable;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.RefNames;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.googlesource.gerrit.plugins.task.TaskPluginDefinedInfoFactory.TaskPluginAttribute;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.junit.Test;

@UseSsh
public class TaskIncludeStatisticsTest extends AbstractDaemonTest {
  private static final Gson GSON = new Gson();

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
                  "gerrit query change:%s --task--applicable --task--include-statistics --format json",
                  change.getChange().getId().get()));
      adminSshSession.assertSuccess();

      Map<Change.Id, TaskPluginAttribute> taskAttrByChange = getTaskAttributes(sshOutput);
      TaskPluginAttribute taskAttribute = taskAttrByChange.get(change.getChange().getId());
      assertThat(taskAttribute.message).isNull();
      assertThat(taskAttribute.roots.size()).isEqualTo(1);
      assertThat(taskAttribute.roots.get(0).name).isEqualTo("test root");
    }
  }

  private String getConfig() {
    return "[root \"test root\"]\n" + "  applicable = is:open\n" + "  pass = True";
  }

  private Map<Change.Id, TaskPluginAttribute> getTaskAttributes(String sshOutput) throws Exception {
    List<Map<String, Object>> changeAttrs = getChangeAttrs(sshOutput);
    Map<Change.Id, TaskPluginAttribute> taskAttrByChange = new HashMap<>();
    changeAttrs.forEach(
        change -> {
          Double changeId = (Double) change.get("number");
          taskAttrByChange.put(
              Change.id(changeId.intValue()),
              deserializeTaskAttributeFromPluginList(change.get("plugins")));
        });
    return taskAttrByChange;
  }

  private List<Map<String, Object>> getChangeAttrs(String sshOutput) throws Exception {
    List<Map<String, Object>> changeAttrs = new ArrayList<>();
    try (BufferedReader buffer = new BufferedReader(new StringReader(sshOutput))) {
      buffer
          .lines()
          .forEach(
              line -> {
                Map<String, Object> changeAttr =
                    GSON.fromJson(line, new TypeToken<Map<String, Object>>() {}.getType());
                if (!"stats".equals(changeAttr.get("type"))) {
                  changeAttrs.add(changeAttr);
                }
              });
    }
    return changeAttrs;
  }

  private TaskPluginAttribute deserializeTaskAttributeFromPluginList(@Nullable Object plugins) {
    if (plugins == null) {
      return null;
    }
    return GSON.fromJson(
        GSON.toJson(((List<?>) plugins).get(0)), new TypeToken<TaskPluginAttribute>() {}.getType());
  }
}
