#!/usr/bin/env python
#
# Copyright (C) 2021 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys
import json

STATUS='status'
SUBTASKS='subTasks'

def keep_invalid(tasks):
    i=0
    while i < len(tasks):
        nexti = i + 1

        task=tasks[i]
        if SUBTASKS in task.keys():
            subtasks=task[SUBTASKS]
            keep_invalid(subtasks)
            if len(subtasks) == 0:
                del task[SUBTASKS]

        status=''
        if STATUS in task.keys():
            status = task[STATUS]
        if status != 'INVALID' and not SUBTASKS in task.keys():
            del tasks[i]
            nexti = i

        i = nexti

plugins=json.loads(sys.stdin.read())
roots=plugins['plugins'][0]['roots']
keep_invalid(roots)
print json.dumps(plugins, indent=3, separators=(',', ' : '), sort_keys=True)
