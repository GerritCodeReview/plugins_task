// Copyright (C) 2015 The Android Open Source Project
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

package com.google.gerrit.server.util;

import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.NoSuchRefException;
import com.google.inject.Inject;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Git {
  public final GitRepositoryManager repos;

  @Inject
  public Git(GitRepositoryManager repos) {
    this.repos = repos;
  }

  public ObjectId getObjectId(Branch.NameKey branch) throws IOException,
      NoSuchRefException, RepositoryNotFoundException {
    Repository repo = repos.openRepository(branch.getParentKey());
    try {
      return repo.getRef(branch.get()).getObjectId();
    } finally {
      repo.close();
    }
  }

  public Map<Branch.NameKey, ObjectId> getObjectIdsByBranch(
      Iterable<Branch.NameKey> branches) throws IOException,
      NoSuchRefException, RepositoryNotFoundException {
    Map<Branch.NameKey, ObjectId> idsByBranch =
        new HashMap<Branch.NameKey, ObjectId>();
    for (Branch.NameKey branch : branches) {
      idsByBranch.put(branch, getObjectId(branch));
    }
    return idsByBranch;
  }
}
