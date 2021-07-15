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

export const htmlTemplate = Polymer.html`
<style>
  ul {
    padding-left: 0.5em;
    margin-top: 0;
  }
  h3 {
    padding-left: 0.1em;
    margin: 0 0 0 0;
  }
  .cursor { cursor: pointer; }
  .links {
    color: blue;
    cursor: pointer;
    text-decoration: underline;
  }
  #tasks_header {
    align-items: center;
    background-color: #fafafa;
    border-top: 1px solid #ddd;
    display: flex;
    padding: 6px 1rem;
  }
  .no-margins { margin: 0 0 0 0; }
</style>

<div id="tasks" hidden$="[[!_tasks.length]]">
  <div id="tasks_header" style="display: flex;">
    <iron-icon
        icon="gr-icons:expand-less"
        hidden$="[[!_expand_all]]"
        on-tap="_switch_expand"
        class="cursor"> </iron-icon>
    <iron-icon
        icon="gr-icons:expand-more"
        hidden$="[[_expand_all]]"
        on-tap="_switch_expand"
        class="cursor"> </iron-icon>
    <div style="display: flex; align-items: center; column-gap: 1em;">
    <h3 class="no-margins" on-tap="_switch_expand" class="cursor"> Tasks </h3>
    <template is="dom-if" if="[[_is_show_all(_show_all)]]">
      <p class="no-margins" >All ([[_all_count]]) |&nbsp;
        <span
            on-click="_needs_and_blocked_tap"
            class="links">Needs + Blocked ([[_ready_count]], [[_fail_count]])</span>
      <p>
    </template>
    <template is="dom-if" if="[[!_is_show_all(_show_all)]]">
      <p class="no-margins" > <span
            class="links"
            on-click="_show_all_tap">All ([[_all_count]])</span>
        &nbsp;| Needs + Blocked ([[_ready_count]], [[_fail_count]])</p>
    </template>
  </div>
  </div>
  <div hidden$="[[!_expand_all]]">
    <ul style="list-style-type:none;">
      <gr-task-plugin-tasks
          tasks="[[_tasks]]"
          show_all$="[[_show_all]]"> </gr-task-plugin-tasks>
    </ul>
  </div>
</div>`;
