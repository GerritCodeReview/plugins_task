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

package com.googlesource.gerrit.plugins.task;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

public class StopWatch {
  protected Stopwatch stopwatch;
  protected LongConsumer consumer;
  protected long nanoseconds;

  public StopWatch enableIfNonNull(Object statistics) {
    if (statistics != null) {
      enable();
    }
    return this;
  }

  public StopWatch enable() {
    stopwatch = Stopwatch.createUnstarted();
    return this;
  }

  public StopWatch run(Runnable runnable) {
    start();
    runnable.run();
    stop();
    return this;
  }

  public StopWatch start() {
    if (stopwatch != null && !stopwatch.isRunning()) {
      stopwatch.start();
    }
    return this;
  }

  public StopWatch stop() {
    if (stopwatch != null && stopwatch.isRunning()) {
      stopwatch.stop();
      if (consumer != null) {
        consume(consumer);
      }
    }
    return this;
  }

  public StopWatch setConsumer(LongConsumer consumer) {
    if (consumer != null) {
      stopwatch = Stopwatch.createUnstarted();
    }
    this.consumer = consumer;
    return this;
  }

  public StopWatch consume(LongConsumer consumer) {
    if (stopwatch != null) {
      consumer.accept(get());
    }
    return this;
  }

  public long get() {
    nanoseconds += stopwatch.elapsed(TimeUnit.NANOSECONDS);
    stopwatch.reset();
    return nanoseconds;
  }
}
