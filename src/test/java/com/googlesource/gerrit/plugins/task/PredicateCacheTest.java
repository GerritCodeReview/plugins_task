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

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.AllProjectsNameProvider;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.query.change.SubmitRequirementChangeQueryBuilder;
import com.google.inject.Inject;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

@TestPlugin(name = "task", sysModule = "com.googlesource.gerrit.plugins.task.Modules$Module")
public class PredicateCacheTest extends LightweightPluginDaemonTest {
  private static final String PLUGIN = "task";

  @Mock private CurrentUser currentUser;
  @Inject private SubmitRequirementChangeQueryBuilder srcqb;
  @Inject private PluginConfigFactory pluginConfigFactory;
  @Inject private AllProjectsNameProvider allProjectsNameProvider;
  @Inject private Config config;
  private TaskPluginConfiguration taskPluginConfiguration;
  private PredicateCache predicateCache;

  @Before
  public void setUp() {
    taskPluginConfiguration =
        new TaskPluginConfiguration(PLUGIN, config, pluginConfigFactory, allProjectsNameProvider);
    predicateCache = new PredicateCache(taskPluginConfiguration, currentUser, srcqb);
  }

  @Test
  public void isCacheableByBranchTest() throws Exception {
    assertThat(predicateCache.isCacheableByBranch("project:p1", true)).isTrue();
    assertThat(predicateCache.isCacheableByBranch("branch:b1", true)).isTrue();
    assertThat(predicateCache.isCacheableByBranch("project:p1 branch:b1", true)).isTrue();

    assertThat(predicateCache.isCacheableByBranch("topic:t1", true)).isFalse();
    assertThat(predicateCache.isCacheableByBranch("label:Code-Review+1", true)).isFalse();
    assertThat(predicateCache.isCacheableByBranch("topic:t1 project:p1", true)).isFalse();
  }
}
