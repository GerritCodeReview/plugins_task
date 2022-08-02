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

package com.google.gerrit.common;

import junit.framework.TestCase;

public class BooleanTableTest extends TestCase {

  public void testNulls() {
    BooleanTable<String, String> cbt = new BooleanTable<>();
    assertNull(cbt.get("r1", "c1"));
    assertNull(cbt.get("r0", "c0"));

    cbt.put("r1", "c0", true);
    assertNull(cbt.get("r1", "c1"));
    assertNull(cbt.get("r0", "c0"));

    cbt.put("r0", "c1", true);
    assertNull(cbt.get("r1", "c1"));
    assertNull(cbt.get("r0", "c0"));
  }

  public void testRowColumn() {
    BooleanTable<String, String> cbt = new BooleanTable<>();
    cbt.put("r1", "c1", true);
    cbt.put("r2", "c2", false);
    assertTrue(cbt.get("r1", "c1"));
    assertNull(cbt.get("r1", "c2"));
    assertNull(cbt.get("r2", "c1"));
    assertFalse(cbt.get("r2", "c2"));
  }

  public void testRowColumnOverride() {
    BooleanTable<String, String> cbt = new BooleanTable<>();
    cbt.put("r1", "c1", true);
    assertTrue(cbt.get("r1", "c1"));

    cbt.put("r1", "c1", false);
    assertFalse(cbt.get("r1", "c1"));
  }

  public void testRepeatedColumns() {
    BooleanTable<String, String> cbt = new BooleanTable<>();
    cbt.put("r1", "c1", true);
    cbt.put("r2", "c1", false);
    assertTrue(cbt.get("r1", "c1"));
    assertFalse(cbt.get("r2", "c1"));
  }

  public void testRepeatedRows() {
    BooleanTable<String, String> cbt = new BooleanTable<>();
    cbt.put("r1", "c1", true);
    cbt.put("r1", "c2", false);
    assertTrue(cbt.get("r1", "c1"));
    assertFalse(cbt.get("r1", "c2"));
  }

  public void testRepeatedRowsAndColumns() {
    BooleanTable<String, String> cbt = new BooleanTable<>();
    cbt.put("r1", "c1", true);
    cbt.put("r2", "c1", false);
    cbt.put("r1", "c2", true);
    cbt.put("r2", "c2", false);
    assertTrue(cbt.get("r1", "c1"));
    assertFalse(cbt.get("r2", "c1"));
    assertTrue(cbt.get("r1", "c2"));
    assertFalse(cbt.get("r2", "c2"));
  }
}
