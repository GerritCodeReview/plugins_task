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

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * A space efficient Table for Booleans. This Table takes advantage of the fact that the values
 * stored in it are all Booleans and uses BitSets to make this very space efficient.
 */
public class BooleanTable<R, C> {
  protected class Row {
    public final BitSet hasValues = new BitSet();
    public final BitSet values = new BitSet();

    public void setPosition(int position, Boolean value) {
      if (value != null) {
        values.set(position, value);
      }
      hasValues.set(position, value != null);
    }

    public Boolean getPosition(int position) {
      if (hasValues.get(position)) {
        return values.get(position);
      }
      return null;
    }
  }

  protected Map<R, Row> rowByRow = new HashMap<>();
  protected Map<C, Integer> positionByColumn = new HashMap<>();
  protected int highestPosition = -1;

  public void put(R r, C c, Boolean v) {
    Row row = rowByRow.computeIfAbsent(r, k -> new Row());
    Integer columnPosition = positionByColumn.computeIfAbsent(c, k -> nextPosition());
    row.setPosition(columnPosition, v);
  }

  protected int nextPosition() {
    return ++highestPosition;
  }

  public Boolean get(R r, C c) {
    Row row = rowByRow.get(r);
    if (row != null) {
      Integer columnPosition = positionByColumn.get(c);
      if (columnPosition != null) {
        return row.getPosition(columnPosition);
      }
    }
    return null;
  }
}
