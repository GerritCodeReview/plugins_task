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
  protected StopWatch appendNanoseconds = new StopWatch();
  protected StopWatch findNanoseconds = new StopWatch();

  public Matcher(String text) {
    this.text = text;
  }

  protected void setStatisticsConsumer(Consumer<Statistics> statisticsConsumer) {
    if (statisticsConsumer != null) {
      statistics = new Statistics();
      statisticsConsumer.accept(statistics);
      appendNanoseconds.setConsumer(ns -> statistics.appendNanoseconds = ns);
      findNanoseconds.setConsumer(ns -> statistics.findNanoseconds = ns);
    }
  }

  public boolean find() {
    findNanoseconds.start();
    start = text.indexOf("${", cursor);
    nameStart = start + 2;
    if (start < 0 || text.length() < nameStart + 1) {
      findNanoseconds.stop();
      return false;
    }
    end = text.indexOf('}', nameStart);
    boolean found = end >= 0;
    findNanoseconds.stop();
    return found;
  }

  public String getName() {
    return text.substring(nameStart, end);
  }

  public void appendValue(StringBuffer buffer, String value) {
    appendNanoseconds.start();
    if (start > cursor) {
      buffer.append(text.substring(cursor, start));
    }
    buffer.append(value);
    cursor = end + 1;
    appendNanoseconds.stop();
  }

  public void appendTail(StringBuffer buffer) {
    appendNanoseconds.start();
    if (cursor < text.length()) {
      buffer.append(text.substring(cursor));
      cursor = text.length();
    }
    appendNanoseconds.stop();
  }
}
