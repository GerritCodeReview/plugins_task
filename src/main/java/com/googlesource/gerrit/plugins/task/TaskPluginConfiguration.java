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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.jgit.lib.Config;

@Singleton
public class TaskPluginConfiguration {
  private static final String CACHEABLE_PREDICATES = "cacheablePredicates";
  private static final String CACHEABLE_PREDICATES_SECTION = "byBranch";
  private static final String CACHEABLE_PREDICATES_KEY = "className";
  private static final String DEPRECATED_CACHEABLE_PREDICATES = "cacheable-predicates";
  private static final String DEPRECATED_CACHEABLE_PREDICATES_KEY = "byBranch-className";

  private final Set<String> cacheableByBranchPredicateClassNames;
  private final Config gerritConfig;
  private final Config pluginConfig;
  private final String plugin;

  @Inject
  public TaskPluginConfiguration(
      @PluginName String plugin,
      @GerritServerConfig Config gerritConfig,
      PluginConfigFactory pluginConfigFactory) {
    this.plugin = plugin;
    this.gerritConfig = gerritConfig;
    this.pluginConfig = pluginConfigFactory.getGlobalPluginConfig(plugin);
    cacheableByBranchPredicateClassNames =
        new HashSet<>(Arrays.asList(readCacheableByBranchPredicateClassNames()));
  }

  public Set<String> getCacheableByBranchPredicateClassNames() {
    return cacheableByBranchPredicateClassNames;
  }

  private String[] readCacheableByBranchPredicateClassNames() {
    String[] fromPluginConfig =
        pluginConfig.getStringList(
            CACHEABLE_PREDICATES, CACHEABLE_PREDICATES_SECTION, CACHEABLE_PREDICATES_KEY);
    if (fromPluginConfig.length > 0) {
      return fromPluginConfig;
    }
    // Read from gerrit config for backward compatibility. This can be removed once all known users
    // have migrated to plugin config.
    return gerritConfig.getStringList(
        plugin, DEPRECATED_CACHEABLE_PREDICATES, DEPRECATED_CACHEABLE_PREDICATES_KEY);
  }
}
