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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

/** This class is used by TaskExpression to decode the task from task reference. */
public class TaskReference {
  protected FileKey currentFile;
  protected String reference;

  public TaskReference(FileKey originalFile, String reference) {
    currentFile = originalFile;
    this.reference = reference.trim();
    if (reference.isEmpty()) {
      throw new NoSuchElementException();
    }
  }

  public TaskKey getTaskKey() {
    String[] referenceSplit = reference.split("\\^");
    switch (referenceSplit.length) {
      case 1:
        return TaskKey.create(currentFile, referenceSplit[0]);
      case 2:
        return TaskKey.create(getFileKey(referenceSplit[0]), referenceSplit[1]);
      default:
        throw new NoSuchElementException();
    }
  }

  protected FileKey getFileKey(String referenceFile) {
    return FileKey.create(currentFile.branch(), getFile(referenceFile));
  }

  protected String getFile(String referencedFile) {
    if (referencedFile.isEmpty()) { // Implies a task from root task.config
      return TaskFileConstants.TASK_CFG;
    }

    if (referencedFile.startsWith("/")) { // Implies absolute path to the config is provided
      return Paths.get(TaskFileConstants.TASK_DIR, referencedFile).toString();
    }

    // Implies a relative path to sub-directory
    Path dir = Paths.get(currentFile.file()).getParent();
    if (dir == null) { // Relative path in root task.config should refer to files under task dir
      return Paths.get(TaskFileConstants.TASK_DIR, referencedFile).toString();
    } else {
      return Paths.get(dir.toString(), referencedFile).toString();
    }
  }
}
