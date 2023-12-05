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

package com.googlesource.gerrit.plugins.task.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Use to expand properties whose values may contain other references to properties.
 *
 * <p>Using a recursive expansion approach makes order of evaluation unimportant as long as there
 * are no looping definitions.
 *
 * <p>Given some property name/value asssociations defined like this:
 *
 * <p><code>
 * valueByName.put("obstacle", "fence");
 * valueByName.put("action", "jumped over the ${obstacle}");
 * </code>
 *
 * <p>a String like: <code>"The brown fox ${action}."</code>
 *
 * <p>will expand to: <code>"The brown fox jumped over the fence."</code>
 */
public class Expander extends AbstractExpander {
  protected final Function<String, String> loadingFunction;
  protected final Map<String, String> valueByName = new HashMap<>();
  protected final Set<String> expanding = new HashSet<>();

  public Expander(Function<String, String> loadingFunction, FieldConverter fieldConverter) {
    super(fieldConverter);
    this.loadingFunction = loadingFunction;
  }

  /**
   * Expand all properties (${property_name} -> property_value) in the given text. Returns same
   * object if no expansions occurred.
   */
  public Map<String, String> expand(Map<String, String> map) {
    if (map != null) {
      boolean hasProperty = false;
      Map<String, String> expandedMap = new HashMap<>(map.size());
      for (Map.Entry<String, String> e : map.entrySet()) {
        String name = e.getKey();
        String value = e.getValue();
        String expanded = getValueForName(name);
        hasProperty = hasProperty || value != expanded;
        expandedMap.put(name, expanded);
      }
      return hasProperty ? Collections.unmodifiableMap(expandedMap) : map;
    }
    return null;
  }

  @Override
  public String getValueForName(String name) {
    String value = valueByName.get(name);
    if (value != null) {
      return value;
    }
    value = loadingFunction.apply(name);
    if (value == null) {
      value = "";
    } else if (!value.isEmpty()) {
      if (!expanding.add(name)) {
        throw new RuntimeException("Looping property definitions.");
      }
      value = expandText(value);
      expanding.remove(name);
    }
    valueByName.put(name, value);
    return value;
  }
}
