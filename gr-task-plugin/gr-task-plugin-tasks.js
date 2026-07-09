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

import {htmlTemplate} from './gr-task-plugin-tasks_html.js';

class GrTaskPluginTasks extends Polymer.Element {
  static get is() {
    return 'gr-task-plugin-tasks';
  }

  static get template() {
    return htmlTemplate;
  }

  static get properties() {
    return {
      tasks: {
        type: Array,
        notify: true,
        value() { return []; },
      },

      show_all: {
        type: String,
        notify: true,
      },

      config: {
        type: Object,
        value() { return {}; },
      },
    };
  }

  _can_show(show, task) {
    return show === 'true' || task.showOnFilter;
  }

  _hasChildren(task) {
    return !!(task.sub_tasks && task.sub_tasks.length);
  }

  _isExpanded(task, expanded) {
    return this._hasChildren(task) && !!expanded;
  }

  _showCaret(task) {
    return this._hasChildren(task);
  }

  _caret(expanded) {
    return expanded ? 'expand_more' : 'chevron_right';
  }

  _toggle(e) {
    e.model.set('task.expanded', !e.model.task.expanded);
  }

  _getChangeUrl(change) {
    return '/c/' + change.toString();
  }
}

customElements.define(GrTaskPluginTasks.is, GrTaskPluginTasks);
