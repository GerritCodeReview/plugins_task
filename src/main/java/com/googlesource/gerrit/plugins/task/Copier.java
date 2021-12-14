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

import com.google.common.primitives.Primitives;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Copier {
  protected static <T> void deepCopyDeclaredFields(
      Class<T> cls,
      T from,
      T to,
      boolean includeInaccessible,
      Collection<Class<?>> copyReferenceOnly) {
    for (Field field : cls.getDeclaredFields()) {
      try {
        if (includeInaccessible) {
          field.setAccessible(true);
        }
        Class<?> fieldCls = field.getType();
        Object val = field.get(from);
        if (field.getType().isPrimitive()
            || Primitives.isWrapperType(fieldCls)
            || (val instanceof String)
            || val == null
            || copyReferenceOnly.contains(fieldCls)) {
          field.set(to, val);
        } else if (val instanceof List) {
          List<?> list = List.class.cast(val);
          field.set(to, new ArrayList<>(list));
        } else if (val instanceof Map) {
          Map<?, ?> map = Map.class.cast(val);
          field.set(to, new HashMap<>(map));
        } else if (field.getName().equals("this$0")) { // Can't copy internal final field
        } else {
          throw new RuntimeException(
              "Don't know how to deep copy " + fieldValueToString(field, val));
        }
      } catch (IllegalAccessException e) {
        if (includeInaccessible) {
          throw new RuntimeException(
              "Cannot access field to copy it " + fieldValueToString(field, "unknown"));
        }
      }
    }
  }

  protected static String fieldValueToString(Field field, Object val) {
    return "field:" + field.getName() + " value:" + val + " type:" + field.getType();
  }
}
