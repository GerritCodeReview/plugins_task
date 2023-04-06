#!/usr/bin/env bash
#
# Copyright (C) 2022 The Android Open Source Project
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

readlink -f / &> /dev/null || readlink() { greadlink "$@" ; } # for MacOS
MYDIR=$(dirname -- "$(readlink -f -- "$0")")
MYPROG=$(basename -- "$0")

source "$MYDIR/lib/lib_helper.sh"

# Visibility tests cases are described using a markdown file.
# Each file has a list of config files specified by file
# markers. The initial state of task configs is created using
# them. Only one of the config file has an inline diff. Gerrit
# change is created by applying that diff to the specified file
# marker and the expected json is asserted by using that change
# as an input to the '--task-preview' switch.

# The syntax for inline diff is similar to  diff --unified=MAX_INT.
# All lines start with a leading space and if a specific line is
# part of diff, we use diff indicators (+/-) instead of a leading
# space.

# Example markdown file:
# (Using block comment to better understand the file syntax.)

: <<'END'
# Test case description header

file: `All-Projects.git:refs/meta/config:task.config`
```
[root "Test root"]
    applicable = "is:open"
    pass = True
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
get_file_markers() {
    echo "$TEST_DOC" | grep -o "^file: .*" | cut -f2 -d'`'
}

# (For example above)
# in: `All-Projects.git:refs/meta/config:task.config`
# out:
#[root "Test root"]
#    applicable = "is:open"
#    pass = True
#
# in: json:
# out :
# {
#    {
#      "some": "example"
#    }
# }
get_marker_content() { # marker
    local start_line=$(echo "$TEST_DOC" | grep -n "$1" | cut -f1 -d':')
    echo "$TEST_DOC" | tail -n+"$start_line" | \
        sed '1,/```/d;/```/,$d' | grep -v '```'
}

# file_marker > project
# in: `All-Projects.git:refs/meta/config:task/task.config`
# out: All-Projects.git
get_project_from_marker() {
    echo "$WORKSPACE_DIR/$(echo "$1" | cut -f1 -d':')"
}

# file_marker > ref
# in: `All-Projects.git:refs/meta/config:task/task.config`
# out: refs/meta/config
get_ref_from_marker() {
    echo "$1" | cut -f2 -d':'
}

# file_marker > file
# in: `All-Projects.git:refs/meta/config:task/task.config`
#out: task/task.config
get_file_from_marker() {
    echo "$1" | cut -f3 -d':'
}

# Example input for all diff functions:
#
#  [root "Root Preview SECRET external"]
#      applicable = is:open
#      pass = True
# -    subtask = Subtask APPLICABLE
# +    subtasks-external = SECRET external
#
# +[external "SECRET external"]
# +    user = {secret_user}
# +    file = secret.config


# Returns if a config has inline diff or not.
diff_indicators_present() { # file_content
    echo "$1" | grep -q "^-\|^+"
}

# file_content_with_diff_indicators > file_content_with_diff_applied
# out:
#[root "Root Preview SECRET external"]
#    applicable = is:open
#    pass = True
#    subtask = Subtask APPLICABLE
diff_apply() {
    sed -e '/^-/d' -e 's/^.//'
}

# file_content_with_diff_indicators > file_content_with_diff_reverted
# out:
#[root "Root Preview SECRET external"]
#    applicable = is:open
#    pass = True
#    subtasks-external = SECRET external
#
#[external "SECRET external"]
#    user = {secret_user}
#    file = secret.config
diff_revert() {
    sed -e '/^+/d' -e 's/^.//'
}

config_ensure() { # config_file_path
    q git config --list -f "$1" || err "Invalid config file: $1"
}

get_remote() { # project > remote_url
    echo "ssh://$SERVER:$PORT/$(basename "$1")"
}

# Gets json from the preview doc and creates
# expected json in workspace to assert later.
create_expected_json() {
    local json=$(get_marker_content "json:")

    echo "$json" | remove_suites "non-secret" | \
      testdoc_2_pjson | ensure json_pp > "$EXPECTED_SECRET"
    echo "$json" | remove_suites "secret" | \
      testdoc_2_pjson | ensure json_pp > "$EXPECTED_NON_SECRET"
}

