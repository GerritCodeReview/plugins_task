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

/** A handcrafted properties Matcher which has an API similar to an RE Matcher, but is faster. */
public class Matcher {
  String text;
  int start;
  int nameStart;
  int end;
  int cursor;

  public Matcher(String text) {
    this.text = text;
  }

  public boolean find() {
    start = text.indexOf("${", cursor);
    nameStart = start + 2;
    if (start < 0 || text.length() < nameStart + 1) {
      return false;
    }
    end = text.indexOf('}', nameStart);
    return end >= 0;
  }

  public String getName() {
    return text.substring(nameStart, end);
  }

  public void appendValue(StringBuffer buffer, String value) {
    if (start > cursor) {
      buffer.append(text.substring(cursor, start));
    }
    buffer.append(value);
    cursor = end + 1;
  }

  public void appendTail(StringBuffer buffer) {
    if (cursor < text.length()) {
      buffer.append(text.substring(cursor));
      cursor = text.length();
    }
  }
}
