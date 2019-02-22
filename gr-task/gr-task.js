// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
(function() {
  'use strict';

  Polymer({
    is: 'gr-task',
    attached() {
       this._getTasks();
    },
    _getTasks() {
      const endpoint = '/changes/' +
          "?q=change:" + this.change._number +
          '&--task--applicable';

      return this.plugin.restApi().get(endpoint).then(r => {
          if (r && r.length === 1) {
            let cinfo = r[0];
            if (cinfo.plugins) {
              cinfo.plugins.forEach(pinfo => {
                if (pinfo.name === "task") {
                  this.tasks = pinfo.roots;
                }
              })
            }
          }
      });
    },
  });
})();
