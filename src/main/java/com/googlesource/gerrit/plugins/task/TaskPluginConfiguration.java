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
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TaskPluginConfiguration {
  private static final String CACHEABLE_PREDICATES = "cacheablePredicates";
  private static final String CACHEABLE_PREDICATES_SECTION = "byBranch";
  private static final String CACHEABLE_PREDICATES_KEY = "className";

  private final Set<String> cacheableByBranchPredicateClassNames;

  @Inject
  public TaskPluginConfiguration(
      PluginConfigFactory pluginConfigFactory, @PluginName String pluginName) {
    cacheableByBranchPredicateClassNames =
        new HashSet<>(
            Arrays.asList(
                pluginConfigFactory
                    .getGlobalPluginConfig(pluginName)
                    .getStringList(
                        CACHEABLE_PREDICATES,
                        CACHEABLE_PREDICATES_SECTION,
                        CACHEABLE_PREDICATES_KEY)));
  }

  public Set<String> getCacheableByBranchPredicateClassNames() {
    return cacheableByBranchPredicateClassNames;
  }
}
