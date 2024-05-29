// Copyright (C) 2020 The Android Open Source Project
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

import com.google.gerrit.entities.Change;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.index.query.Matchable;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.logging.Metadata;
import com.google.gerrit.server.logging.TraceContext;
import com.google.gerrit.server.logging.TraceContext.TraceTimer;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.task.statistics.HitBooleanTable;
import com.googlesource.gerrit.plugins.task.statistics.StopWatch;

public class MatchCache {
  private final String pluginName;

  public interface Factory {
    MatchCache create(@Assisted PredicateCache predicateCache);
  }

  protected final HitBooleanTable<String, Change.Id> resultByChangeByQuery =
      new HitBooleanTable<>();
  protected final PredicateCache predicateCache;

  @Inject
  public MatchCache(@PluginName String pluginName, @Assisted PredicateCache predicateCache) {
    this.pluginName = pluginName;
    this.predicateCache = predicateCache;
  }

  public Boolean matchOrNull(ChangeData changeData, String query, boolean isVisible) {
    try {
      return match(changeData, query, isVisible);
    } catch (StorageException | QueryParseException e) {
    }
    return null;
  }

  @SuppressWarnings("try")
  public boolean match(ChangeData changeData, String query, boolean isVisible)
      throws StorageException, QueryParseException {
    if (query == null || "true".equalsIgnoreCase(query)) {
      return true;
    }
    Boolean isMatched = resultByChangeByQuery.get(query, changeData.getId());
    if (isMatched == null) {
      Matchable<ChangeData> matchable = predicateCache.getPredicate(query, isVisible).asMatchable();
      try (StopWatch stopWatch =
              resultByChangeByQuery.createLoadingStopWatch(query, changeData.getId(), isVisible);
          TraceTimer traceTimer =
              TraceContext.newTimer(
                  query,
                  Metadata.builder()
                      .pluginName(pluginName)
                      .changeId(changeData.getId().get())
                      .className(getClass().getSimpleName())
                      .methodName("match")
                      .build())) {
        isMatched = matchable.match(changeData);
        resultByChangeByQuery.put(query, changeData.getId(), isMatched);
      }
    }
    return isMatched;
  }

  public void initStatistics(int summaryCount) {
    resultByChangeByQuery.initStatistics(summaryCount);
  }

  public Object getStatistics() {
    return resultByChangeByQuery.getStatistics();
  }
}
