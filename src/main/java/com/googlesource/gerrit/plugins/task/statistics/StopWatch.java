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

package com.googlesource.gerrit.plugins.task.statistics;

import com.google.common.base.Stopwatch;
import com.googlesource.gerrit.plugins.task.util.SamTryWrapper;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

/**
 * StopWatches with APIs designed to make it easy to disable, and to make robust in the face of
 * exceptions.
 *
 * <p>The Stopwatch class from google commons is used by placing start() and stop() calls around
 * code sections which need to be timed. This approach can be problematic since the code being timed
 * could throw an exception and if the stop() is not in a finally clause, then it will likely never
 * get called, potentially causing bad timings, or worse programatic issues elsewhere due to double
 * calls to start(). The need for a finally clause to make things safe is an obvious hint that using
 * an AutoCloseable approach is likely going to be safer. With that in mind, the two API approaches
 * provided by these StopWatch classes are:
 *
 * <ol>
 *   <li>The timed try-with-resource API. Use the StopWatch.Enabled class for this API.
 *   <li>The timed SAM evaluation API. Use the StopWatch.Runner.Enabled class for these APIs.
 * </ol>
 *
 * <p>Finally, the commons stopwatch API also does not provide an easy way to disable timings at
 * runtime when they are not desired, so the DISABLED classes can be used for this with both
 * approaches above, and thus provide low cost runtime substitutes for either the StopWatch.Enabled
 * or StopWatch.Runner.Enabled classes.
 */
public interface StopWatch extends AutoCloseable {
  /** Designed for the greatest simplicity to time SAM executions. */
  public abstract static class Runner extends SamTryWrapper<AutoCloseable> {
    public static class Enabled extends Runner {
      protected LongConsumer nanosConsumer = EMPTY_LONG_CONSUMER;

      @Override
      protected AutoCloseable getAutoCloseable() {
        return new StopWatch.Enabled().setNanosConsumer(nanosConsumer);
      }

      @Override
      public Runner setNanosConsumer(LongConsumer nanosConsumer) {
        this.nanosConsumer = nanosConsumer;
        return this;
      }
    }

    /** May be used anywhere that Enabled can be used */
    public static final Runner DISABLED =
        new Runner() {
          @Override
          protected AutoCloseable getAutoCloseable() {
            return () -> {};
          }
        };

    public Runner setNanosConsumer(LongConsumer nanosConsumer) {
      return this;
    }
  }

  /** Should be created and used only within a try-with-resource */
  public static class Enabled implements StopWatch {
    protected LongConsumer nanosConsumer = EMPTY_LONG_CONSUMER;
    protected Stopwatch stopwatch = Stopwatch.createStarted();

    @Override
    public StopWatch setNanosConsumer(LongConsumer nanosConsumer) {
      this.nanosConsumer = nanosConsumer;
      return this;
    }

    @Override
    public void close() {
      stopwatch.stop();
      nanosConsumer.accept(stopwatch.elapsed(TimeUnit.NANOSECONDS));
    }
  }

  /**
   * A easy way to build a timer which needes to be enabled/disabled based on a runtime boolean.
   *
   * <p>Example Usage:
   *
   * <p><code>
   * try (StopWatch stopWatch =
   *      StopWatch.builder().enabled(myBoolean).build().setNanosConsumer(myConsumer)) {
   *   // Code to be timed here...
   * }
   * </code>
   */
  public static class Builder {
    protected static class Enabled extends Builder {
      @Override
      public StopWatch build() {
        return new StopWatch.Enabled();
      }
    }

    protected static final Builder ENABLED = new Enabled();
    protected static final Builder DISABLED = new Builder();

    public Builder enabled(boolean enabled) {
      return enabled ? ENABLED : this;
    }

    public StopWatch build() {
      return StopWatch.DISABLED;
    }
  }

  /** May be used anywhere that Enabled can be used */
  public static final StopWatch DISABLED = new StopWatch() {};

  public static final LongConsumer EMPTY_LONG_CONSUMER = l -> {};

  public static Builder builder() {
    return Builder.DISABLED;
  }

  default StopWatch setNanosConsumer(LongConsumer nanosConsumer) {
    return this;
  }

  default void close() {}
}
