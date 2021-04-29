(function () {
  'use strict';

  /**
   * @license
   * Copyright (C) 2021 The Android Open Source Project
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   * http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */

  const htmlTemplate$1 = Polymer.html`
<template is="dom-repeat" as="task" items="[[tasks]]">
  <template is="dom-if" if="[[task.message]]">
    <li>[[task.message]]</li>
  </template>
  <gr-task-plugin-tasks tasks="[[task.sub_tasks]]"></gr-task-plugin-tasks>
</template>`;

  // Copyright (C) 2021 The Android Open Source Project

  class GrTaskPluginTasks extends Polymer.Element {
    static get is() {
      return 'gr-task-plugin-tasks';
    }

    static get template() {
      return htmlTemplate$1;
    }

    static get properties() {
      return {
        tasks: {
          type: Array,
          notify: true,
          value() { return []; },
        }
      };
    }
  }

  customElements.define(GrTaskPluginTasks.is, GrTaskPluginTasks);

  /**
   * @license
   * Copyright (C) 2021 The Android Open Source Project
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   * http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */

  const htmlTemplate = Polymer.html`
<style>
  ul { padding-left: 30px; }
  h3 { padding-left: 5px; }
</style>

<div id="tasks" hidden$="[[!_tasks.length]]">
  <h3>Tasks: (Needs + Blocked)</h3>
  <ul>
    <gr-task-plugin-tasks tasks="[[_tasks]]"></gr-task-plugin-tasks>
  </ul>
</div>`;

  // Copyright (C) 2021 The Android Open Source Project

  class GrTaskPlugin extends Polymer.Element {
    static get is() {
      return 'gr-task-plugin';
    }

    static get template() {
      return htmlTemplate;
    }

    static get properties() {
      return {
        change: {
          type: Object,
        },

        // @type {Array<Defs.Task>}
        _tasks: {
          type: Array,
          notify: true,
          value() { return []; },
        }
      };
    }

    connectedCallback() {
      super.connectedCallback();

      this._getTasks();
    }

    _getTasks() {
      if (!this.change) {
        return;
      }

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
    }

    _getTaskDescription(task) {
      return task.hint || task.name;
    }

    _computeMessage(task) {
      if (!task) return '';
      switch (task.status) {
        case 'FAIL':
        case 'READY':
        case 'INVALID':
          return this._getTaskDescription(task);
      }
    }

    _addTasks(tasks) { // rename to process, remove DOM bits
      if (!tasks) return [];
      tasks.forEach(task => {
        task.message = this._computeMessage(task);
        this._addTasks(task.sub_tasks);
      });
      return tasks;
    }
  }

  customElements.define(GrTaskPlugin.is, GrTaskPlugin);

  /**
   * @license
   * Copyright (C) 2021 The Android Open Source Project
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   * http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */

  Gerrit.install(plugin => {
    plugin.registerCustomComponent(
      'change-view-integration', 'gr-task-plugin');
  });

}());
