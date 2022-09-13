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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public class CopyOnWrite<T> {
  public static class CloneOnWrite<C extends Cloneable> extends CopyOnWrite<C> {
    public CloneOnWrite(C cloneable) {
      super(cloneable, copier(cloneable));
    }
  }

  public static <C extends Cloneable> Function<C, C> copier(C cloneable) {
    return c -> clone(c);
  }

  @SuppressWarnings("unchecked")
  public static <C extends Cloneable> C clone(C cloneable) {
    try {
      for (Class<?> cls = cloneable.getClass(); cls != null; cls = cls.getSuperclass()) {
        Optional<Method> optional = getOptionalDeclaredMethod(cls, "clone");
        if (optional.isPresent()) {
          Method clone = optional.get();
          clone.setAccessible(true);
          return (C) cloneable.getClass().cast(clone.invoke(cloneable));
        }
      }
      throw new RuntimeException("Cannot find clone() method");
    } catch (SecurityException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * A faster getDeclaredMethod() without exceptions. The original apparently does a linear search
   * anyway, and it is significantly slower when it throws NoSuchMethodExceptions.
   */
  public static Optional<Method> getOptionalDeclaredMethod(
      Class<?> cls, String name, Class<?>... parameterTypes) {
    for (Method method : cls.getDeclaredMethods()) {
      if (method.getName().equals(name)
          && Arrays.equals(method.getParameterTypes(), parameterTypes)) {
        return Optional.of(method);
      }
    }
    return Optional.empty();
  }

  protected Function<T, T> copier;
  protected T original;
  protected T copy;

  public CopyOnWrite(T original, Function<T, T> copier) {
    this.original = original;
    this.copier = copier;
  }

  public T getOriginal() {
    return original;
  }

  public T getForRead() {
    return copy != null ? copy : original;
  }

  public T getForWrite() {
    if (copy == null) {
      copy = copier.apply(original);
    }
    return copy;
  }
}
