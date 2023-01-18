// Copyright (C) 2023 The Android Open Source Project
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

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class designed to make SAM calls wrapped by an AutoCloseable. This is usefull with AutoCloseables
 * which do not provide resources which are directly needed during the SAM call, but rather with
 * AutoCloseables which likely manage external resources or state such as a locks or timers.
 */
public abstract class SamTryWrapper<A extends AutoCloseable> {
  protected abstract A getAutoCloseable();

  @SuppressWarnings("try")
  public void run(Runnable runnable) {
    try (A autoCloseable = getAutoCloseable()) {
      runnable.run();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("try")
  public <T> T get(Supplier<T> supplier) {
    try (A autoCloseable = getAutoCloseable()) {
      return supplier.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("try")
  public <T> void accept(Consumer<T> consumer, T t) {
    try (A autoCloseable = getAutoCloseable()) {
      consumer.accept(t);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("try")
  public <T, U> void accept(BiConsumer<T, U> consumer, T t, U u) {
    try (A autoCloseable = getAutoCloseable()) {
      consumer.accept(t, u);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("try")
  public <T, R> R apply(Function<T, R> func, T t) {
    try (A autoCloseable = getAutoCloseable()) {
      return func.apply(t);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("try")
  public <T, U, R> R apply(BiFunction<T, U, R> func, T t, U u) {
    try (A autoCloseable = getAutoCloseable()) {
      return func.apply(t, u);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
