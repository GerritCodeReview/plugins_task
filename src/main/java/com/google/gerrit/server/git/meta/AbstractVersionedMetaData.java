// Copyright (C) 2013 The Android Open Source Project
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

package com.google.gerrit.server.git.meta;

import com.google.gerrit.entities.BranchNameKey;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;

/** Versioned Configuration file living in git */
public class AbstractVersionedMetaData extends VersionedMetaData {
  protected final BranchNameKey branch;
  protected final String fileName;
  protected Config cfg;

  public AbstractVersionedMetaData(BranchNameKey branch, String fileName) {
    this.branch = branch;
    this.fileName = fileName;
  }

  @Override
  protected String getRefName() {
    return branch.branch();
  }

  protected String getFileName() {
    return fileName;
  }

  @Override
  protected void onLoad() throws IOException, ConfigInvalidException {
    cfg = readConfig(fileName);
  }

  public Config get() {
    if (cfg == null) {
      cfg = new Config();
    }
    return cfg;
  }

  public BranchNameKey getBranch() {
    return branch;
  }

  @Override
  protected boolean onSave(CommitBuilder commit) throws IOException, ConfigInvalidException {
    if (commit.getMessage() == null || "".equals(commit.getMessage())) {
      commit.setMessage("Updated configuration\n");
    }
    saveConfig(fileName, cfg);
    return true;
  }
}
