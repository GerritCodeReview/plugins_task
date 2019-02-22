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

  const Defs = {};
  /**
   * @typedef {{
   *  message: string,
   *  sub_tasks: Array<Defs.Task>,
   *  hint?: string,
   *  name: string,
   *  status: string
   * }}
   */
  Defs.Task;

  Polymer({
    is: 'gr-task-plugin',
    properties: {
      change: {
        type: Object,
      },

      // @type {Array<Defs.Task>}
      _tasks: {
        type: Array,
        notify: true,
        value() { return []; },
      },
    },

    attached() {
      this._getTasks();
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
              this._tasks = this._addTasks(taskPluginInfo.roots);
            }
          }
        }
      });
    },

    _getTaskDescription(task) {
      return task.hint || task.name;
    },

    _computeMessage(task) {
      switch (task.status) {
        case 'FAIL':
        case 'READY':
        case 'INVALID':
          return this._getTaskDescription(task);
      }
    },

    _addTasks(tasks) { // rename to process, remove DOM bits
      if (!tasks) return [];
      tasks.forEach(task => {
        task.message = this._computeMessage(task);
        this._addTasks(task.sub_tasks);
      });
      return tasks;
    },
  });
})();
