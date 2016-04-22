// Copyright (C) 2016 The Android Open Source Project
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

import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.server.git.meta.AbstractVersionedMetaData;
import java.util.ArrayList;
import java.util.List;

/** Task Configuration file living in git */
public class TaskConfig extends AbstractVersionedMetaData {
  protected static final String SECTION_ROOT = "root";
  protected static final String KEY_APPLICABLE = "applicable";
  protected static final String KEY_NAME = "name";

  public TaskConfig(Branch.NameKey branch, String fileName) {
    super(branch, fileName);
  }

  public List<TaskDefinition> getRootTaskDefinitions() {
    List<TaskDefinition> roots = new ArrayList<TaskDefinition>();
    // No need to get a root with no name (what would we call it?)
    for (String root : cfg.getSubsections(SECTION_ROOT)) {
      roots.add(getRootDefinition(root));
    }
    return roots;
  }

  protected TaskDefinition getRootDefinition(String name) {
    Section s = new Section(SECTION_ROOT, name);
    TaskDefinition root = new TaskDefinition();
    root.applicable = getString(s, KEY_APPLICABLE, null);
    root.name = getString(s, KEY_NAME, name);
    return root;
  }

  protected String getString(Section s, String key, String def) {
    String v = cfg.getString(s.section, s.subSection, key);
    return v != null ? v : def;
  }

  protected static class Section {
    public final String section;
    public final String subSection;

    protected Section(String section, String subSection) {
      this.section = section;
      this.subSection = subSection;
    }
  }
}
