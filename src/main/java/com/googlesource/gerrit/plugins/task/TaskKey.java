// Copyright (C) 2021 The Android Open Source Project
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

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.gerrit.entities.BranchNameKey;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.jgit.errors.ConfigInvalidException;

/** An immutable reference to a task in task config file. */
@AutoValue
public abstract class TaskKey {
  protected static final String CONFIG_SECTION = "task";
  protected static final String CONFIG_TASKS_FACTORY = "tasks-factory";

  /** Creates a TaskKey with task name as the name of sub section. */
  public static TaskKey create(SubSectionKey section) {
    return create(section, section.subSection());
  }

  /** Creates a TaskKey with given FileKey and task name and sub section's name as 'task'. */
  public static TaskKey create(FileKey file, String task) {
    return create(SubSectionKey.create(file, CONFIG_SECTION, task));
  }

  /** Creates a TaskKey from a sub section and task name, generally used by TasksFactory. */
  public static TaskKey create(SubSectionKey section, String task) {
    return new AutoValue_TaskKey(section, task);
  }

  public BranchNameKey branch() {
    return subSection().file().branch();
  }

  public abstract SubSectionKey subSection();

  public abstract String task();

  public boolean isTasksFactoryGenerated() {
    return subSection().section().equals(CONFIG_TASKS_FACTORY);
  }

  public static class Builder {
    protected final FileKey relativeTo;
    protected String file;
    protected String task;

    Builder(FileKey relativeTo) {
      this.relativeTo = relativeTo;
    }

    public TaskKey buildTaskKey() {
      return TaskKey.create(
          isRelativePath() ? relativeTo : FileKey.create(relativeTo.branch(), file), task);
    }

    public void setAbsolute() {
      file = TaskFileConstants.TASK_DIR;
    }

    public void setPath(Path path) throws ConfigInvalidException {
      Path parentDir = Paths.get(relativeTo.file()).getParent();
      if (parentDir == null) {
        parentDir = Paths.get(TaskFileConstants.TASK_DIR);
      }

      file =
          isRelativePath()
              ? parentDir.resolve(path).toString()
              : Paths.get(file).resolve(path).toString();
      throwIfInvalidPath();
    }

    public void setRefRootFile() throws ConfigInvalidException {
      Preconditions.checkState(!isFileAlreadySet());
      file = TaskFileConstants.TASK_CFG;
    }

    public void setTaskName(String task) {
      this.task = task;
    }

    protected void throwIfInvalidPath() throws ConfigInvalidException {
      Path path = Paths.get(file);
      if (!path.startsWith(TaskFileConstants.TASK_DIR)
          && !path.equals(Paths.get(TaskFileConstants.TASK_CFG))) {
        throw new ConfigInvalidException(
            "Invalid config location, path should be "
                + TaskFileConstants.TASK_CFG
                + " or under "
                + TaskFileConstants.TASK_DIR
                + " directory");
      }
    }

    /** Returns true when the path implies relative or same file. */
    protected boolean isRelativePath() {
      return file == null;
    }

    protected boolean isFileAlreadySet() {
      return file != null;
    }
  }
}
