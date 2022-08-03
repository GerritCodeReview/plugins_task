// Copyright (C) 2022 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.task.properties;

import com.google.gerrit.entities.Change;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.server.query.change.ChangeData;
import com.googlesource.gerrit.plugins.task.TaskConfig.Task;
import java.util.function.Function;

public class Loader {
  protected final Task task;
  protected final ChangeData changeData;
  protected final Function<String, String> inherritedMapper;
  protected Change change;
  protected boolean isInheritedPropertyLoaded;

  public Loader(Task task, ChangeData changeData, Function<String, String> inherritedMapper) {
    this.task = task;
    this.changeData = changeData;
    this.inherritedMapper = inherritedMapper;
  }

  public boolean isNonTaskDefinedPropertyLoaded() {
    return change != null || isInheritedPropertyLoaded;
  }

  public String load(String name) throws StorageException {
    if (name.startsWith("_")) {
      return internal(name);
    }
    String value = task.exported.get(name);
    if (value == null) {
      value = task.properties.get(name);
      if (value == null) {
        value = inherritedMapper.apply(name);
        if (!value.isEmpty()) {
          isInheritedPropertyLoaded = true;
        }
      }
    }
    return value;
  }

  protected String internal(String name) throws StorageException {
    if ("_name".equals(name)) {
      return task.name();
    }
    String changeProp = name.replace("_change_", "");
    if (changeProp != name) {
      return change(changeProp);
    }
    return "";
  }

  protected String change(String changeProp) throws StorageException {
    switch (changeProp) {
      case "number":
        return String.valueOf(change().getId().get());
      case "id":
        return change().getKey().get();
      case "project":
        return change().getProject().get();
      case "branch":
        return change().getDest().branch();
      case "status":
        return change().getStatus().toString();
      case "topic":
        return change().getTopic();
      default:
        return "";
    }
  }

  protected Change change() {
    if (change == null) {
      change = changeData.change();
    }
    return change;
  }
}
