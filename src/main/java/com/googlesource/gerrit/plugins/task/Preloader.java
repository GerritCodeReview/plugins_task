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
import java.util.NoSuchElementException;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;

/** Use to pre-load a task definition with values from its preload-task definition. */
public class Preloader {
  protected final Map<TaskExpressionKey, Optional<Task>> optionalTaskByExpression = new HashMap<>();

  public List<Task> getRootTasks(TaskConfig cfg) {
    return getTasks(cfg, TaskConfig.SECTION_ROOT);
  }

  public List<Task> getTasks(TaskConfig cfg) {
    return getTasks(cfg, TaskConfig.SECTION_TASK);
  }

  protected List<Task> getTasks(TaskConfig cfg, String type) {
    List<Task> preloaded = new ArrayList<>();
    for (Task task : cfg.getTasks(type)) {
      try {
        preloaded.add(preload(task));
      } catch (ConfigInvalidException e) {
        preloaded.add(null);
      }
    }
    return preloaded;
  }

  /**
   * Get a preloaded Task for this TaskExpression.
   *
   * @param expression
   * @return Optional<Task> which is empty if the expression is optional and no tasks are resolved
   * @throws ConfigInvalidException if the expression requires a task and no tasks are resolved
   */
  public Optional<Task> getOptionalTask(TaskConfig cfg, TaskExpression expression)
      throws ConfigInvalidException {
    Optional<Task> task = optionalTaskByExpression.get(expression.key);
    if (task == null) {
      task = preloadOptionalTask(cfg, expression);
      optionalTaskByExpression.put(expression.key, task);
    }
    return task;
  }

  protected Optional<Task> preloadOptionalTask(TaskConfig cfg, TaskExpression expression)
      throws ConfigInvalidException {
    Optional<Task> definition = loadOptionalTask(cfg, expression);
    return definition.isPresent() ? Optional.of(preload(definition.get())) : definition;
  }

  public Task preload(Task definition) throws ConfigInvalidException {
    String expression = definition.preloadTask;
    if (expression != null) {
      Optional<Task> preloadFrom =
          getOptionalTask(definition.config, new TaskExpression(definition.file(), expression));
      if (preloadFrom.isPresent()) {
        return preloadFrom(definition, preloadFrom.get());
      }
    }
    return definition;
  }

  protected Optional<Task> loadOptionalTask(TaskConfig cfg, TaskExpression expression)
      throws ConfigInvalidException {
    try {
      for (String name : expression) {
        Optional<Task> task = cfg.getOptionalTask(name);
        if (task.isPresent()) {
          return task;
        }
      }
    } catch (NoSuchElementException e) {
      // expression was not optional but we ran out of names to try
      throw new ConfigInvalidException("task not defined");
    }
    return Optional.empty();
  }

  protected static Task preloadFrom(Task definition, Task preloadFrom) {
    Task preloadTo = definition.config.new Task(definition.subSection);
    for (Field field : definition.getClass().getFields()) {
      String name = field.getName();
      if ("config".equals(name)) {
        continue;
      }

      try {
        field.setAccessible(true);
        preloadField(field, definition, preloadFrom, preloadTo);
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new RuntimeException();
      }
    }
    return preloadTo;
  }

  protected static <S, K, V> void preloadField(
      Field field, Task definition, Task preloadFrom, Task preloadTo)
      throws IllegalArgumentException, IllegalAccessException {
    Object pre = field.get(preloadFrom);
    Object val = field.get(definition);
    if (val == null) {
      field.set(preloadTo, pre);
    } else if (pre == null) {
      field.set(preloadTo, val);
    } else if (val instanceof List) {
      List<?> valList = List.class.cast(val);
      List<?> preList = List.class.cast(pre);
      field.set(preloadTo, preloadListFrom(castUnchecked(valList), castUnchecked(preList)));
    } else if (val instanceof Map) {
      Map<?, ?> valMap = Map.class.cast(val);
      Map<?, ?> preMap = Map.class.cast(pre);
      field.set(preloadTo, preloadMapFrom(castUnchecked(valMap), castUnchecked(preMap)));
    } else {
      field.set(preloadTo, val);
    }
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