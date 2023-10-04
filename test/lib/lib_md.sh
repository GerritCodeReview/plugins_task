#!/usr/bin/env bash
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

# ---- Markdown Format Helpers ----

# Example markdown file:
# (Using block comment to better understand the file syntax.)

: <<'END'
# Test case description header

file: `All-Projects.git:refs/meta/config:task.config`
```
[root "Test root"]
    applicable = "is:open"
    pass = is:true_task
```

file: `All-Users:refs/users/some_ref:task/sample.config`
```
 [task "NON-SECRET task"]
     applicable = is:open
     pass = Fail
+    subtasks-external = SECRET

+[external "SECRET"]
+    user = {secret_user}
+    file = secret.config
```

json:
```
{
   {
     "some": "example"
   }
}
END

# (For example above)
# out:
# `All-Projects.git:refs/meta/config:task.config`
# `All-Users:refs/users/some_ref:task/sample.config`
md_file_markers() { # DOC_CONTENT
    echo "$1" | grep -o "^file: .*" | cut -f2 -d'`'
}

# (For example above)
# in: `All-Projects.git:refs/meta/config:task.config`
# out:
#[root "Test root"]
#    applicable = "is:open"
#    pass = is:true_task
#
# in: json:
# out :
# {
#    {
#      "some": "example"
#    }
# }
md_marker_content() { # DOC marker
    local start_line=$(echo "$1" | grep -n "$2" | cut -f1 -d':')
    echo "$1" | tail -n+"$start_line" | \
        sed '1,/```/d;/```/,$d' | grep -v '```'
}

# file_marker > project
# in: `All-Projects.git:refs/meta/config:task/task.config`
# out: All-Projects.git
md_file_marker_project() {
    echo "$1" | cut -f1 -d':'
}

# file_marker > ref
# in: `All-Projects.git:refs/meta/config:task/task.config`
# out: refs/meta/config
md_file_marker_ref() {
    echo "$1" | cut -f2 -d':'
}

# file_marker > file
# in: `All-Projects.git:refs/meta/config:task/task.config`
#out: task/task.config
md_file_marker_file() {
    echo "$1" | cut -f3 -d':'
}