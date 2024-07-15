#!/usr/bin/env python3
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

APPLICABLE='applicable'
HASPASS='hasPass'
STATUS='status'
SUBTASKS='subTasks'

def del_non_applicable(tasks):
    i=0
    while i < len(tasks):
        nexti = i + 1

        task=tasks[i]
        if APPLICABLE in list(task.keys()) and task[APPLICABLE] == False:
            del tasks[i]
            nexti = i
        else:
            subtasks=[]
            if SUBTASKS in list(task.keys()):
                subtasks=task[SUBTASKS]
                del_non_applicable(subtasks)
            if SUBTASKS in list(task.keys()) and len(subtasks) == 0:
                del task[SUBTASKS]
            if not SUBTASKS in list(task.keys()):
                if HASPASS in list(task.keys()) and task[HASPASS] == False:
                    status=''
                    if STATUS in list(task.keys()):
                        status = task[STATUS]
                    if status != 'INVALID' and status != 'DUPLICATE' and status != 'SKIPPED':
                        del tasks[i]
                        nexti = i

        i = nexti

plugins=json.loads(sys.stdin.read())
roots=plugins['plugins'][0]['roots']
del_non_applicable(roots)
print(json.dumps(plugins, indent=3, separators=(',', ' : '), sort_keys=True))
