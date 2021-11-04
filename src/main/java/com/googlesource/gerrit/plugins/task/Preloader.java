// Copyright (C) 2019 The Android Open Source Project
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

import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.errors.ConfigInvalidException;

/** Use to pre-load a task definition with values from its preload-task definition. */
public class Preloader {
  public static void preload(Task definition) throws ConfigInvalidException {
    String name = definition.preloadTask;
    if (name != null) {
      Task task = definition.config.getTaskOptional(name);
      if (task != null) {
        preload(task);
        preloadFrom(definition, task);
      }
    }
  }

  protected static void preloadFrom(Task definition, Task preloadFrom) {
    for (Field field : definition.getClass().getFields()) {
      String name = field.getName();
      if ("isVisible".equals(name) || "isTrusted".equals(name) || "config".equals(name)) {
        continue;
      }

      try {
        field.setAccessible(true);
        preloadField(field.getType(), field, definition, preloadFrom);
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new RuntimeException();
      }
    }
  }

  protected static <T, S, K, V> void preloadField(
      Class<T> clz, Field field, Task definition, Task preloadFrom)
      throws IllegalArgumentException, IllegalAccessException {
    T pre = getField(clz, field, preloadFrom);
    if (pre != null) {
      T val = getField(clz, field, definition);
      if (val == null) {
        field.set(definition, pre);
      } else if (val instanceof List) {
        List<?> valList = List.class.cast(val);
        List<?> preList = List.class.cast(pre);
        field.set(definition, preloadListFrom(castUnchecked(valList), castUnchecked(preList)));
      } else if (val instanceof Map) {
        Map<?, ?> valMap = Map.class.cast(val);
        Map<?, ?> preMap = Map.class.cast(pre);
        field.set(definition, preloadMapFrom(castUnchecked(valMap), castUnchecked(preMap)));
      } // nothing to do for overridden preloaded scalars
    }
  }

  protected static <T> T getField(Class<T> clz, Field field, Object obj)
      throws IllegalArgumentException, IllegalAccessException {
    return clz.cast(field.get(obj));
  }

  @SuppressWarnings("unchecked")
  protected static <S> List<S> castUnchecked(List<?> list) {
    List<S> forceCheck = (List<S>) list;
    return forceCheck;
  }

  @SuppressWarnings("unchecked")
  protected static <K, V> Map<K, V> castUnchecked(Map<?, ?> map) {
    Map<K, V> forceCheck = (Map<K, V>) map;
    return forceCheck;
  }

  protected static <T> List<T> preloadListFrom(List<T> list, List<T> preList) {
    if (preList.isEmpty()) {
      return list;
    }
    if (list.isEmpty()) {
      return preList;
    }

    List<T> extended = new ArrayList<>(list.size() + preList.size());
    extended.addAll(preList);
    extended.addAll(list);
    return extended;
  }

  protected static <K, V> Map<K, V> preloadMapFrom(Map<K, V> map, Map<K, V> preMap) {
    if (preMap.isEmpty()) {
      return map;
    }
    if (map.isEmpty()) {
      return preMap;
    }

    Map<K, V> extended = new HashMap<>(map.size() + preMap.size());
    extended.putAll(preMap);
    extended.putAll(map);
    return extended;
  }
}
