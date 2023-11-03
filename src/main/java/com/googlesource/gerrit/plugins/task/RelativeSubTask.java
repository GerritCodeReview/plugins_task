// Copyright (C) 2023 The Android Open Source Project
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

import com.google.auto.value.AutoValue;
import com.googlesource.gerrit.plugins.task.properties.AbstractExpander;

@AutoValue
public abstract class RelativeSubTask
    implements AbstractExpander.UnExpandedStringProvider,
        AbstractExpander.InstanceWithExpandedStringProvider<RelativeSubTask> {
  public static RelativeSubTask create(FileKey file, String subTask) {
    return new AutoValue_RelativeSubTask(file, subTask);
  }

  public abstract FileKey file();

  public abstract String value();

  @Override
  public String getUnexpandedString() {
    return value();
  }

  @Override
  public RelativeSubTask createInstance(String expanded) {
    return create(file(), expanded);
  }
}
