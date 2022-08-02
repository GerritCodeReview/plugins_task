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
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.query.change.ChangeData;

public class MatchCache {
  protected final HitBooleanTable<String, Change.Id> resultByChangeByQuery =
      new HitBooleanTable<>();
  protected final PredicateCache predicateCache;

  public MatchCache(PredicateCache predicateCache) {
    this.predicateCache = predicateCache;
  }

  public boolean match(ChangeData changeData, String query)
      throws StorageException, QueryParseException {
    if (query == null) {
      return true;
    }
    Boolean isMatched = resultByChangeByQuery.get(query, changeData.getId());
    if (isMatched == null) {
      isMatched = predicateCache.matchWithExceptions(changeData, query);
      resultByChangeByQuery.put(query, changeData.getId(), isMatched);
    }
    return isMatched;
  }

  public Boolean matchOrNull(ChangeData changeData, String query) {
    if (query == null) {
      return null;
    }
    Boolean isMatched = resultByChangeByQuery.get(query, changeData.getId());
    if (isMatched == null) {
      try {
        isMatched = predicateCache.matchWithExceptions(changeData, query);
      } catch (QueryParseException | RuntimeException e) {
      }
      resultByChangeByQuery.put(query, changeData.getId(), isMatched);
    }
    return isMatched;
  }

  public void initStatistics() {
    resultByChangeByQuery.initStatistics();
  }

  public Object getStatistics() {
    return resultByChangeByQuery.getStatistics();
  }
}
