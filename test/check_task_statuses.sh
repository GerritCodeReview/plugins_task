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
# All-Projects.git - must have 'Push' rights on refs/meta/config

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
    "$MYPROG" --server <gerrit_host> --untrusted-user <untrusted user>

    --help|-h                     help text
    --server|-s                   gerrit host
    --untrusted-user              user who don't have permission
                                  to view other user refs.
EOF

    [ -n "$1" ] && { echo "Error: $1" ; exit 1 ; }
    exit 0
}

readlink -f / &> /dev/null || readlink() { greadlink "$@" ; } # for MacOS
MYDIR=$(dirname -- "$(readlink -f -- "$0")")
MYPROG=$(basename -- "$0")

source "$MYDIR/lib/lib_helper.sh"

DOCS=$MYDIR/.././src/main/resources/Documentation/test
OUT=$MYDIR/../target/tests

ALL=$OUT/All-Projects
ALL_TASKS=$ALL/task

USERS=$OUT/All-Users
USER_TASKS=$USERS/task

DOC_PREVIEW=$DOCS/preview.md
EXPECTED=$OUT/expected
ACTUAL=$OUT/actual

ROOT_CFG=$ALL/task.config
COMMON_CFG=$ALL_TASKS/common.config
INVALIDS_CFG=$ALL_TASKS/invalids.config
USER_SPECIAL_CFG=$USER_TASKS/special.config

# --- Args ----

while (( "$#" )) ; do
    case "$1" in
        --help|-h)                usage ;;
        --server|-s)              shift ; SERVER=$1 ;;
        --untrusted-user)         shift ; UNTRUSTED_USER=$1 ;;
        *)                        usage "invalid argument $1" ;;
    esac
    shift
done

[ -z "$SERVER" ] && usage "You must specify --server"
[ -z "$UNTRUSTED_USER" ] && usage "You must specify --untrusted-user"


PORT=29418
PROJECT=test
BRANCH=master
REMOTE_ALL=ssh://$SERVER:$PORT/All-Projects
REMOTE_USERS=ssh://$SERVER:$PORT/All-Users
REMOTE_TEST=ssh://$SERVER:$PORT/$PROJECT

REF_ALL=refs/meta/config
REF_USERS=refs/users/self

CONFIG=$ROOT_CFG

mkdir -p "$OUT" "$ALL_TASKS" "$USER_TASKS"

q_setup setup_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
q_setup setup_repo "$USERS" "$REMOTE_USERS" "$REF_USERS" --initial-commit
q_setup setup_repo "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH"

changes=$(gssh query "status:open limit:2" --format json)
set_change "$(echo "$changes" | awk 'NR==1')" ; CHANGE1=("${CHANGE[@]}")
set_change "$(echo "$changes" | awk 'NR==2')" ; CHANGE2=("${CHANGE[@]}")
DOC_STATES=$(replace_default_changes < "$DOCS/task_states.md")

example 2 | replace_user | testdoc_2_cfg > "$ROOT_CFG"
example 3 > "$COMMON_CFG"
example 4 > "$INVALIDS_CFG"
example 5 > "$USER_SPECIAL_CFG"

ROOTS=$(config_section_keys "root") || err "Invalid ROOTS"

q_setup update_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
q_setup update_repo "$USERS" "$REMOTE_USERS" "$REF_USERS"

change3_id=$(gen_change_id)
change4_id=$(gen_change_id)
change4_number=$(create_repo_change "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH" "$change4_id")
change3_number=$(create_repo_change "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH" "$change3_id")

ex2_pjson=$(example 2 | testdoc_2_pjson)
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

no_all_json=$(echo "$all_pjson" | remove_suites all)
no_all2_json=$(echo "$all2_pjson" | remove_suites all)

echo "$no_all_json" | strip_non_applicable | \
    grep -v "\"applicable\" :" > "$EXPECTED".applicable
echo "$no_all2_json" | strip_non_applicable | \
    grep -v "\"applicable\" :" > "$EXPECTED".applicable2

echo "$all_pjson" | remove_not_suite all | ensure json_pp > "$EXPECTED".all

echo "$no_all_json" | strip_non_invalid > "$EXPECTED".invalid

strip_non_invalid < "$EXPECTED".applicable > "$EXPECTED".invalid-applicable


preview_pjson=$(testdoc_2_pjson < "$DOC_PREVIEW" | replace_default_changes)
echo "$preview_pjson" | remove_suites "invalid" "!untrusted" | \
    ensure json_pp > "$EXPECTED".preview-untrusted
echo "$preview_pjson" | remove_suites "invalid" "untrusted" | \
    ensure json_pp > "$EXPECTED".preview-admin
echo "$preview_pjson" | remove_suites "!untrusted" "!invalid" | \
    strip_non_invalid > "$EXPECTED".preview-invalid

testdoc_2_cfg < "$DOC_PREVIEW" | replace_user > "$ROOT_CFG"
cnum=$(create_repo_change "$ALL" "$REMOTE_ALL" "$REF_ALL")
PREVIEW_ROOTS=$(config_section_keys "root")


RESULT=0
query="(change:$change3_number OR change:$change4_number) status:open"
test_2generated applicable --task--applicable "$query"
test_generated all --task--all "$query"

test_generated invalid --task--invalid "$query"
test_generated invalid-applicable --task--applicable --task--invalid "$query"

ROOTS=$PREVIEW_ROOTS
test_generated preview-admin --task--preview "$cnum,1" --task--all "$query"
test_generated preview-untrusted -l "$UNTRUSTED_USER" --task--preview "$cnum,1" --task--all "$query"
test_generated preview-invalid --task--preview "$cnum,1" --task--invalid "$query"

exit $RESULT
