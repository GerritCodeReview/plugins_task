#!/bin/bash
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

# ---- TEST RESULTS ----
result() { # test [error_message]
    local result=$?
    if [ $result -eq 0 ] ; then
        echo "PASSED - $1 test"
    else
        echo "*** FAILED *** - $1 test"
        RESULT=$result
        [ $# -gt 1 ] && echo "$2"
    fi
}

# --------
gssh() { ssh -x -p "$PORT" "$SERVER" gerrit "$@" ; } # cmd [args]...

q() { "$@" > /dev/null 2>&1 ; } # cmd [args...]  # quiet a command

gen_change_id() { echo "I$(uuidgen | openssl dgst -sha1 -binary | xxd -p)"; } # > change_id

commit_message() { printf "$1 \n\nChange-Id: $2" ; } # message change-id > commit_msg

# Run a test setup command quietly, exit on failure
q_setup() { # cmd [args...]
  local out ; out=$("$@" 2>&1) || { echo "$out" ; exit ; }
}

replace_change_properties() { # file change_token change_number change_id project branch status topic

    sed -i -e "s/_change$2_number/$3/g" \
              -e "s/_change$2_id/$4/g" \
              -e "s/_change$2_project/$5/g" \
              -e "s/_change$2_branch/$6/g" \
              -e "s/_change$2_status/$7/g" \
              -e "s/_change$2_topic/$8/g" "$1"
}

replace_user() { # < text_with_testuser > text_with_$USER
    sed -e"s/testuser/$USER/"
}

example() { # example_num
    awk '/```/{Q++;E=(Q+1)/2};E=='"$1" < "$DOC_STATES" | grep -v '```' | replace_user
}

get_change_num() { # < gerrit_push_response > changenum
    local url=$(awk '/New Changes:/ { getline; print $2 }')
    echo "${url##*\/}" | tr -d -c '[:digit:]'
}

install_changeid_hook() { # repo
    local hook=$(git rev-parse --git-dir)/hooks/commit-msg
    scp -p -P "$PORT" "$SERVER":hooks/commit-msg "$hook"
    chmod +x "$hook"
}

setup_repo() { # repo remote ref [--initial-commit]
    local repo=$1 remote=$2 ref=$3 init=$4
    git init "$repo"
    (
        cd "$repo"
        install_changeid_hook "$repo"
        git fetch "$remote" "$ref"
        if ! git checkout FETCH_HEAD ; then
            if [ "$init" = "--initial-commit" ] ; then
                git commit --allow-empty -a -m "Initial Commit"
            fi
        fi
    )
}

update_repo() { # repo remote ref
    local repo=$1 remote=$2 ref=$3
    (
        cd "$repo"
        git add .
        git commit -m 'Testing task plugin'
        git push "$remote" HEAD:"$ref"
    )
}

create_repo_change() { # repo remote ref [change_id] > change_num
    local repo=$1 remote=$2 ref=$3 change_id=$4 msg="Test change"
    (
        q cd "$repo"
        date > file
        q git add .
        [ -n "$change_id" ] && msg=$(commit_message "$msg" "$change_id")
        q git commit -m "$msg"
        git push "$remote" HEAD:"refs/for/$ref" 2>&1 | get_change_num
    )
}

query() { # query
    gssh query "$@" \
        --format json | head -1 | python -c "import sys, json; \
        print json.dumps(json.loads(sys.stdin.read()), indent=3, \
        separators=(',', ' : '), sort_keys=True)"
}

query_plugins() { # query
    gssh query "$@" --format json | head -1 | python -c "import sys, json; \
        plugins={}; plugins['plugins']=json.loads(sys.stdin.read())['plugins']; \
        print json.dumps(plugins, indent=3, separators=(',', ' : '), sort_keys=True)"
}

test_tasks() { # name expected_file task_args...
    local name=$1 expected=$2 ; shift 2
    local output=$STATUSES.$name

    query_plugins "$@" > "$output"
    out=$(diff "$expected" "$output")
    result "$name" "$out"
}

test_generated() { # name task_args...
    local name=$1 ; shift
    test_tasks "$name" "$EXPECTED.$name" "$@"
}

test_file() { # name task_args...
    local name=$1 ; shift
    local expected=$MYDIR/$name output=$STATUSES.$name

    query "$@" | awk '$0=="   \"plugins\" : [",$0=="   ],"' > "$output"
    out=$(diff "$expected" "$output")
    result "$name" "$out"
}

MYDIR=$(dirname "$0")
DOCS=$MYDIR/.././src/main/resources/Documentation
OUT=$MYDIR/../target/tests

ALL=$OUT/All-Projects
ALL_TASKS=$ALL/task

USERS=$OUT/All-Users
USER_TASKS=$USERS/task

DOC_STATES=$DOCS/task_states.md
EXPECTED=$OUT/expected
STATUSES=$OUT/statuses

ROOT_CFG=$ALL/task.config
COMMON_CFG=$ALL_TASKS/common.config
INVALIDS_CFG=$ALL_TASKS/invalids.config
USER_SPECIAL_CFG=$USER_TASKS/special.config

# --- Args ----
SERVER=$1
[ -z "$SERVER" ] && { echo "You must specify a server" ; exit ; }

PORT=29418
PROJECT=test
BRANCH=master
REMOTE_ALL=ssh://$SERVER:$PORT/All-Projects
REMOTE_USERS=ssh://$SERVER:$PORT/All-Users
REMOTE_TEST=ssh://$SERVER:$PORT/$PROJECT

REF_ALL=refs/meta/config
REF_USERS=refs/users/self

mkdir -p "$OUT"
q_setup setup_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
q_setup setup_repo "$USERS" "$REMOTE_USERS" "$REF_USERS" --initial-commit
q_setup setup_repo "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH"

mkdir -p "$ALL_TASKS" "$USER_TASKS"

CHANGES=($(gssh query "status:open limit:2" | grep 'number:' | awk '{print $2}'))
replace_change_properties "$DOC_STATES" "1" "${CHANGES[0]}"
replace_change_properties "$DOC_STATES" "2" "${CHANGES[1]}"
replace_change_properties "$MYDIR/preview" "1" "${CHANGES[0]}"
replace_change_properties "$MYDIR/preview" "2" "${CHANGES[1]}"
replace_change_properties "$MYDIR/preview.invalid" "1" "${CHANGES[0]}"
replace_change_properties "$MYDIR/preview.invalid" "2" "${CHANGES[1]}"
replace_change_properties "$MYDIR/invalid" "1" "${CHANGES[0]}"
replace_change_properties "$MYDIR/invalid" "2" "${CHANGES[1]}"
replace_change_properties "$MYDIR/invalid-applicable" "1" "${CHANGES[0]}"
replace_change_properties "$MYDIR/invalid-applicable" "2" "${CHANGES[1]}"

example 1 |sed -e"s/current-user/$USER/" > "$ROOT_CFG"
example 2 > "$COMMON_CFG"
example 3 > "$INVALIDS_CFG"
example 4 > "$USER_SPECIAL_CFG"

q_setup update_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
q_setup update_repo "$USERS" "$REMOTE_USERS" "$REF_USERS"

example 5 | tail -n +3 | sed -e'/^   \.\.\.,/d; s/^   \.\.\./}/; s/^   ],/   ]/' \
    > "$EXPECTED".all

change3_id=$(gen_change_id)
change3_number=$(create_repo_change "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH" "$change3_id")

replace_change_properties "$EXPECTED".all "3" \
    "$change3_number" \
    "$change3_id" \
    "$PROJECT" \
    "refs\/heads\/$BRANCH" \
    "NEW" \
    ""

"$MYDIR"/strip_non_applicable.py < "$EXPECTED".all | \
    grep -v "\"applicable\" :" > "$EXPECTED".applicable

RESULT=0
query="change:$change3_number status:open"
test_generated applicable --task--applicable "$query"
test_generated all --task--all "$query"

replace_user < "$MYDIR"/root.change > "$ROOT_CFG"
cnum=$(create_repo_change "$ALL" "$REMOTE_ALL" "$REF_ALL")
test_file preview --task--preview "$cnum,1" --task--all "$query"
test_file preview.invalid --task--preview "$cnum,1" --task--invalid "$query"

test_file invalid --task--invalid "$query"
test_file invalid-applicable --task--applicable --task--invalid "$query"

exit $RESULT
