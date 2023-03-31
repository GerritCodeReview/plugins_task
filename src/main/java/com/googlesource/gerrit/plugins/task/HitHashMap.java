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

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class HitHashMap<K, V> extends HashMap<K, V> implements StatisticsMap<K, V> {
  public static class Statistics {
    public long hits;
    public int size;
    public Long sumNanosecondsLoading;
    public List<Object> elements;
  }

  public static final long serialVersionUID = 1;

  protected Statistics statistics;

  public HitHashMap() {}

  public HitHashMap(boolean initStatistics) {
    if (initStatistics) {
      initStatistics();
    }
  }

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
  @SuppressWarnings("try")
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
  @SuppressWarnings("try")
  public V computeIfAbsentTimed(K key, Function<? super K, ? extends V> mappingFunction) {
    V v = get(key);
    if (v == null) {
      try (StopWatch stopWatch = createLoadingStopWatch()) {
        v = mappingFunction.apply(key);
      }
      if (v != null) {
        put(key, v);
      }
    }
    return v;
  }

  @Override
  public V put(K key, V value) {
    if (statistics != null && value instanceof TracksStatistics) {
      ((TracksStatistics) value).ensureStatistics();
    }
    return super.put(key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    m.entrySet().stream().forEach(e -> put(e.getKey(), e.getValue()));
  }

  @Override
  public V putIfAbsent(K key, V value) {
    if (!containsKey(key)) {
      put(key, value);
      return null;
    }
    return get(key);
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
  public V replace(K key, V value) {
    throw new UnsupportedOperationException(); // Todo if needed
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    throw new UnsupportedOperationException(); // Todo if needed
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    throw new UnsupportedOperationException(); // Todo if needed
  }

  @Override
  public void initStatistics() {
    statistics = new Statistics();
  }

  @Override
  public void ensureStatistics() {
    if (statistics == null) {
      initStatistics();
    }
  }

  public StopWatch createLoadingStopWatch() {
    if (statistics == null) {
      return new StopWatch.Disabled();
    }
    if (statistics.sumNanosecondsLoading == null) {
      statistics.sumNanosecondsLoading = 0L;
    }
    return new StopWatch.Enabled().setNanosConsumer(ns -> statistics.sumNanosecondsLoading += ns);
  }

  @Override
  public Object getStatistics() {
    statistics.size = size();
    List<Object> elementStatistics =
        values().stream()
            .filter(e -> e instanceof TracksStatistics)
            .map(e -> ((TracksStatistics) e).getStatistics())
            .collect(toList());
    if (!elementStatistics.isEmpty()) {
      statistics.elements = elementStatistics;
    }
    return statistics;
  }
}
