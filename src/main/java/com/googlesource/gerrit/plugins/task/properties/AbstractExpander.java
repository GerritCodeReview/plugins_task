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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
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
  /** Returns a string which needs to be expanded */
  @Target({METHOD})
  @Retention(RUNTIME)
  public @interface ProvidesUnexpandedString {}

  /** Returns an instance with the input expanded string */
  @Target({METHOD})
  @Retention(RUNTIME)
  public @interface ProvidesInstanceWithExpandedString {}

  protected Consumer<Matcher.Statistics> statisticsConsumer;

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
        List<?> expanded = expand((List<?>) o);
        if (expanded != o) {
          field.set(cow.getForWrite(), expanded);
        }
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return cow.getForRead();
  }

  /**
   * Returns expanded unmodifiable List if property found. Returns same object if no expansions
   * occurred.
   */
  @SuppressWarnings("unchecked")
  protected <T> List<T> expand(List<T> list) {
    if (list != null) {
      if (list.isEmpty()) {
        return list;
      }
      boolean hasProperty = false;
      List<T> expandedList = new ArrayList<>(list.size());
      Class<T> classType = (Class<T>) list.get(0).getClass();
      Function<T, String> getUnexpanded = getUnexpandedStringFunction(classType);
      BiFunction<T, String, T> createInstanceWithExpanded =
          getCreateInstanceWithExpandedFunction(classType);
      for (T value : list) {
        String toExpand = getUnexpanded.apply(value);
        String expanded = expandText(toExpand);
        boolean hasExpanded = toExpand != expanded;
        hasProperty = hasProperty || hasExpanded;
        if (hasExpanded) {
          expandedList.add(createInstanceWithExpanded.apply(value, expanded));
        } else {
          expandedList.add(value);
        }
      }
      return hasProperty ? Collections.unmodifiableList(expandedList) : list;
    }
    return null;
  }

  protected <T> Function<T, String> getUnexpandedStringFunction(Class<T> classType) {
    if (classType == String.class) {
      return (v) -> (String) v;
    }
    for (Method method : classType.getMethods()) {
      if (method.isAnnotationPresent(ProvidesUnexpandedString.class)) {
        checkArgument(
            String.class.isAssignableFrom(method.getReturnType())
                && method.getParameterTypes().length == 0
                && Modifier.isPublic(method.getModifiers()));
        method.setAccessible(true);
        return (v) -> {
          try {
            return (String) method.invoke(v);
          } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Unable to provide unexpanded string");
          }
        };
      }
    }
    throw new RuntimeException("No ProvidesUnexpandedString annotation present");
  }

  @SuppressWarnings("unchecked")
  protected <T> BiFunction<T, String, T> getCreateInstanceWithExpandedFunction(Class<T> classType) {
    if (classType == String.class) {
      return (v, expanded) -> (T) expanded;
    }
    for (Method method : classType.getMethods()) {
      if (method.isAnnotationPresent(ProvidesInstanceWithExpandedString.class)) {
        checkArgument(
            method.getReturnType().isAssignableFrom(classType)
                && method.getParameterTypes().length == 1
                && method.getParameterTypes()[0] == String.class
                && Modifier.isPublic(method.getModifiers()));
        method.setAccessible(true);
        return (v, expanded) -> {
          try {
            return (T) method.invoke(v, expanded);
          } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Unable to provide instance with expanded");
          }
        };
      }
    }
    throw new RuntimeException("No ProvidesInstanceWithExpandedString annotation present");
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
