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

export const htmlTemplate = Polymer.html`
  <style include="gr-a11y-styles">
    /* Workaround for empty style block - see https://github.com/Polymer/tools/issues/408 */
  </style>
  <style include="shared-styles">
    :host {
      display: block;
      color: var(--deemphasized-text-color);
      max-width: 625px;
      margin-bottom: var(--spacing-m);
    }
    .zeroState {
      color: var(--deemphasized-text-color);
    }
    .loading.zeroState {
      margin-right: var(--spacing-m);
    }
    div.error,
    .login {
      display: flex;
      color: var(--primary-text-color);
      padding: 0 var(--spacing-s);
      margin: var(--spacing-xs) 0;
      width: 490px;
    }
    div.error {
      background-color: var(--error-background);
    }
    div.error .right {
      overflow: hidden;
    }
    div.error .right .message {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    td.key {
      padding-right: var(--spacing-l);
      padding-bottom: var(--spacing-s);
      line-height: calc(var(--line-height-normal) + var(--spacing-s));
    }
    td.value {
      padding-right: var(--spacing-l);
      padding-bottom: var(--spacing-s);
      line-height: calc(var(--line-height-normal) + var(--spacing-s));
      display: flex;
    }
    div {
      margin-left: var(--spacing-m);
    }
  </style>
  <div class="task_summary" hidden$="[[!_can_show(ready_count, fail_count)]]">
    <table>
      <tr>
        <td class="key">Tasks</td>
        <td class="value">
          <gr-task-chip chip_style="ready" hidden$="[[!ready_count]]">[[ready_count]] needs</gr-task-chip>
          <gr-task-chip chip_style="fail" hidden$="[[!fail_count]]">[[fail_count]] blocked</gr-task-chip>
        </td>
      </tr>
    </table>
  </div>
`;
