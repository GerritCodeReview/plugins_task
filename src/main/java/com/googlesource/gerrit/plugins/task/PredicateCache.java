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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.index.query.AndPredicate;
import com.google.gerrit.index.query.NotPredicate;
import com.google.gerrit.index.query.OrPredicate;
import com.google.gerrit.index.query.Predicate;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.DestinationPredicate;
import com.google.gerrit.server.query.change.ProjectPredicate;
import com.google.gerrit.server.query.change.RefPredicate;
import com.google.gerrit.server.query.change.RegexProjectPredicate;
import com.google.gerrit.server.query.change.RegexRefPredicate;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.jgit.lib.Config;

public class PredicateCache {
  protected final ChangeQueryBuilder cqb;
  protected final Set<String> cacheableByBranchPredicateClassNames;
  protected final CurrentUser user;

  protected final Map<String, ThrowingProvider<Predicate<ChangeData>, QueryParseException>>
      predicatesByQuery = new HashMap<>();

  @Inject
  public PredicateCache(
      @GerritServerConfig Config config,
      @PluginName String pluginName,
      CurrentUser user,
      ChangeQueryBuilder cqb) {
    this.user = user;
    this.cqb = cqb;
    cacheableByBranchPredicateClassNames =
        new HashSet<>(
            Arrays.asList(
                config.getStringList(pluginName, "cacheable-predicates", "byBranch-className")));
  }

  public boolean match(ChangeData c, String query) throws OrmException, QueryParseException {
    if (query == null) {
      return true;
    }
    return matchWithExceptions(c, query);
  }

  public Boolean matchOrNull(ChangeData c, String query) {
    if (query != null) {
      try {
        return matchWithExceptions(c, query);
      } catch (OrmException | QueryParseException | RuntimeException e) {
      }
    }
    return null;
  }

  protected boolean matchWithExceptions(ChangeData c, String query)
      throws QueryParseException, OrmException {
    if ("true".equalsIgnoreCase(query)) {
      return true;
    }
    return getPredicate(query).asMatchable().match(c);
  }

  protected Predicate<ChangeData> getPredicate(String query) throws QueryParseException {
    ThrowingProvider<Predicate<ChangeData>, QueryParseException> predProvider =
        predicatesByQuery.get(query);
    if (predProvider != null) {
      return predProvider.get();
    }
    // never seen 'query' before
    try {
      Predicate<ChangeData> pred = cqb.parse(query);
      predicatesByQuery.put(query, new ThrowingProvider.Entry<>(pred));
      return pred;
    } catch (QueryParseException e) {
      predicatesByQuery.put(query, new ThrowingProvider.Thrown<>(e));
      throw e;
    }
  }

  /**
   * Can this query's output be assumed to be constant given any Change destined for the same
   * Branch.NameKey?
   */
  public boolean isCacheableByBranch(String query) throws QueryParseException {
    if (query == null
        || "".equals(query)
        || "false".equalsIgnoreCase(query)
        || "true".equalsIgnoreCase(query)) {
      return true;
    }
    return isCacheableByBranch(getPredicate(query));
  }

  protected boolean isCacheableByBranch(Predicate<ChangeData> predicate) {
    if (predicate instanceof AndPredicate
        || predicate instanceof NotPredicate
        || predicate instanceof OrPredicate) {
      for (Predicate<ChangeData> subPred : predicate.getChildren()) {
        if (!isCacheableByBranch(subPred)) {
          return false;
        }
      }
      return true;
    }
    if (predicate instanceof DestinationPredicate
        || predicate instanceof ProjectPredicate
        || predicate instanceof RefPredicate
        || predicate instanceof RegexProjectPredicate
        || predicate instanceof RegexRefPredicate) {
      return true;
    }
    return cacheableByBranchPredicateClassNames.contains(predicate.getClass().getName());
  }
}
