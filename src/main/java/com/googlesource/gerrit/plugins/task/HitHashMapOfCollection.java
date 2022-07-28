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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class HitHashMapOfCollection<K, V extends Collection<?>> extends HitHashMap<K, V> {
  public static class Statistics extends HitHashMap.Statistics {
    public List<Integer> top5CollectionSizes;
    public List<Integer> bottom5CollectionSizes;
  }

  public static final long serialVersionUID = 1;

  protected Statistics statistics;

  @Override
  public void initStatistics() {
    super.initStatistics();
    statistics = new Statistics();
  }

  @Override
  public Object getStatistics() {
    super.getStatistics();
    statistics.hits = super.statistics.hits;
    statistics.size = super.statistics.size;

    List<Integer> collectionSizes =
        values().stream().map(l -> l.size()).sorted(Comparator.reverseOrder()).collect(toList());
    statistics.top5CollectionSizes = new ArrayList<>(5);
    statistics.bottom5CollectionSizes = new ArrayList<>(5);
    for (int i = 0; i < 5 && i < collectionSizes.size(); i++) {
      statistics.top5CollectionSizes.add(collectionSizes.get(i));
      int bottom = collectionSizes.size() - 6 + i;
      if (bottom > 4 && bottom < collectionSizes.size()) {
        // The > 4 ensures that there are no entries also in the top list
        statistics.bottom5CollectionSizes.add(collectionSizes.get(bottom));
      }
    }
    if (statistics.bottom5CollectionSizes.isEmpty()) {
      statistics.bottom5CollectionSizes = null;
    }
    return statistics;
  }
}
