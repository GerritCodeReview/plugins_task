// Copyright (C) 2020 The Android Open Source Project
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

public interface ThrowingProvider<V, E extends Exception> {
  public V get() throws E;

  public static class Entry<V, E extends Exception> implements ThrowingProvider<V, E> {
    protected V entry;

    public Entry(V entry) {
      this.entry = entry;
    }

    @Override
    public V get() {
      return entry;
    }
  }

  public static class Thrown<V, E extends Exception> implements ThrowingProvider<V, E> {
    protected E exception;

    public Thrown(E exception) {
      this.exception = exception;
    }

    @Override
    public V get() throws E {
      throw exception;
    }
  }
}
