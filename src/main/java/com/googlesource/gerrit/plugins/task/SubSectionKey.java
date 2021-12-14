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

/** An immutable reference to a SubSection in fully qualified task config file. */
@AutoValue
public abstract class SubSectionKey {
  public static SubSectionKey create(FileKey file, String section, String subSection) {
    return new AutoValue_SubSectionKey(file, section, subSection == null ? "" : subSection);
  }

  public abstract FileKey file();

  public abstract String section();

  public abstract String subSection();
}
