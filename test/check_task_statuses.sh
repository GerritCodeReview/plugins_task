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

# Usage:
# 1. All-Projects.git - must have 'Push' rights on refs/meta/config for test user
# 2. All-Projects.git - must have 'viewTaskPaths' capability for test user
# 3. All-Projects.git - must have 'accessDatabase' capability for test user
# 4. All-Users.git - must have 'push' rights on refs/users/* for test user
# 5. All-Users.git - must have 'push' rights on refs/users/${shardeduserid} for Registered Users
# 6. All-Users.git - must have 'read' rights on refs/users/${shardeduserid} for Registered Users
# 7. All-Users.git - must have 'create' rights on refs/users/${shardeduserid} for Registered Users
# 8. All-Users.git - must deny 'read' rights on refs/* for Anonymous Users
# 9. GERRIT_GIT_DIR environment variable must have the path to gerrit
#    site's git directory (as group ref updates are done directly to git).

create_configs_from_task_states() {
    for marker in $(md_file_markers "$DOC_STATES") ; do
        local project_name="$(md_file_marker_project "$marker")"
        local project_dir="$OUT/$project_name"
        local file="$(md_file_marker_file "$marker")"
        local ref="$(md_file_marker_ref "$marker")"

        if [[ "$ref" == refs/groups/* ]] ; then
            project_dir="$project_dir-${ref:(-7)}}"
            q_setup setup_repo "$project_dir" "$REMOTE_USERS" "$ref"
        fi

        mkdir -p -- "$(dirname -- "$project_dir/$file")"
        md_marker_content "$DOC_STATES" "$marker" | replace_user \
            | testdoc_2_cfg > "$project_dir/$file"

        if [[ "$ref" == refs/groups/* ]] ; then
            # As support for pushing a change to group refs [1] is not yet in any release,
            # push the update behind gerrit's back, directly into git.
            # [1] https://gerrit-review.googlesource.com/c/gerrit/+/390614
            q_setup update_repo "$project_dir" "$GERRIT_GIT_DIR/All-Users.git" "$ref"
        fi
    done
}

test_2generated() { # name task_args...
    local name=$1 ; shift
    local out=$(query "$@")
    results_suite "$name" "$EXPECTED.$name" "$(echo "$out" | change_plugins 1)"
    results_suite "$name 2nd change" "$EXPECTED.$name"2 "$(echo "$out" | change_plugins 2)"
}

test_generated() { # name [-l query_user] task_args...
    local name=$1 ; shift
    query "$@" | change_plugins 1 > "$ACTUAL.$name"
    results_suite "$name" "$EXPECTED.$name" "$( < "$ACTUAL.$name")"
}

usage() { # [error_message]
    cat <<-EOF
Usage:
    "$MYPROG" --server <gerrit_host> --non-secret-user <non-secret user>
    --untrusted-user <untrusted user>

    --help|-h                         help text
    --server|-s                       gerrit host
    --root-config-project             project containing the root task config
    --root-config-branch              branch containing the root task config
    --non-secret-user                 user who don't have permission
                                      to view other user refs.
    --untrusted-user                  user who doesn't have permission
                                      to view refs/meta/config ref on All-Projects repo
    --non-secret-group-without-space  non-secret group name without spaces
    --non-secret-group-with-space     non-secret group name with spaces
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
        --untrusted-user)                 shift ; UNTRUSTED_USER=$1 ;;
        --non-secret-group-without-space) shift ; GROUP_NAME_WITHOUT_SPACE=$1 ;;
        --non-secret-group-with-space)    shift ; GROUP_NAME_WITH_SPACE=$1 ;;
        *)                                usage "invalid argument $1" ;;
    esac
    shift
done

[ -z "$SERVER" ] && usage "You must specify --server"
[ -z "$NON_SECRET_USER" ] && usage "You must specify --non-secret-user"
[ -z "$UNTRUSTED_USER" ] && usage "You must specify --untrusted-user"
[ -z "$GROUP_NAME_WITHOUT_SPACE" ] && usage "You must specify --non-secret-group-without-space"
[ -z "$GROUP_NAME_WITH_SPACE" ] && usage "You must specify --non-secret-group-with-space"
[ -z "$GERRIT_GIT_DIR" ] && usage "GERRIT_GIT_DIR environment variable not set"
[ -z "$ROOT_CONFIG_PRJ" ] && ROOT_CONFIG_PRJ=All-Projects
[ -z "$ROOT_CONFIG_BRANCH" ] && ROOT_CONFIG_BRANCH=refs/meta/config


PORT=29418
HTTP_PORT=8080
PROJECT=test
BRANCH=master
REMOTE_ALL=ssh://$SERVER:$PORT/$ROOT_CONFIG_PRJ
REMOTE_USERS=ssh://$SERVER:$PORT/All-Users
REMOTE_TEST=ssh://$SERVER:$PORT/$PROJECT
REF_ALL=$ROOT_CONFIG_BRANCH
REF_USERS=refs/users/self

ALL=$OUT/$ROOT_CONFIG_PRJ
ALL_TASKS=$ALL/task

USERS=$OUT/All-Users
USER_TASKS=$USERS/task

EXPECTED=$OUT/expected
ACTUAL=$OUT/actual

ROOT_CFG=$ALL/task.config

CONFIG=$ROOT_CFG

declare -A USER_REFS
USER_REFS["{testuser_user_ref}"]="$(get_user_ref "$USER")"

declare -A GROUP_EXPANDED_BY_PLACEHOLDER
GROUP_EXPANDED_BY_PLACEHOLDER["{non_secret_group_name_without_space}"]="$GROUP_NAME_WITHOUT_SPACE"
GROUP_EXPANDED_BY_PLACEHOLDER["{non_secret_group_name_with_space}"]="$GROUP_NAME_WITH_SPACE"
GROUP_EXPANDED_BY_PLACEHOLDER["{non_secret_group_uuid}"]="$(get_group_uuid "$GROUP_NAME_WITHOUT_SPACE")"
GROUP_EXPANDED_BY_PLACEHOLDER["{sharded_non_secret_group_uuid_without_space}"]="$(get_sharded_group_uuid "$GROUP_NAME_WITHOUT_SPACE")"
GROUP_EXPANDED_BY_PLACEHOLDER["{sharded_non_secret_group_uuid_with_space}"]="$(get_sharded_group_uuid "$GROUP_NAME_WITH_SPACE")"

mkdir -p -- "$OUT" "$ALL_TASKS" "$USER_TASKS"

q_setup setup_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
q_setup setup_repo "$USERS" "$REMOTE_USERS" "$REF_USERS" --initial-commit
q_setup setup_repo "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH"

changes=$(gssh query "status:open limit:2" --format json)
set_change "$(echo "$changes" | awk 'NR==1')" ; CHANGE1=("${CHANGE[@]}")
set_change "$(echo "$changes" | awk 'NR==2')" ; CHANGE2=("${CHANGE[@]}")

DOC_STATES=$(replace_tokens < "$DOCS/task_states.md")
DOC_PREVIEW=$(replace_tokens < "$DOCS/preview.md")
DOC_PATHS=$(replace_tokens < "$DOCS/paths.md")

create_configs_from_task_states

ROOTS=$(config_section_keys "root") || err "Invalid ROOTS"

q_setup update_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
q_setup update_repo "$USERS" "$REMOTE_USERS" "$REF_USERS"

change3_id=$(gen_change_id)
change4_id=$(gen_change_id)
change4_number=$(create_repo_change "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH" "$change4_id")
change3_number=$(create_repo_change "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH" "$change3_id")

ex2_pjson=$(example "$DOC_STATES" 2 | testdoc_2_pjson)
all_pjson=$(echo "$ex2_pjson" | \
    replace_change_properties \
        "" \
        "$change3_number" \
        "$change3_id" \
        "$PROJECT" \
        "refs\/heads\/$BRANCH" \
        "NEW" \
        "")

all2_pjson=$(echo "$ex2_pjson" | \
    replace_change_properties \
        "" \
        "$change4_number" \
        "$change4_id" \
        "$PROJECT" \
        "refs\/heads\/$BRANCH" \
        "NEW" \
        "")

no_all_visible_json=$(echo "$all_pjson" | remove_suites "all" "!visible")
no_all_no_visible_json=$(echo "$all_pjson" | remove_suites "all" "visible")
no_all_visible2_json=$(echo "$all2_pjson" | remove_suites "all" "!visible")
no_all_no_visible2_json=$(echo "$all2_pjson" | remove_suites "all" "visible")

echo "$no_all_visible_json" | strip_non_applicable | \
    grep -v "\"applicable\" :" | ensure json_pp > "$EXPECTED".applicable

echo "$no_all_no_visible_json" | strip_non_applicable | \
    grep -v "\"applicable\" :" | \
    ensure json_pp > "$EXPECTED".applicable-visibility

echo "$no_all_visible2_json" | strip_non_applicable | \
    grep -v "\"applicable\" :" | ensure json_pp > "$EXPECTED".applicable2

echo "$no_all_no_visible2_json" | strip_non_applicable | \
    grep -v "\"applicable\" :" | \
    ensure json_pp > "$EXPECTED".applicable-visibility2

echo "$all_pjson" | remove_suites "!all" "!visible" | \
    ensure json_pp > "$EXPECTED".all

echo "$no_all_visible_json" | strip_non_invalid | \
    ensure json_pp > "$EXPECTED".invalid

echo "$no_all_visible_json" | strip_non_applicable | \
    grep -v "\"applicable\" :" | strip_non_invalid | \
    ensure json_pp > "$EXPECTED".invalid-applicable


preview_pjson=$(example "$DOC_PREVIEW" 1 | testdoc_2_pjson)
echo "$preview_pjson" | remove_suites "invalid" "secret" | \
    ensure json_pp > "$EXPECTED".preview-non-secret
echo "$preview_pjson" | remove_suites "invalid" "!secret" | \
    ensure json_pp > "$EXPECTED".preview-admin
echo "$preview_pjson" | remove_suites "secret" "!invalid" | \
    strip_non_invalid | ensure json_pp > "$EXPECTED".preview-invalid

example "$DOC_PREVIEW" 1 | testdoc_2_cfg | replace_user > "$ROOT_CFG"
cnum=$(create_repo_change "$ALL" "$REMOTE_ALL" "$REF_ALL")
PREVIEW_ROOTS=$(config_section_keys "root")


RESULT=0
query="(change:$change3_number OR change:$change4_number) status:open"
test_2generated applicable --task--applicable "$query"
test_2generated applicable-visibility -l "$UNTRUSTED_USER" --task--applicable "$query"
test_generated all --task--all "$query"

test_generated invalid --task--invalid "$query"
test_generated invalid-applicable --task--applicable --task--invalid "$query"

ROOTS=$PREVIEW_ROOTS
test_generated preview-admin --task--preview "$cnum,1" --task--all "$query"
test_generated preview-non-secret -l "$NON_SECRET_USER" --task--preview "$cnum,1" --task--all "$query"
test_generated preview-invalid --task--preview "$cnum,1" --task--invalid "$query"

example "$DOC_STATES" 2 | keep_suites "task_only" | testdoc_2_pjson | \
    ensure json_pp > "$EXPECTED".task-roots-filter
test_generated task-roots-filter --task--all --task--only "Root\ PASS" "$query"

example "$DOC_PATHS" 1 | testdoc_2_cfg | replace_user > "$ROOT_CFG"
q_setup update_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
ROOTS=$(config_section_keys "root")
example "$DOC_PATHS" 1 | testdoc_2_pjson | ensure json_pp > "$EXPECTED".task-paths

test_generated task-paths --task--all --task--include-paths "$query"

example "$DOC_PATHS" 2 | testdoc_2_cfg > "$ROOT_CFG"
q_setup update_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
ROOTS=$(config_section_keys "root")
example "$DOC_PATHS" 2 | testdoc_2_pjson | ensure json_pp > "$EXPECTED".task-paths.non-secret
test_generated task-paths.non-secret -l "$NON_SECRET_USER" --task--all --task--include-paths "$query"

exit $RESULT
