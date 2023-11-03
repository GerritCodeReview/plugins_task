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

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ConfigSourcedValue {
  public static ConfigSourcedValue create(FileKey sourceFile, String value) {
    return new AutoValue_ConfigSourcedValue(sourceFile, value);
  }

  public static Class<? extends ConfigSourcedValue> getClassType() {
    return AutoValue_ConfigSourcedValue.class;
  }

  public abstract FileKey sourceFile();

  public abstract String value();
}
