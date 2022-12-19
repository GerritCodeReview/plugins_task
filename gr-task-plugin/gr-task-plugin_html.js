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
  <style include="gr-a11y-styles">
    /* Workaround for empty style block - see https://github.com/Polymer/tools/issues/408 */
  </style>
  <style include="shared-styles">
    .header {
      align-items: center;
      background-color: var(--background-color-primary);
      border-bottom: 1px solid var(--border-color);
      display: flex;
      padding: var(--spacing-s) var(--spacing-l);
      z-index: 99; /* Less than gr-overlay's backdrop */
    }
    .headerTitle {
      align-items: center;
      display: flex;
      flex: 1;
    }
    .headerSubject {
      font-family: var(--header-font-family);
      font-size: var(--font-size-h3);
      font-weight: var(--font-weight-h3);
      line-height: var(--line-height-h3);
      margin-left: var(--spacing-l);
    }
    paper-tabs {
      background-color: var(--background-color-tertiary);
      margin-top: var(--spacing-m);
      height: calc(var(--line-height-h3) + var(--spacing-m));
      --paper-tabs-selection-bar-color: var(--link-color);
    }
    paper-tab {
      box-sizing: border-box;
      max-width: 12em;
      --paper-tab-ink: var(--link-color);
    }
    section {
      background-color: var(--view-background-color);
      box-shadow: var(--elevation-level-1);
    }
    ul {
      padding-left: 0.5em;
      margin-top: 0;
    }
    .links {
      color: var(--link-color);
      cursor: pointer;
      text-decoration: underline;
    }
    .show-all-button iron-icon {
      color: inherit;
      --iron-icon-height: 18px;
      --iron-icon-width: 18px;
    }
    .no-margins { margin: 0 0 0 0; }
    .task-list-item {
      display: flex;
      align-items: center;
      column-gap: 1em;
      padding-top: 12px;
      padding-left: 12px;
    }
</style>

<div id="tasks" hidden$="[[_is_hidden(_isPending, _tasks)]]">
  <paper-tabs id="secondaryTabs" selected="0">
    <paper-tab
      data-name$="Tasks"
      class="Tasks"
    >
      Tasks
    </paper-tab>
  </paper-tabs>
  <section class="TasksList">
    <div hidden$="[[!_isPending]]" class="task-list-item">Loading...</div>
    <div hidden$="[[_isPending]]" class="task-list-item">
    <template is="dom-if" if="[[_is_show_all(_show_all)]]">
      <p> All ([[_all_count]]) |
        <span
            on-click="_needs_and_blocked_tap"
            class="links">Needs + Blocked ([[_ready_count]], [[_fail_count]])</span>
      <p>
    </template>
    <template is="dom-if" if="[[!_is_show_all(_show_all)]]">
      <p> <span
            class="links"
            on-click="_show_all_tap">All ([[_all_count]])</span>
        &nbsp;| Needs + Blocked ([[_ready_count]], [[_fail_count]])</p>
    </template>
    <gr-button link="" class="show-all-button" on-click="_switch_expand"
    >[[_computeShowAllLabelText(_expand_all)]]
    <iron-icon
      icon="gr-icons:expand-more"
      hidden$="[[_expand_all]]"
    ></iron-icon
    ><iron-icon
      icon="gr-icons:expand-less"
      hidden$="[[!_expand_all]]"
    ></iron-icon>
    </gr-button>
  </div>
  <div hidden$="[[!_expand_all]]" style="padding-bottom: 12px">
    <ul style="list-style-type:none;">
      <gr-task-plugin-tasks
          tasks="[[_tasks]]"
          show_all$="[[_show_all]]"> </gr-task-plugin-tasks>
    </ul>
  </div>
  </section>
</div>`;