test_preview() { # preview_change_number
    query --task--all --task--preview "$1,1" "change:1" \
      | change_plugins 1 > "$ACTUAL_SECRET"
    query -l "$NON_SECRET_USER" --task--all --task--preview "$1,1" "change:1" \
      | change_plugins 1 > "$ACTUAL_NON_SECRET"

    ROOTS=$(jq -r '.plugins[].roots | .[].name' < "$EXPECTED_SECRET")
    results_suite "Visibility Secret Test" "$EXPECTED_SECRET" "$( < "$ACTUAL_SECRET" )"

    ROOTS=$(jq -r '.plugins[].roots | .[].name' < "$EXPECTED_NON_SECRET")
    results_suite "Visibility Non-Secret Test" "$EXPECTED_NON_SECRET" "$( < "$ACTUAL_NON_SECRET" )"
}

init_configs() {
    for marker in $(get_file_markers) ; do
        local project="$(get_project_from_marker "$marker")"
        local ref="$(get_ref_from_marker "$marker")"
        local file="$(get_file_from_marker "$marker")"
        local content="$(get_marker_content "$marker")"
        local tip_content

        q_setup setup_repo "$project" "$(get_remote "$project")" "$ref"
        mkdir -p "$(dirname "$project/$file")"

        if diff_indicators_present "$content" ; then
            CHANGE_FILE_MARKER=$marker
            CHANGE_CONTENT=$(echo "$content" | diff_apply)
            tip_content=$(echo "$content" | diff_revert)
        else
            tip_content=$content
        fi

        echo "$tip_content" > "$project/$file"
        config_ensure "$project/$file"
        q_setup update_repo "$project" "$(get_remote "$project")" "$ref"
    done
}

test_change() {
    local project="$(get_project_from_marker "$CHANGE_FILE_MARKER")"
    local ref="$(get_ref_from_marker "$CHANGE_FILE_MARKER")"
    local file="$(get_file_from_marker "$CHANGE_FILE_MARKER")"
    q_setup setup_repo "$project" "$(get_remote "$project")" "$ref"

    echo "$CHANGE_CONTENT"  > "$project/$file"
    config_ensure "$project/$file"
    local cnum=$(create_repo_change "$project" "$(get_remote "$project")" "$ref")

    create_expected_json
    test_preview "$cnum"
}

usage() { # [error_message]
    cat <<-EOF
Usage:
    "$MYPROG" --server <gerrit_host> --non-secret-user <non-secret user>

    --help|-h                     help text
    --server|-s                   gerrit host
    --non-secret-user             user who doesn't have permission
                                  to view other user refs.
EOF

    [ -n "$1" ] && { echo "Error: $1" ; exit 1 ; }
    exit 0
}

while (( "$#" )) ; do
    case "$1" in
        --help|-h)                usage ;;
        --server|-s)              shift ; SERVER=$1 ;;
        --non-secret-user)        shift ; NON_SECRET_USER=$1 ;;
        *)                        usage "invalid argument $1" ;;
    esac
    shift
done

[ -z "$SERVER" ] && usage "You must specify --server"
[ -z "$NON_SECRET_USER" ] && usage "You must specify --non-secret-user"

RESULT=0
PORT=29418
HTTP_PORT=8080
WORKSPACE_DIR=$MYDIR/../target/preview
EXPECTED_SECRET="$WORKSPACE_DIR/expected-secret"
EXPECTED_NON_SECRET="$WORKSPACE_DIR/expected-non-secret"
ACTUAL_SECRET="$WORKSPACE_DIR/actual-secret"
ACTUAL_NON_SECRET="$WORKSPACE_DIR/actual-non-secret"
TEST_DOC_DIR="$MYDIR/../src/main/resources/Documentation/test/task-preview/"

declare -A USERS
declare -A USER_REFS
USERS["{secret_user}"]="$USER"
USER_REFS["{secret_user_ref}"]="$(get_user_ref "$USER")"
USERS["{non_secret_user}"]="$NON_SECRET_USER"
USER_REFS["{non_secret_user_ref}"]="$(get_user_ref "$NON_SECRET_USER")"

mkdir -p "$WORKSPACE_DIR"
trap 'rm -rf "$WORKSPACE_DIR"' EXIT

TESTS=(
"new_root_with_original_with_external_secret_ref.md"
"non-secret_ref_with_external_secret_ref.md"
"root_with_external_non-secret_ref_with_external_secret_ref.md"
"root_with_external_secret_ref.md")

for test in "${TESTS[@]}" ; do
    TEST_DOC="$(replace_user_refs < "$TEST_DOC_DIR/$test" | replace_users)"
    init_configs
    test_change
done

exit $RESULT