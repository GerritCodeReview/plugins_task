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
 * }} Defs.Task
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

      _isPending: {
        type: Boolean,
        value: true,
      },

      _tasks_info: {
        type: Object,
        observer: '_tasksInfoChanged'
      },
    };
  }

  _is_show_all(show_all) {
    return show_all === 'true';
  }

  ready() {
    super.ready();
    if (!this.change) {
      return;
    }
    document.addEventListener(`response-tasks-${this.change._number}`, e => {
      this._tasks_info = e.detail.tasks_info;
      this._isPending = e.detail.is_loading;
    });
    this._getTasks();
  }

  _tasksInfoChanged(newValue, oldValue) {
    if (this._tasks_info) {
      this._tasks = this._addTasks(this._tasks_info.roots);
    }
  }

  _is_hidden(_isPending, _tasks) {
    return (!_isPending && !_tasks.length);
  }

  async _getTasks() {
    while (this._isPending) {
      document.dispatchEvent(new CustomEvent(`request-tasks-${this.change._number}`, {
        composed: true, bubbles: true,
      }));
      await new Promise(r => setTimeout(r, 100));
    }
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
        icon.id = 'gr-icons:playArrow';
        icon.color = 'green';
        icon.tooltip = 'Ready';
        break;
      case 'INVALID':
        icon.id = 'gr-icons:abandon';
        icon.color = 'red';
        icon.tooltip = 'Invalid';
        break;
      case 'WAITING':
        icon.id = 'gr-icons:pause';
        icon.color = 'orange';
        icon.tooltip = 'Waiting';
        break;
      case 'DUPLICATE':
        icon.id = 'gr-icons:check';
        icon.color = 'green';
        icon.tooltip = 'Duplicate';
        break;
      case 'PASS':
        icon.id = 'gr-icons:check-circle';
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

  _computeShowOnNeededAndBlockedFilter(task) {
    return this._isFailOrReadyOrInvalid(task) ||
      (task.sub_tasks && task.sub_tasks.some(t =>
        this._computeShowOnNeededAndBlockedFilter(t)));
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
      task.showOnFilter = this._computeShowOnNeededAndBlockedFilter(task);
      this._compute_counts(task);
      this._addTasks(task.sub_tasks);
    });
    return tasks;
  }

  _show_all_tap() {
    this._show_all = 'true';
    this._expand_all = 'true';
  }

  _needed_and_blocked_tap() {
    this._show_all = 'false';
    this._expand_all = 'true';
  }

  _switch_expand() {
    this._expand_all = !this._expand_all;
  }

  _computeShowAllLabelText(showAllSections) {
    if (showAllSections) {
      return 'Hide all';
    } else {
      return 'Show all';
    }
  }
}

customElements.define(GrTaskPlugin.is, GrTaskPlugin);
