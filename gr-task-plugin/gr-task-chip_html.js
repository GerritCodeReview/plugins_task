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
    .taskSummaryChip {
      color: var(--chip-color);
      cursor: pointer;
      display: inline-block;
      padding: var(--spacing-xxs) var(--spacing-m) var(--spacing-xxs)
        var(--spacing-s);
      margin-right: var(--spacing-s);
      border-radius: 12px;
      border: 1px solid gray;
      vertical-align: top;
      /* centered position of 20px chips in 24px line-height inline flow */
      vertical-align: top;
      position: relative;
      top: 2px;
    }
    .taskSummaryChip.warning {
      border-color: var(--warning-foreground);
      background: var(--warning-background);
    }
    .taskSummaryChip.warning:hover {
      background: var(--warning-background-hover);
      box-shadow: var(--elevation-level-1);
    }
    .taskSummaryChip.warning:focus-within {
      background: var(--warning-background-focus);
    }
    .taskSummaryChip.error {
      color: var(--error-foreground);
      border-color: var(--error-foreground);
      background: var(--error-background);
    }
    .taskSummaryChip.error:hover {
      background: var(--error-background-hover);
      box-shadow: var(--elevation-level-1);
    }
    .taskSummaryChip.error:focus-within {
      background: var(--error-background-focus);
    }
    .font-small {
      font-size: var(--font-size-small);
      font-weight: var(--font-weight-normal);
      line-height: var(--line-height-small);
    }
  </style>
  <button
    class$="taskSummaryChip font-small [[chip_style]]"
    on-click="_onChipClick">
    <slot></slot>
  </button>
`;
