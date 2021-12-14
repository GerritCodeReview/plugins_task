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

/** An immutable reference to a fully qualified file in gerrit repo. */
@AutoValue
public abstract class FileKey {
  public static FileKey create(Branch.NameKey branch, String file) {
    return new AutoValue_FileKey(branch, file);
  }

  public abstract Branch.NameKey branch();

  public abstract String file();
}
