// Copyright (C) 2023 The Android Open Source Project
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

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.index.query.Predicate;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.SubmitRequirementPredicate;
import com.google.inject.AbstractModule;

// TODO: Remove this class when up-merging to Gerrit v3.6+ as it supports
//       the 'is:true' submit requirement
public class IsTrueOperator implements ChangeQueryBuilder.ChangeIsOperandFactory {

  public static final String TRUE = "true";

  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      bind(ChangeQueryBuilder.ChangeIsOperandFactory.class)
          .annotatedWith(Exports.named(TRUE))
          .to(IsTrueOperator.class);
    }
  }

  public class TruePredicate extends SubmitRequirementPredicate {

    public TruePredicate() {
      super("is", TRUE);
    }

    @Override
    public boolean match(ChangeData data) {
      return true;
    }

    @Override
    public int getCost() {
      return 1;
    }
  }

  @Override
  public Predicate<ChangeData> create(ChangeQueryBuilder builder) throws QueryParseException {
    return new IsTrueOperator.TruePredicate();
  }
}
