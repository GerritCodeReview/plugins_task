// Copyright (C) 2016 The Android Open Source Project
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

package com.google.gerrit.common;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* A data container, all fields considered in equals and hash */
public class Container {
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    try {
      for (Field field : getClass().getDeclaredFields()) {
        field.setAccessible(true);
        if (!Objects.deepEquals(field.get(this), field.get(o))) {
          return false;
        }
      }
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException();
    }
    return true;
  }

  @Override
  public int hashCode() {
    List<Object> values = new ArrayList<>();
    try {
      for (Field field : getClass().getDeclaredFields()) {
        field.setAccessible(true);
        values.add(field.get(this));
      }
    } catch (IllegalArgumentException | IllegalAccessException e) {
    }
    return Objects.hash(values);
  }

  @Override
  public String toString() {
    List<String> fieldStrings = new ArrayList<>();
    try {
      for (Field field : getClass().getDeclaredFields()) {
        field.setAccessible(true);
        fieldStrings.add(field.getName() + ": " + Objects.toString(field.get(this)));
      }
    } catch (IllegalArgumentException | IllegalAccessException e) {
    }
    String fields = String.join(", ", fieldStrings);
    if (!"".equals(fields)) {
      fields = "{" + fields + "}";
    }
    return getClass().toString() + fields;
  }
}
