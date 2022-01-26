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

import java.util.function.Function;

public class CopyOnWrite<T> {
  protected Function<T, T> copier;
  protected T original;
  protected T copy;

  public CopyOnWrite(T original, Function<T, T> copier) {
    this.original = original;
    this.copier = copier;
  }

  public T getOriginal() {
    return original;
  }

  public T getForRead() {
    return copy != null ? copy : original;
  }

  public T getForWrite() {
    if (copy == null) {
      copy = copier.apply(original);
    }
    return copy;
  }
}
