// Copyright (C) 2022 The Android Open Source Project
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

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public class HitHashMap<K, V> extends HashMap<K, V> implements StatisticsMap<K, V> {
  public static class Statistics {
    public long hits;
    public int size;
  }

  public static final long serialVersionUID = 1;

  protected Statistics statistics;

  @Override
  public V get(Object key) {
    V v = super.get(key);
    if (statistics != null && v != null) {
      statistics.hits++;
    }
    return v;
  }

  @Override
  public V getOrDefault(Object key, V dv) {
    V v = get(key);
    if (v == null) {
      return dv;
    }
    return v;
  }

  @Override
  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    V v = get(key);
    if (v == null) {
      v = mappingFunction.apply(key);
      if (v != null) {
        put(key, v);
      }
    }
    return v;
  }

  @Override
  public V computeIfPresent(
      K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    throw new UnsupportedOperationException(); // Todo if needed
  }

  @Override
  public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    throw new UnsupportedOperationException(); // Todo if needed
  }

  @Override
  public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    throw new UnsupportedOperationException(); // Todo if needed
  }

  @Override
  public void initStatistics() {
    statistics = new Statistics();
  }

  @Override
  public Object getStatistics() {
    statistics.size = size();
    return statistics;
  }
}
