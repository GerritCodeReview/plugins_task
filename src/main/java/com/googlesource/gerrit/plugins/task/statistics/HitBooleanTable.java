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

import com.google.gerrit.common.BooleanTable;
import com.googlesource.gerrit.plugins.task.util.TopKeyMap;

/**
 * A space efficient Table for Booleans. This Table takes advantage of the fact that the values
 * stored in it are all Booleans and uses BitSets to make this very space efficient.
 */
public class HitBooleanTable<R, C> extends BooleanTable<R, C> implements TracksStatistics {
  public static class Statistics<V> {
    public long hits;
    public long misses;
    public long size;
    public int numberOfRows;
    public int numberOfColumns;
    public Long sumNanosecondsLoading;
    public TopKeyMap<V> topNanosecondsLoadingKeys;
  }

  protected Statistics<TopKeyMap.TableKeyValue<R, C>> statistics;

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

  public StopWatch createLoadingStopWatch(R row, C column, boolean isVisible) {
    if (statistics == null) {
      return new StopWatch.Disabled();
    }
    if (statistics.sumNanosecondsLoading == null) {
      statistics.sumNanosecondsLoading = 0L;
    }
    return new StopWatch.Enabled()
        .setNanosConsumer(
            ns ->
                statistics.sumNanosecondsLoading +=
                    updateTopLoadingTimes(ns, row, column, isVisible));
  }

  public long updateTopLoadingTimes(long nanos, R row, C column, boolean isVisible) {
    statistics.topNanosecondsLoadingKeys.addIfTop(
        nanos, isVisible ? new TopKeyMap.TableKeyValue<R, C>(row, column) : null);
    return nanos;
  }

  @Override
  public void initStatistics(int summaryCount) {
    statistics = new Statistics<>();
    statistics.topNanosecondsLoadingKeys = new TopKeyMap<>(summaryCount);
  }

  @Override
  public void ensureStatistics(int summaryCount) {
    if (statistics == null) {
      initStatistics(summaryCount);
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
