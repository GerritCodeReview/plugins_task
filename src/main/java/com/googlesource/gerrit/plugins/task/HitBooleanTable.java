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

import com.google.gerrit.common.BooleanTable;

/**
 * A space efficient Table for Booleans. This Table takes advantage of the fact that the values
 * stored in it are all Booleans and uses BitSets to make this very space efficient.
 */
public class HitBooleanTable<R, C> extends BooleanTable<R, C> implements TracksStatistics {
  public static class Statistics {
    public long hits;
    public long misses;
    public long size;
    public int numberOfRows;
    public int numberOfColumns;
    public long sumNanosecondsLoading;
  }

  protected Statistics statistics;

  @Override
  public Boolean get(R r, C c) {
    Boolean value = super.get(r, c);
    if (statistics != null) {
      if (value != null) {
        statistics.hits++;
      } else {
        statistics.misses++;
      }
    }
    return value;
  }

  public StopWatch createLoadingStopWatch() {
    if (statistics == null) {
      return new StopWatch.Disabled();
    }
    return new StopWatch.Enabled().setNanosConsumer(ns -> statistics.sumNanosecondsLoading += ns);
  }

  @Override
  public void initStatistics() {
    statistics = new Statistics();
  }

  @Override
  public void ensureStatistics() {
    if (statistics == null) {
      initStatistics();
    }
  }

  @Override
  public Object getStatistics() {
    statistics.numberOfRows = rowByRow.size();
    statistics.numberOfColumns = positionByColumn.size();
    statistics.size =
        rowByRow.values().stream()
            .map(r -> (long) r.hasValues.size() + (long) r.values.size())
            .mapToLong(Long::longValue)
            .sum();
    return statistics;
  }
}
