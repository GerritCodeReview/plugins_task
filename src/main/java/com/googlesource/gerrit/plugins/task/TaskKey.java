// Copyright (C) 2021 The Android Open Source Project
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
import com.google.gerrit.reviewdb.client.Branch;

/** An immutable reference to a task in task config file. */
@AutoValue
public abstract class TaskKey {
  protected static final String CONFIG_SECTION = "task";

  /** Creates a TaskKey with task name as the name of sub section. */
  public static TaskKey create(SubSectionKey section) {
    return create(section, section.subSection());
  }

  /** Creates a TaskKey with given FileKey and task name and sub section's name as 'task'. */
  public static TaskKey create(FileKey file, String task) {
    return create(SubSectionKey.create(file, CONFIG_SECTION, task));
  }

  /** Creates a TaskKey from a sub section and task name, generally used by TasksFactory. */
  public static TaskKey create(SubSectionKey section, String task) {
    return new AutoValue_TaskKey(section, task);
  }

  public Branch.NameKey branch() {
    return subSection().file().branch();
  }

  public abstract SubSectionKey subSection();

  public abstract String task();
}
