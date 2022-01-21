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

import './gr-task-plugin-tasks.js';

import {htmlTemplate} from './gr-task-plugin_html.js';

const Defs = {};
/**
 * @typedef {{
 *  sub_tasks: Array<Defs.Task>,
 *  hint: ?string,
 *  name: string,
 *  change: ?number,
 *  status: string
 * }}
 */
Defs.Task;

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
      },

      _show_all: {
        type: String,
        notify: true,
        value: 'false',
      },

      _expand_all: {
        type: Boolean,
        notify: true,
        value: true,
      },

      _all_count: {
        type: Number,
        notify: true,
        value: 0,
      },

      _ready_count: {
        type: Number,
        notify: true,
        value: 0,
      },

      _fail_count: {
        type: Number,
        notify: true,
        value: 0,
      },
    };
  }

  _is_show_all(show_all) {
    return show_all === 'true';
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

  _computeIcon(task) {
    const icon = {};
    switch (task.status) {
      case 'FAIL':
        icon.id = 'gr-icons:close';
        icon.color = 'red';
        icon.tooltip = 'Failed';
        break;
      case 'READY':
        icon.id = 'gr-icons:rebase';
        icon.color = 'green';
        icon.tooltip = 'Ready';
        break;
      case 'INVALID':
        icon.id = 'gr-icons:abandon';
        icon.color = 'red';
        icon.tooltip = 'Invalid';
        break;
      case 'WAITING':
        icon.id = 'gr-icons:side-by-side';
        icon.color = 'red';
        icon.tooltip = 'Waiting';
        break;
      case 'PASS':
        icon.id = 'gr-icons:check';
        icon.color = 'green';
        icon.tooltip = 'Passed';
        break;
    }
    return icon;
  }

  _isFailOrReadyOrInvalid(task) {
    switch (task.status) {
      case 'FAIL':
      case 'READY':
      case 'INVALID':
        return true;
    }
    return false;
  }

  _computeShowOnNeedsAndBlockedFilter(task) {
    return this._isFailOrReadyOrInvalid(task) ||
      (task.sub_tasks && task.sub_tasks.some(t =>
        this._computeShowOnNeedsAndBlockedFilter(t)));
  }

  _compute_counts(task) {
    this._all_count++;
    switch (task.status) {
      case 'FAIL':
        this._fail_count++;
        break;
      case 'READY':
        this._ready_count++;
        break;
    }
  }

  _addTasks(tasks) { // rename to process, remove DOM bits
    if (!tasks) return [];
    tasks.forEach(task => {
      task.icon = this._computeIcon(task);
      task.showOnFilter = this._computeShowOnNeedsAndBlockedFilter(task);
      this._compute_counts(task);
      this._addTasks(task.sub_tasks);
    });
    return tasks;
  }

  _show_all_tap() {
    this._show_all = 'true';
    this._expand_all = 'true';
  }

  _needs_and_blocked_tap() {
    this._show_all = 'false';
    this._expand_all = 'true';
  }

  _switch_expand() {
    this._expand_all = !this._expand_all;
  }
}

customElements.define(GrTaskPlugin.is, GrTaskPlugin);
