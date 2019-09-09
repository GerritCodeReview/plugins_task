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
import java.lang.IllegalAccessException;
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
      if (name.equals("isVisible") || name.equals("isTrusted") || name.equals("config")) {
        continue;
      }

      try {
        field.setAccessible(true);
        Object pre = field.get(preloadFrom);
        if (pre != null) {
          Object val = field.get(definition);
          if (val == null) {
            field.set(definition, pre);
          } else if (val instanceof List) {
            field.set(definition, preloadListFrom((List) val, (List) pre));
          } else if (val instanceof Map) {
            field.set(definition, preloadMapFrom((Map) val, (Map) pre));
          } // nothing to do for overridden preloaded scalars
        }
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new RuntimeException();
      }
    }
  }

  protected static List preloadListFrom(List list, List preList) {
    List extended = list;
    if (!preList.isEmpty()) {
      extended = preList;
      if (!list.isEmpty()) {
        extended = new ArrayList(list.size() + preList.size());
        extended.addAll(preList);
        extended.addAll(list);
      }
    }
    return extended;
  }

  protected static Map preloadMapFrom(Map map, Map preMap) {
    Map extended = map;
    if (!preMap.isEmpty()) {
      extended = preMap;
      if (!map.isEmpty()) {
        extended = new HashMap(map.size() + preMap.size());
        extended.putAll(preMap);
        extended.putAll(map);
      }
    }
    return extended;
  }
}
