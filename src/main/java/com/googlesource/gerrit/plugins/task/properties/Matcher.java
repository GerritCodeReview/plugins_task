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

import com.googlesource.gerrit.plugins.task.StopWatch;
import java.util.function.Consumer;

/** A handcrafted properties Matcher which has an API similar to an RE Matcher, but is faster. */
public class Matcher {
  public static class Statistics {
    public long appendNanoseconds;
    public long findNanoseconds;

    public Statistics sum(Statistics other) {
      if (other == null) {
        return this;
      }
      Statistics statistics = new Statistics();
      statistics.appendNanoseconds = appendNanoseconds + other.appendNanoseconds;
      statistics.findNanoseconds = findNanoseconds + other.findNanoseconds;
      return statistics;
    }
  }

  protected String text;
  protected int start;
  protected int nameStart;
  protected int end;
  protected int cursor;

  protected Statistics statistics;
  protected StopWatch.Runner appendNanoseconds = new StopWatch.Runner.Disabled();
  protected StopWatch.Runner findNanoseconds = new StopWatch.Runner.Disabled();

  public Matcher(String text) {
    this.text = text;
  }

  protected void setStatisticsConsumer(Consumer<Statistics> statisticsConsumer) {
    if (statisticsConsumer != null) {
      statistics = new Statistics();
      statisticsConsumer.accept(statistics);
      appendNanoseconds =
          new StopWatch.Runner.Enabled().setNanosConsumer(ns -> statistics.appendNanoseconds = ns);
      findNanoseconds =
          new StopWatch.Runner.Enabled().setNanosConsumer(ns -> statistics.findNanoseconds = ns);
    }
  }

  public boolean find() {
    return findNanoseconds.get(() -> findUntimed());
  }

  protected boolean findUntimed() {
    start = text.indexOf("${", cursor);
    nameStart = start + 2;
    if (start < 0 || text.length() < nameStart + 1) {
      return false;
    }
    end = text.indexOf('}', nameStart);
    boolean found = end >= 0;
    return found;
  }

  public String getName() {
    return text.substring(nameStart, end);
  }

  public void appendValue(StringBuffer buffer, String value) {
    appendNanoseconds.accept((b, v) -> appendValueUntimed(b, v), buffer, value);
  }

  protected void appendValueUntimed(StringBuffer buffer, String value) {
    if (start > cursor) {
      buffer.append(text.substring(cursor, start));
    }
    buffer.append(value);
    cursor = end + 1;
  }

  public void appendTail(StringBuffer buffer) {
    appendNanoseconds.accept(b -> appendTailUntimed(b), buffer);
  }

  protected void appendTailUntimed(StringBuffer buffer) {
    if (cursor < text.length()) {
      buffer.append(text.substring(cursor));
      cursor = text.length();
    }
  }
}
