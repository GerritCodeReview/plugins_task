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

package com.googlesource.gerrit.plugins.task.util;

import java.lang.reflect.Field;

public class Copier {
  public static <T> void shallowCopyDeclaredFields(
      Class<T> cls, T from, T to, boolean includeInaccessible) {
    for (Field field : cls.getDeclaredFields()) {
      try {
        if (includeInaccessible) {
          field.setAccessible(true);
        }
        Object val = field.get(from);
        if (!field.getName().equals("this$0")) { // Can't copy internal final field
          field.set(to, val);
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
