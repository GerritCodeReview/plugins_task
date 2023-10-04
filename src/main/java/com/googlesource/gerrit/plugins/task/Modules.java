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

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.config.CapabilityDefinition;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.webui.JavaScriptPlugin;
import com.google.gerrit.extensions.webui.WebUiPlugin;
import com.google.gerrit.server.DynamicOptions.DynamicBean;
import com.google.gerrit.server.change.ChangePluginDefinedInfoFactory;
import com.google.gerrit.server.restapi.change.GetChange;
import com.google.gerrit.server.restapi.change.QueryChanges;
import com.google.gerrit.sshd.commands.Query;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.task.cli.PatchSetArgument;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Option;

public class Modules {
  public static class Module extends FactoryModule {
    @Override
    protected void configure() {
      bind(CapabilityDefinition.class)
          .annotatedWith(Exports.named(ViewPathsCapability.VIEW_PATHS))
          .to(ViewPathsCapability.class);
      factory(TaskPath.Factory.class);
      factory(TaskReference.Factory.class);
      factory(TaskExpression.Factory.class);

      bind(ChangePluginDefinedInfoFactory.class)
          .annotatedWith(Exports.named("task"))
          .to(TaskAttributeFactory.class);

      install(new IsTrueOperator.Module());

      bind(DynamicBean.class).annotatedWith(Exports.named(GetChange.class)).to(MyOptions.class);
      bind(DynamicBean.class).annotatedWith(Exports.named(Query.class)).to(MyOptions.class);
      bind(DynamicBean.class).annotatedWith(Exports.named(QueryChanges.class)).to(MyOptions.class);
      DynamicSet.bind(binder(), WebUiPlugin.class)
          .toInstance(new JavaScriptPlugin("gr-task-plugin.js"));
    }
  }

  public static class MyOptions implements DynamicBean {
    @Option(name = "--all", usage = "Include all visible tasks in the output")
    public boolean all = false;

    @Option(name = "--applicable", usage = "Include only applicable tasks in the output")
    public boolean onlyApplicable = false;

    @Option(
        name = "--invalid",
        usage = "Include only invalid tasks and the tasks referencing them in the output")
    public boolean onlyInvalid = false;

    @Option(name = "--include-paths", usage = "Include absolute path to each task")
    public boolean includePaths = false;

    @Option(name = "--evaluation-time", usage = "Include elapsed evaluation time on each task")
    public boolean evaluationTime = false;

    @Option(name = "--include-statistics", usage = "Include statistcs about the task evaluations")
    public boolean includeStatistics = false;

    @Option(name = "--summary-count", usage = "number of items to output in statistics summaries")
    public int summaryCount = 5;

    @Option(
        name = "--preview",
        metaVar = "{CHANGE,PATCHSET}",
        usage = "list of patch sets to preview task evaluation for")
    public void addPatchSet(String token) {
      PatchSetArgument psa = patchSetArgumentFactory.createForArgument(token);
      patchSetArguments.add(psa);
    }

    public List<PatchSetArgument> patchSetArguments = new ArrayList<>();

    public PatchSetArgument.Factory patchSetArgumentFactory;

    @Inject
    public MyOptions(PatchSetArgument.Factory patchSetArgumentFactory) {
      this.patchSetArgumentFactory = patchSetArgumentFactory;
    }
  }
}
