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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.acceptance.GitUtil.fetch;
import static com.google.gerrit.server.query.change.OutputStreamQuery.GSON;

import com.google.common.io.CharStreams;
import com.google.gerrit.acceptance.AbstractDaemonTest;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.UseSsh;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.server.DynamicOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.googlesource.gerrit.plugins.task.TaskAttributeFactory.TaskPluginAttribute;
import com.googlesource.gerrit.plugins.task.extensions.PluginProvidedTaskNamesFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.junit.Test;

@UseSsh
public class TaskNamesFactoryIT extends AbstractDaemonTest {
  public static class NamesProvider implements PluginProvidedTaskNamesFactory {
    @Override
    public List<String> getNames(List<String> args) {
      return List.of("foo", "bar");
    }
  }

  public static class NamesFactoryProviderPluginModule extends AbstractModule {
    @Override
    public void configure() {
      bind(DynamicOptions.DynamicBean.class)
          .annotatedWith(Exports.named("names_provider"))
          .to(NamesProvider.class);
    }
  }

  @Test
  public void testPluginProvidedApi() throws Exception {
    try (AutoCloseable foobar = installPlugin("foobar", NamesFactoryProviderPluginModule.class);
        AutoCloseable task = installPlugin("task", Modules.Module.class)) {
      PushOneCommit.Result change = createChange();

      TestRepository<InMemoryRepository> repo = cloneProject(allProjects);
      fetch(repo, RefNames.REFS_CONFIG + ":metaconfig");
      repo.reset("metaconfig");

      PushOneCommit.Result r =
          pushFactory
              .create(admin.newIdent(), repo, "Update task config", "task.config", getConfig())
              .to(RefNames.REFS_CONFIG);
      r.assertOkStatus();

      String sshOutput =
          adminSshSession.exec(
              String.format(
                  "gerrit query change:%s --task--applicable --format json",
                  change.getChange().getId().get()));
      adminSshSession.assertSuccess();
      Map<Change.Id, TaskPluginAttribute> taskInfoByChange = taskInfosFromList(sshOutput);
      assertThat(taskInfoByChange.size()).isEqualTo(1);

      TaskPluginAttribute taskInfo = taskInfoByChange.get(change.getChange().getId());
      assertThat(taskInfo.roots.size()).isEqualTo(1);
      assertThat(taskInfo.roots.get(0).name).isEqualTo("qux");
      assertThat(taskInfo.roots.get(0).subTasks.size()).isEqualTo(2);
      assertThat(taskInfo.roots.get(0).subTasks.get(0).name).isEqualTo("foo");
      assertThat(taskInfo.roots.get(0).subTasks.get(1).name).isEqualTo("bar");
    }
  }

  @Nullable
  public Map<Change.Id, TaskPluginAttribute> taskInfosFromList(String sshOutput) throws Exception {
    List<Map<String, Object>> changeAttrs = getChangeAttrs(sshOutput);
    return getTaskInfosFromChangeInfos(GSON, changeAttrs);
  }

  public List<Map<String, Object>> getChangeAttrs(String sshOutput) throws Exception {
    List<Map<String, Object>> changeAttrs = new ArrayList<>();
    for (String line : CharStreams.readLines(new StringReader(sshOutput))) {
      Map<String, Object> changeAttr =
          GSON.fromJson(line, new TypeToken<Map<String, Object>>() {}.getType());
      if (!"stats".equals(changeAttr.get("type"))) {
        changeAttrs.add(changeAttr);
      }
    }
    return changeAttrs;
  }

  public Map<Change.Id, TaskPluginAttribute> getTaskInfosFromChangeInfos(
      Gson gson, List<Map<String, Object>> changeInfos) {
    Map<Change.Id, TaskPluginAttribute> out = new HashMap<>();
    changeInfos.forEach(
        change -> {
          Double changeId = (Double) change.get("number");
          out.put(
              Change.id(changeId.intValue()), decodeRawPluginsList(gson, change.get("plugins")));
        });
    return out;
  }

  public TaskPluginAttribute decodeRawPluginsList(Gson gson, @Nullable Object plugins) {
    if (plugins == null) {
      return null;
    }
    checkArgument(plugins instanceof List, "not a list: %s", plugins);
    assertThat(((List<?>) plugins).size()).isAtLeast(1);
    return gson.fromJson(
        gson.toJson(((List<?>) plugins).get(0)), new TypeToken<TaskPluginAttribute>() {}.getType());
  }

  public String getConfig() {
    return "[root \"qux\"]\n"
        + "    applicable = status:new\n"
        + "    subtasks-factory=plugin names\n"
        + "\n"
        + "[tasks-factory \"plugin names\"]\n"
        + "    names-factory = plugin provided names\n"
        + "    fail = true\n"
        + "    fail-hint = hint\n"
        + "\n"
        + "[names-factory \"plugin provided names\"]\n"
        + "    type = plugin\n"
        + "    plugin = foobar\n"
        + "    provider = names_provider\n"
        + "    arg = 123";
  }
}
