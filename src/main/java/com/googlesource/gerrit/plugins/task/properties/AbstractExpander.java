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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Use to expand properties like ${property} in Strings into their values.
 *
 * <p>Given some property name/value associations like this:
 *
 * <p><code>
 * "animal" -> "fox"
 * "bar" -> "foo"
 * "obstacle" -> "fence"
 * </code>
 *
 * <p>a String like: <code>"The brown ${animal} jumped over the ${obstacle}."</code>
 *
 * <p>will expand to: <code>"The brown fox jumped over the fence."</code> This class is meant to be
 * used as a building block for other full featured expanders and thus must be overriden to provide
 * the name/value associations via the getValueForName() method.
 */
public abstract class AbstractExpander {
  public interface FieldConverter {
    /**
     * Returns a string from the input object which needs to be expanded
     *
     * @param object which contains a non-expanded string
     * @return a string which needs to be expanded
     */
    <T> String fromField(T object) throws IllegalAccessException, NoSuchFieldException;

    /**
     * Creates and returns a field object with the expanded string
     *
     * @param object which contains a non-expanded string
     * @param expanded string
     * @return a field object which has expanded string
     */
    <T> T toField(T object, String expanded);

    /**
     * Returns true if the input field type is a valid field which can be expanded.
     *
     * @param fieldType
     * @return true if the field type is valid
     */
    <T> boolean isValidField(Class<T> fieldType);
  }

  protected Consumer<Matcher.Statistics> statisticsConsumer;
  protected FieldConverter fieldConverter;

  protected AbstractExpander(FieldConverter fieldConverter) {
    this.fieldConverter = fieldConverter;
  }

  public void setStatisticsConsumer(Consumer<Matcher.Statistics> statisticsConsumer) {
    this.statisticsConsumer = statisticsConsumer;
  }

  /**
   * Returns expanded object if property found in the Strings in the object's Fields (except the
   * excluded ones). Returns same object if no expansions occurred.
   */
  public <C extends Cloneable> C expand(C object, Set<String> excludedFieldNames) {
    return expand(new CopyOnWrite.CloneOnWrite<>(object), excludedFieldNames);
  }

  /**
   * Returns expanded object if property found in the Strings in the object's Fields (except the
   * excluded ones). Returns same object if no expansions occurred.
   */
  public <T> T expand(T object, Function<T, T> copier, Set<String> excludedFieldNames) {
    return expand(new CopyOnWrite<>(object, copier), excludedFieldNames);
  }

  /**
   * Returns expanded object if property found in the Strings in the object's Fields (except the
   * excluded ones). Returns same object if no expansions occurred.
   */
  public <T> T expand(CopyOnWrite<T> cow, Set<String> excludedFieldNames) {
    for (Field field : cow.getOriginal().getClass().getFields()) {
      if (!excludedFieldNames.contains(field.getName())) {
        expand(cow, field);
      }
    }
    return cow.getForRead();
  }

  /**
   * Returns expanded object if property found in the fieldName Field if it is a String, or in the
   * List's Strings if it is a List. Returns same object if no expansions occurred.
   */
  public <T> T expand(CopyOnWrite<T> cow, String fieldName) {
    try {
      return expand(cow, cow.getOriginal().getClass().getField(fieldName));
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns expanded object if property found in the Field if it is a String, or in the List's
   * Strings if it is a List. Returns same object if no expansions occurred.
   */
  public <T> T expand(CopyOnWrite<T> cow, Field field) {
    try {
      field.setAccessible(true);
      Object o = field.get(cow.getOriginal());
      if (o instanceof String) {
        String expanded = expandText((String) o);
        if (expanded != o) {
          field.set(cow.getForWrite(), expanded);
        }
      } else if (o instanceof List) {
        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        Class<?> classType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        if (fieldConverter.isValidField(classType)) {
          List<?> expanded = expand((List<?>) o);
          if (expanded != o) {
            field.set(cow.getForWrite(), expanded);
          }
        } else {
          throw new RuntimeException(String.format("Unknown list type: %s", classType));
        }
      }
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
    return cow.getForRead();
  }

  /**
   * Returns expanded unmodifiable List if property found. Returns same object if no expansions
   * occurred.
   */
  public <T> List<T> expand(List<T> list) throws NoSuchFieldException, IllegalAccessException {
    if (list != null) {
      boolean hasProperty = false;
      List<T> expandedList = new ArrayList<>(list.size());
      for (T value : list) {
        T expanded = expand(value);
        boolean hasExpanded = (value != expanded);
        hasProperty = hasProperty || hasExpanded;
        expandedList.add(expanded);
      }
      return hasProperty ? Collections.unmodifiableList(expandedList) : list;
    }
    return null;
  }

  /**
   * Expand all properties (${property_name} -> property_value) in the given field. Returns same
   * object if no expansions occurred.
   */
  public <T> T expand(T value) throws NoSuchFieldException, IllegalAccessException {
    String toExpand = fieldConverter.fromField(value);
    String expanded = expandText(toExpand);
    if (toExpand != expanded) {
      return fieldConverter.toField(value, expanded);
    }
    return value;
  }

  /**
   * Expand all properties (${property_name} -> property_value) in the given text. Returns same
   * object if no expansions occurred.
   */
  public String expandText(String text) {
    if (text == null) {
      return null;
    }
    Matcher m = new Matcher(text);
    m.setStatisticsConsumer(statisticsConsumer);
    if (!m.find()) {
      return text;
    }
    StringBuffer out = new StringBuffer();
    do {
      m.appendValue(out, getValueForName(m.getName()));
    } while (m.find());
    m.appendTail(out);
    return out.toString();
  }

  /**
   * Get the replacement value for the property identified by name
   *
   * @param name of the property to get the replacement value for
   * @return the replacement value. Since the expandText() method alwyas needs a String to replace
   *     '${property-name}' reference with, even when the property does not exist, this will never
   *     return null, instead it will returns the empty string if the property is not found.
   */
  protected abstract String getValueForName(String name);
}
