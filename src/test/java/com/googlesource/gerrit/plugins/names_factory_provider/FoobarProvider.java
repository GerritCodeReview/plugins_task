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

package com.googlesource.gerrit.plugins.names_factory_provider;

import com.google.gerrit.server.query.change.ChangeData;
import com.googlesource.gerrit.plugins.task.extensions.PluginProvidedTaskNamesFactory;
import java.util.ArrayList;
import java.util.List;

public class FoobarProvider implements PluginProvidedTaskNamesFactory {
  @Override
  public List<String> getNames(ChangeData changeData, List<String> args) throws Exception {
    String delimiter = "-";
    String name =
        new StringBuilder("foobar").append(delimiter).append(changeData.project().get()).toString();
    if (args != null && args.size() == 2) {
      List<String> names = new ArrayList<>();
      String suffix = args.get(0);
      int cnt = Integer.parseInt(args.get(1));
      for (int i = 1; i <= cnt; i++) {
        names.add(
            new StringBuilder(name)
                .append(delimiter)
                .append(i)
                .append(delimiter)
                .append(suffix)
                .toString());
      }
      return names;
    }
    return List.of(name);
  }
}
