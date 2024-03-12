/**
 * @license
 * Copyright (C) 2023 The Android Open Source Project
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

import {htmlTemplate} from './gr-task-summary_html.js';
import {GrTaskChip} from './gr-task-chip.js';

class GrTaskSummary extends Polymer.Element {
  static get is() {
    return 'gr-task-summary';
  }

  static get template() {
    return htmlTemplate;
  }

  static get properties() {
    return {
      change: {
        type: Object,
      },

      ready_count: {
        type: Number,
        notify: true,
        value: 0,
      },

      fail_count: {
        type: Number,
        notify: true,
        value: 0,
      },

      invalid_count: {
        type: Number,
        notify: true,
        value: 0,
      },

      waiting_count: {
        type: Number,
        notify: true,
        value: 0,
      },

      duplicate_count: {
        type: Number,
        notify: true,
        value: 0,
      },

      pass_count: {
        type: Number,
        notify: true,
        value: 0,
      },

      is_loading: {
        type: Boolean,
        value: true,
      },

      tasks_info: {
        type: Object,
      },
    };
  }

  /** @override */
  ready() {
    super.ready();
    this._fetch_tasks();

    document.addEventListener(`request-tasks-${this.change._number}`, e => {
      document.dispatchEvent(
          new CustomEvent(`response-tasks-${this.change._number}`, {
            detail: {
              tasks_info: this.tasks_info,
              is_loading: this.is_loading,
            },
            composed: true, bubbles: true,
          }));
    });
  }

  _fetch_tasks() {
    const endpoint =
        `/changes/?q=change:${this.change._number}&--task--applicable`;
    return this.plugin.restApi().get(endpoint).then(response => {
      if (response && response.length === 1) {
        const cinfo = response[0];
        if (cinfo.plugins) {
          this.tasks_info = cinfo.plugins.find(
              pluginInfo => pluginInfo.name === 'task');
          this._compute_counts(this.tasks_info.roots);
        }
      }
    }).finally(e => {
      this.is_loading = false;
      if (!this._can_show_summary(
          this.is_loading, this.ready_count,
          this.fail_count, this.invalid_count,
          this.waiting_count, this.duplicate_count,
          this.pass_count)) {
        this._hide_tasks_tab();
      }
    });
  }

  _compute_counts(tasks) {
    if (!tasks) return [];
    tasks.forEach(task => {
      switch (task.status) {
        case 'FAIL':
          this.fail_count++;
          break;
        case 'READY':
          this.ready_count++;
          break;
        case 'INVALID':
          this.invalid_count++;
          break;
        case 'WAITING':
          this.waiting_count++;
          break;
        case 'DUPLICATE':
          this.duplicate_count++;
          break;
        case 'PASS':
          this.pass_count++;
          break;
      }
      this._compute_counts(task.sub_tasks);
    });
  }

  _hide_tasks_tab() {
    const paperTabs = GrTaskChip.getPrimaryTabs();
    const tabs = paperTabs.querySelectorAll('paper-tab');
    for (let i=0; i <= tabs.length; i++) {
      if (tabs[i].dataset['name'] === 'change-view-tab-header-task') {
        tabs[i].setAttribute('hidden', true);
        paperTabs.selected = 0;
        break;
      }
    }
  }

  _can_show_summary(is_loading, ready_count,
      fail_count, invalid_count,
      waiting_count, duplicate_count,
      pass_count) {
    if (is_loading || ready_count || fail_count || invalid_count ||
      waiting_count || duplicate_count || pass_count) {
      return true;
    }
    return false;
  }
}

customElements.define(GrTaskSummary.is, GrTaskSummary);
