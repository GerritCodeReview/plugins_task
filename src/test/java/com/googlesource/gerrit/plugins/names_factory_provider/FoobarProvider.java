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
import java.util.stream.Collectors;

public class FoobarProvider implements PluginProvidedTaskNamesFactory {
  public static final String DELIMITER = "-";

  @Override
  public List<String> getNames(ChangeData changeData, List<String> args) throws Exception {
    String name = String.join(DELIMITER, "foobar", changeData.project().get());
    if (args == null || args.isEmpty()) {
      return List.of(name);
    }
    return new ArrayList<>(
        args.stream().map(x -> String.join(DELIMITER, name, x)).collect(Collectors.toList()));
  }
}
