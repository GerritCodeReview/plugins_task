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
import com.google.gerrit.server.DynamicOptions.DynamicBean;
import com.google.gerrit.server.query.change.ChangeQueryProcessor.ChangeAttributeFactory;
import com.google.gerrit.server.restapi.change.QueryChanges;
import com.google.gerrit.sshd.commands.Query;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.task.cli.PatchSetArgument;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Option;

public class Modules {
  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      bind(ChangeAttributeFactory.class)
          .annotatedWith(Exports.named("task"))
          .to(TaskAttributeFactory.class);
    }
  }

  public static class SshModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(DynamicBean.class).annotatedWith(Exports.named(Query.class)).to(MyOptions.class);
    }
  }

  public static class HttpModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(DynamicBean.class).annotatedWith(Exports.named(QueryChanges.class)).to(MyOptions.class);
    }
  }

  public static class MyOptions implements DynamicBean {
    @Option(name = "--all", usage = "Include all visible tasks in the output")
    public boolean all = false;

    @Option(name = "--applicable", usage = "Include only applicable tasks in the output")
    public boolean onlyApplicable = false;

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
