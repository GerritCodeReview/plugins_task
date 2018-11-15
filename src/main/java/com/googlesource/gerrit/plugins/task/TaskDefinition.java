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
import java.util.List;
import java.util.Objects;

public class TaskDefinition {
  public Branch.NameKey branch;
  public String fileName;

  public String applicable;
  public String fail;
  public String name;
  public String pass;
  public List<String> subTasks;

  public TaskDefinition(Branch.NameKey branch, String fileName) {
    this.branch = branch;
    this.fileName = fileName;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o == null || !(o instanceof TaskDefinition)) {
      return false;
    }
    TaskDefinition t = (TaskDefinition) o;
    return Objects.equals(branch, t.branch)
        && Objects.equals(fileName, t.fileName)
        && Objects.equals(applicable, t.applicable)
        && Objects.equals(fail, t.fail)
        && Objects.equals(name, t.name)
        && Objects.equals(pass, t.pass)
        && Objects.equals(subTasks, t.subTasks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(branch, fileName, applicable, fail, name, pass, subTasks);
  }
}
