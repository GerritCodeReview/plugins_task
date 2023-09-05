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

package com.googlesource.gerrit.plugins.task;

import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.account.Accounts;
import com.google.gerrit.server.config.AllUsersNameProvider;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;

public class TaskPath {
  public interface Factory {
    TaskPath create(TaskKey key);
  }

  protected String name;
  protected String type;
  protected String tasksFactory;
  protected String user;
  protected String project;
  protected String ref;
  protected String file;
  protected String error;

  @Inject
  public TaskPath(AllUsersNameProvider allUsers, Accounts accounts, @Assisted TaskKey key) {
    name = key.task();
    type = key.subSection().section();
    tasksFactory = key.isTasksFactoryGenerated() ? key.subSection().subSection() : null;
    user = getUserOrNull(accounts, allUsers.get(), key);
    project = key.branch().project().get();
    ref = key.branch().branch();
    file = key.subSection().file().file();
  }

  public TaskPath(String error) {
    this.error = error;
  }

  private String getUserOrNull(Accounts accounts, Project.NameKey allUsers, TaskKey key) {
    try {
      if (allUsers.get().equals(key.branch().project().get())) {
        String ref = key.branch().branch();
        Account.Id id = Account.Id.fromRef(ref);
        if (id != null) {
          Optional<AccountState> state = accounts.get(id);
          if (state.isPresent()) {
            Optional<String> userName = state.get().userName();
            if (userName.isPresent()) {
              return userName.get();
            }
          }
        }
      }
    } catch (ConfigInvalidException | IOException e) {
    }
    return null;
  }
}
