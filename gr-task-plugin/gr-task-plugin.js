// Copyright (C) 2019 The Android Open Source Project
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
(function() {
  'use strict';

  Polymer({
    is: 'gr-task-plugin',
    properties: {
      hasTasks: {
        type: Boolean,
        value: false,
      },
      change: {
        type: Object,
      },
    },

    attached() {
      this._getTasks();
      this._tasksFragment = document.createDocumentFragment();
    },

    _getTasks() {
      const endpoint =
          `/changes/?q=change:${this.change._number}&--task--applicable`;

      return this.plugin.restApi().get(endpoint).then(response => {
        if (response && response.length === 1) {
          const cinfo = response[0];
          if (cinfo.plugins) {
            const taskPluginInfo = cinfo.plugins.find(
                pluginInfo => pluginInfo.name === 'task');

            if (taskPluginInfo) {
              this.tasks = taskPluginInfo.roots;
              this._addTasks(this.tasks);
              if (this.hasTasks) {
                this.$.tasks.appendChild(this._tasksFragment);
              }
            }
          }
        }
      });
    },

    _computeMessage(task) {
      switch (task.status) {
        case 'FAIL':
        case 'READY':
        case 'INVALID':
          return this._getTaskDescription(task);
      }
    },

    _getTaskDescription(task) {
      return task.hint || task.name;
    },

    _addTasks(tasks) {
      if (!tasks) return;
      tasks.forEach(task => {
        const message = this._computeMessage(task);
        if (message) {
          this.hasTasks = true;
          const taskMessage = document.createElement('div');
          taskMessage.appendChild(document.createTextNode(message));
          this._tasksFragment.appendChild(taskMessage);
        }
        this._addTasks(task.sub_tasks);
      });
    },
  });
})();
