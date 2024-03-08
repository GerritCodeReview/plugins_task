#!/usr/bin/env bash
#
# Copyright (C) 2024 The Android Open Source Project
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

gquery() { gssh query "$@" ; } # [args]...

change_query() { # TestName query expected_changes
    local test=$1 query=$2 changes=$3
    local out=$(gquery "$query" | grep 'number:' | awk '{print $2}' | sort)
    local expected=$(for c in $changes ; do echo $c ; done | sort)
    result_out "$1" "$expected" "$out"
    return $RESULT
}

create_config_from_md() {
    for marker in $(md_file_markers "$DOC") ; do
        local project_name="$(md_file_marker_project "$marker")"
        local project_dir="$OUT/$project_name"
        local file="$(md_file_marker_file "$marker")"
        local ref="$(md_file_marker_ref "$marker")"

        mkdir -p -- "$(dirname -- "$project_dir/$file")"
        md_marker_content "$DOC" "$marker" | \
            testdoc_2_cfg > "$project_dir/$file"
    done
}

usage() { # [error_message]
    cat <<-EOF
Usage:
    "$MYPROG" --server <gerrit_host> --non-secret-user <non-secret user>

    --help|-h                         help text
    --server|-s                       gerrit host
    --root-config-project             project containing the root task config
    --root-config-branch              branch containing the root task config
    --non-secret-user                 user who does not have permission
                                      to view other user refs.
EOF

    [ -n "$1" ] && { echo "Error: $1" ; exit 1 ; }
    exit 0
}

readlink -f / &> /dev/null || readlink() { greadlink "$@" ; } # for MacOS
MYDIR=$(dirname -- "$(readlink -f -- "$0")")
MYPROG=$(basename -- "$0")

source "$MYDIR/lib/lib_helper.sh"
source "$MYDIR/lib/lib_md.sh"

DOCS=$MYDIR/.././src/main/resources/Documentation/test
OUT=$MYDIR/../target/tests

# --- Args ----

while (( "$#" )) ; do
    case "$1" in
        --help|-h)                        usage ;;
        --server|-s)                      shift ; SERVER=$1 ;;
        --root-config-project)            shift ; ROOT_CONFIG_PRJ=$1 ;;
        --root-config-branch)             shift ; ROOT_CONFIG_BRANCH=$1 ;;
        --non-secret-user)                shift ; NON_SECRET_USER=$1 ;;
        *)                                usage "invalid argument $1" ;;
    esac
    shift
done

[ -z "$SERVER" ] && usage "You must specify --server"
[ -z "$ROOT_CONFIG_PRJ" ] && ROOT_CONFIG_PRJ=All-Projects
[ -z "$ROOT_CONFIG_BRANCH" ] && ROOT_CONFIG_BRANCH=refs/meta/config
[ -z "$NON_SECRET_USER" ] && usage "You must specify --non-secret-user"
[ -z "$GERRIT_GIT_DIR" ] && usage "GERRIT_GIT_DIR environment variable not set"

PORT=29418
HTTP_PORT=8080
PROJECT_TEST=test
PROJECT_DEV=dev
BRANCH=master
REMOTE_ALL=ssh://$SERVER:$PORT/$ROOT_CONFIG_PRJ
REF_ALL=$ROOT_CONFIG_BRANCH
REMOTE_TEST=ssh://$SERVER:$PORT/$PROJECT_TEST
REMOTE_DEV=ssh://$SERVER:$PORT/$PROJECT_DEV
ALL=$OUT/$ROOT_CONFIG_PRJ
ALL_TASKS=$ALL/task
ROOT_CFG=$ALL/task.config
CONFIG=$ROOT_CFG
TEST_NAME="Root Operator"

mkdir -p -- "$OUT" "$ALL_TASKS"
q_setup setup_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
q_setup setup_repo "$OUT/$PROJECT_TEST" "$REMOTE_TEST" "$BRANCH"
q_setup setup_repo "$OUT/$PROJECT_DEV" "$REMOTE_DEV" "$BRANCH"

DOC=$(replace_tokens < "$DOCS/root_task_operator.md")
create_config_from_md
q_setup update_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"

ROOTS=$(config_section_keys "root") || err "Invalid ROOTS"
CHANGE1=$(create_repo_change "$OUT/$PROJECT_TEST" "$REMOTE_TEST" "$BRANCH")
CHANGE2=$(create_repo_change "$OUT/$PROJECT_TEST" "$REMOTE_TEST" "$BRANCH")
CHANGE3=$(create_repo_change "$OUT/$PROJECT_TEST" "$REMOTE_TEST" "$BRANCH")
CHANGE4=$(create_repo_change "$OUT/$PROJECT_DEV" "$REMOTE_DEV" "$BRANCH")

RESULT=0
CHANGES="change:$CHANGE1 OR change:$CHANGE2 OR change:$CHANGE3 OR change:$CHANGE4"
gssh set-topic "$CHANGE3" --topic skip

query="($CHANGES) root_task:${ROOTS[0]}"
change_query "$TEST_NAME - no options" "$query" "$CHANGE1 $CHANGE2"

gssh review --code-review +2 "${CHANGE1},1" || err "Failed to +2 $CHANGE1"

query="($CHANGES) root_task:${ROOTS[0]},status=waiting"
change_query "$TEST_NAME - status=waiting" "$query" "$CHANGE2"

gssh review --code-review +2 "${CHANGE2},1" || err "Failed to +2 $CHANGE2"

query="($CHANGES) root_task:${ROOTS[0]},status=pass"
change_query "$TEST_NAME - status=pass" "$query" "$CHANGE1 $CHANGE2"

exit $RESULT
