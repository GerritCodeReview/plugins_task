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

package com.googlesource.gerrit.plugins.task.util;

import com.googlesource.gerrit.plugins.task.util.function.SupplierThrows3;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

/**
 * A Supplier which can cache a value softly, and (re)load it on demand when needed.
 */
public class CachingSupplierThrows3<T, E1 extends Exception, E2 extends Exception, E3 extends Exception> {
  protected static final Reference<?> EMPTY = new SoftReference<>(null);

  protected Reference<T> element;
  protected SupplierThrows3<T, E1, E2, E3> supplier;
  protected boolean wasLoaded;

  public CachingSupplierThrows3(SupplierThrows3<T, E1, E2, E3> supplier) {
    this.supplier = supplier;
    clear();
  }

  /** Get the value from the cache, or load it if needed by calling the supplier */
  public T get() throws E1, E2, E3 {
    T t = element.get();
    wasLoaded = t != null;
    if (!wasLoaded) {

      t = supplier.get();
      element = new SoftReference<>(t);
    }
    return t;
  }

  /** Did the last call to get() return an already loaded and cached value? */
  public boolean wasLoaded() {
    return wasLoaded;
  }

  /** Clear and release for gc the cached value */
  @SuppressWarnings("unchecked")
  public void clear() {
    element =  (Reference<T>) EMPTY;
  }
}

