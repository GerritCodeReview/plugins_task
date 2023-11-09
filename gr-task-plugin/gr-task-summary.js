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
import './gr-task-chip.js';

class GrTaskSummary extends Polymer.Element {
  static get is() {
    return 'gr-task-summary';
  }

  static get template() {
    return htmlTemplate;
  }

  static get properties() {
    return {
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
    };
  }

  /** @override */
  ready() {
    super.ready();
    document.addEventListener('tasks-loaded', e => {
      this.ready_count = e.detail.ready_count;
      this.fail_count = e.detail.fail_count;
      this.invalid_count = e.detail.invalid_count;
      this.waiting_count = e.detail.waiting_count;
      this.duplicate_count = e.detail.duplicate_count;
      this.pass_count = e.detail.pass_count;
    });
  }

  _can_show_chips(ready_count, fail_count, invalid_count,
      waiting_count, duplicate_count, pass_count) {
    return ready_count || fail_count || invalid_count ||
      waiting_count || duplicate_count || pass_count;
  }
}

customElements.define(GrTaskSummary.is, GrTaskSummary);