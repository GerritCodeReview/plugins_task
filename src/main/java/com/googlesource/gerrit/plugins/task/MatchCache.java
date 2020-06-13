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

import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gwtorm.server.OrmException;
import java.util.HashMap;
import java.util.Map;

public class MatchCache {
  protected final PredicateCache predicateCache;
  protected final ChangeData changeData;

  protected final Map<String, Boolean> matchResultByQuery = new HashMap<>();

  public MatchCache(PredicateCache predicateCache, ChangeData changeData) {
    this.predicateCache = predicateCache;
    this.changeData = changeData;
  }

  protected boolean match(String query) throws OrmException, QueryParseException {
    if (query == null) {
      return true;
    }
    Boolean isMatched = matchResultByQuery.get(query);
    if (isMatched == null) {
      isMatched = predicateCache.match(changeData, query);
      matchResultByQuery.put(query, isMatched);
    }
    return isMatched;
  }

  protected Boolean matchOrNull(String query) {
    if (query == null) {
      return null;
    }
    Boolean isMatched = matchResultByQuery.get(query);
    if (isMatched == null) {
      isMatched = predicateCache.matchOrNull(changeData, query);
      matchResultByQuery.put(query, isMatched);
    }
    return isMatched;
  }
}
