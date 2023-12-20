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

import './gr-task-plugin.js';
import {htmlTemplate} from './gr-task-chip_html.js';

export class GrTaskChip extends Polymer.Element {
  static get is() {
    return 'gr-task-chip';
  }

  static get template() {
    return htmlTemplate;
  }

  static get properties() {
    return {
      chip_style: {
        type: String,
        notify: true,
        value: 'ready',
      },
    };
  }

  static getPrimaryTabs() {
    return document.querySelector('gr-app')
        .shadowRoot.querySelector('gr-app-element')
        .shadowRoot.querySelector('main')
        .querySelector('gr-change-view')
        .shadowRoot.querySelector('#mainContent')
        .querySelector('#primaryTabs');
  }

  _setTasksTabActive() {
    // TODO: Identify a better way as current implementation is fragile
    const paperTabs = GrTaskChip.getPrimaryTabs();
    const tabs = paperTabs.querySelectorAll('paper-tab');
    for (let i=0; i <= tabs.length; i++) {
      if (tabs[i].dataset['name'] === 'change-view-tab-header-task') {
        paperTabs.selected = i;
        tabs[i].scrollIntoView({block: 'center'});
        break;
      }
    }
  }

  _onChipClick() {
    this._setTasksTabActive();
  }
}

customElements.define(GrTaskChip.is, GrTaskChip);