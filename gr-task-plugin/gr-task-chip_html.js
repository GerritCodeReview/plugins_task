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
    .taskSummaryChip.loading {
      border-color: var(--gray-foreground);
      background: var(--gray-background);
    }
    .taskSummaryChip.loading:hover {
      background: var(--gray-background-hover);
      box-shadow: var(--elevation-level-1);
    }
    .taskSummaryChip.loading:focus-within {
      background: var(--gray-background-focus);
    }
    .taskSummaryChip.pass {
      border-color: var(--success-foreground);
      background: var(--success-background);
    }
    .taskSummaryChip.pass iron-icon {
      color: var(--success-foreground);
    }
    .taskSummaryChip.pass:hover {
      background: var(--success-background-hover);
      box-shadow: var(--elevation-level-1);
    }
    .taskSummaryChip.pass:focus-within {
      background: var(--success-background-focus);
    }
    .taskSummaryChip.waiting {
      border-color: var(--warning-foreground);
      background: var(--warning-background);
    }
    .taskSummaryChip.waiting iron-icon {
      color: var(--warning-foreground);
    }
    .taskSummaryChip.waiting:hover {
      background: var(--warning-background-hover);
      box-shadow: var(--elevation-level-1);
    }
    .taskSummaryChip.waiting:focus-within {
      background: var(--warning-background-focus);
    }
    .taskSummaryChip.ready {
      border-color: var(--success-foreground);
      background: var(--success-background);
    }
    .taskSummaryChip.ready iron-icon {
      color: var(--success-foreground);
    }
    .taskSummaryChip.ready:hover {
      background: var(--success-background-hover);
      box-shadow: var(--elevation-level-1);
    }
    .taskSummaryChip.ready:focus-within {
      background: var(--success-background-focus);
    }
    .taskSummaryChip.invalid {
      color: var(--error-foreground);
      border-color: var(--error-foreground);
      background: var(--error-background);
    }
    .taskSummaryChip.invalid iron-icon {
      color: var(--error-foreground);
    }
    .taskSummaryChip.invalid:hover {
      background: var(--error-background-hover);
      box-shadow: var(--elevation-level-1);
    }
    .taskSummaryChip.invalid:focus-within {
      background: var(--error-background-focus);
    }
    .taskSummaryChip.duplicate {
      color: var(--success-foreground);
      border-color: var(--success-foreground);
      background: var(--success-background);
    }
    .taskSummaryChip.duplicate iron-icon {
      color: var(--success-foreground);
    }
    .taskSummaryChip.duplicate:hover {
      background: var(--success-background-hover);
      box-shadow: var(--elevation-level-1);
    }
    .taskSummaryChip.duplicate:focus-within {
      background: var(--success-background-focus);
    }
    .taskSummaryChip.fail {
      color: var(--error-foreground);
      border-color: var(--error-foreground);
      background: var(--error-background);
    }
    .taskSummaryChip.fail iron-icon {
      color: var(--error-foreground);
    }
    .taskSummaryChip.fail:hover {
      background: var(--error-background-hover);
      box-shadow: var(--elevation-level-1);
    }
    .taskSummaryChip.fail:focus-within {
      background: var(--error-background-focus);
    }
    .font-small {
      font-size: var(--font-size-small);
      font-weight: var(--font-weight-normal);
      line-height: var(--line-height-small);
    }
    div {
      display: flex;
      justify-content: space-between;
    }
    iron-icon {
      margin-right: 2px;
      --iron-icon-height: 16px;
      --iron-icon-width: 16px;
    }
  </style>
  <button
    class$="taskSummaryChip font-small [[chip_style]]"
    on-click="_onChipClick">
    <div tabindex="0">
      <iron-icon icon="[[_computeIconId()]]"></iron-icon>
      <div class="text">[[text]]</div>
    </div>
  </button>
`;
