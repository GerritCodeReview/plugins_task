#!/bin/bash

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
    git init -- "$repo"
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

query_plugins() { query "$@" | awk '$0=="   \"plugins\" : [",$0=="   ],"' ; }

test_tasks() { # name expected_file task_args...
    local name=$1 expected=$2 ; shift 2
    local output=$STATUSES.$name

    query_plugins "$@" > "$output"
    out=$(diff -- "$expected" "$output")
    result "$name" "$out"
}

test_file() { # name task_args...
    local name=$1 ; shift
    test_tasks "$name" "$MYDIR/$name" "$@"
}

MYDIR=$(dirname -- "$0")
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

RESULT=0

mkdir -p -- "$OUT"
q_setup setup_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
q_setup setup_repo "$USERS" "$REMOTE_USERS" "$REF_USERS" --initial-commit
q_setup setup_repo "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH"

mkdir -p -- "$ALL_TASKS" "$USER_TASKS"

CHANGES=($(gssh query "status:open limit:2" | grep 'number:' | awk '{print $2}'))
replace_change_properties "$DOC_STATES" "1" "${CHANGES[0]}"
replace_change_properties "$DOC_STATES" "2" "${CHANGES[1]}"
replace_change_properties "$MYDIR/all" "1" "${CHANGES[0]}"
replace_change_properties "$MYDIR/all" "2" "${CHANGES[1]}"
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

example 5 |tail -n +5| awk 'NR>1{print P};{P=$0}' > "$EXPECTED"

change3_id=$(gen_change_id)
change3_number=$(create_repo_change "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH" "$change3_id")
replace_change_properties "$EXPECTED" "3" "$change3_number" "$change3_id" "$PROJECT" "refs\/heads\/$BRANCH" "NEW" ""
replace_change_properties "$MYDIR/all" "3" "$change3_number" "$change3_id" "$PROJECT" "refs\/heads\/$BRANCH" "NEW" ""

query="change:$change3_number status:open"
test_tasks statuses "$EXPECTED" --task--applicable "$query"
test_file all --task--all "$query"

replace_user < "$MYDIR"/root.change > "$ROOT_CFG"
cnum=$(create_repo_change "$ALL" "$REMOTE_ALL" "$REF_ALL")
test_file preview --task--preview "$cnum,1" --task--all "$query"
test_file preview.invalid --task--preview "$cnum,1" --task--invalid "$query"

test_file invalid --task--invalid "$query"
test_file invalid-applicable --task--applicable --task--invalid "$query"

exit $RESULT
