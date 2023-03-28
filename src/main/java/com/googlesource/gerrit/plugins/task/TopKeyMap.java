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

/**
 * A TopKeyMap is a lightweight limited size (default 5) map with 'long' keys designed to store only
 * the elements with the top five largest keys.
 *
 * <p>A TopKeyMap is array based and has O(n) insertion time. Despite not having O(1) insertion
 * times, it should likely be much faster than a hash based map for small n sizes. It also is more
 * memory efficient than a hash based map, although both are likely O(n) in space usage. The
 * TopKeyMap allocates all of its entries up front so it does not change its memory utilization at
 * all, and it does not have to create or free any Objects during its post constructor lifespan.
 *
 * <p>While a TopKeyMap currently only uses 'long's as keys, it is possible to easiy upgrade this
 * collection to use any type of Comparable key.
 *
 * <p>Although not currently thread safe, due to the simplicity of the data structures used, and the
 * insertion approach, it is easy to make a TopKeyMap efficiently thread safe.
 */
public class TopKeyMap<V> {
  /**
   * A TableKeyValue is a helper class for TopKeyMap use cases, such as a table with with row and
   * column keys, which involve two values.
   */
  public static class TableKeyValue<R, C> {
    public final R row;
    public final C column;

    public TableKeyValue(R row, C column) {
      this.row = row;
      this.column = column;
    }
  }

  protected class Entry {
    public long key;
    public V value;

    protected void set(long key, V value) {
      this.key = key;
      this.value = value;
    }
  }

  protected Entry[] entries;

  public TopKeyMap() {
    this(5);
  }

  @SuppressWarnings("unchecked")
  public TopKeyMap(int length) {
    entries = (Entry[]) new Object[length];
    for (int i = 0; i < entries.length; i++) {
      entries[i] = new Entry();
    }
  }

  public void addIfTop(long key, V value) {
    addIfTop(0, key, value);
  }

  protected void addIfTop(int i, long key, V value) {
    if (entries[entries.length - 1].key < key) {
      for (; i < entries.length; i++) {
        Entry e = entries[i];
        if (e.key < key) {
          long eKValue = e.key;
          V eValue = e.value;
          e.set(key, value);
          addIfTop(i + 1, eKValue, eValue);
          return;
        }
      }
    }
  }

  public int size() {
    return entries.length;
  }
}
