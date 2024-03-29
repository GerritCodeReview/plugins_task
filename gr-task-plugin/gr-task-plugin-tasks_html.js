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
<template is="dom-repeat" as="task" items="[[tasks]]">
  <template is="dom-if" if="[[_can_show(show_all, task)]]">
    <li>
      <style>
        /* Matching colors with core theme. */
        .green {
          color: var(--success-foreground);
        }
        .red {
          color: var(--error-foreground);
        }
        .orange {
          color: var(--warning-foreground);
        }
        .links {
          color: var(--link-color);
          cursor: pointer;
          text-decoration: underline;
        }
        gr-icon.close {
          color: var(--error-foreground);
        }
        gr-icon.block {
          color: var(--error-foreground);
        }
        gr-icon.pause {
          color: var(--warning-foreground);
        }
        gr-icon.play_arrow {
          color: var(--success-foreground);
        }
        gr-icon.check_circle {
          color: var(--success-foreground);
        }
        li {
          margin: 3px 0;
        }
      </style>
      <template is="dom-if" if="[[task.icon.id]]">
        <gr-tooltip-content
            has-tooltip
            title="In Progress"
            hidden="[[!task.in_progress]]">
            <gr-icon
              icon="hourglass_empty"
              class="hourglass_empty">
            </gr-icon>
        </gr-tooltip-content>
        <gr-tooltip-content
            has-tooltip
            title$="[[task.icon.tooltip]]">
            <gr-icon class$="[[task.icon.id]]" filled icon="[[task.icon.id]]"></gr-icon>
        </gr-tooltip-content>
      </template>
      <template is="dom-if" if="[[task.change]]">
        <a class="links" href$="[[_getChangeUrl(task.change)]]">[[task.change]]</a>
      </template>
      <template is="dom-if" if="[[!task.change]]">
        <template is="dom-if" if="[[!task.hint]]">
          [[task.name]]
        </template>
      </template>
      <template is="dom-if" if="[[task.hint]]">
        <gr-formatted-text style="display: -webkit-inline-box;"
          pre=""
          content="[[task.hint]]"
          config="[[config]]">
        </gr-formatted-text>
      </template>
    </li>
  </template>
  <ul style="list-style-type:none; margin: 0 0 0 0; padding: 0 0 0 2em;">
    <gr-task-plugin-tasks
        tasks="[[task.sub_tasks]]"
        show_all$="[[show_all]]"> </gr-task-plugin-tasks>
  </ul>
</template>`;
