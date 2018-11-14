#!/bin/bash

# Usage:
# All-Users.git - refs/users/self must already exist
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

# output must match expected to pass
result_out() { # test expected output
    local disp=$(echo "Expected Output:" ;\
                 echo "    $2" ;\
                 echo "Actual Output:" ;\
                 echo "    $3")

    [ "$2" = "$3" ]
    result "$1" "$disp"
}

# --------

q() { "$@" > /dev/null 2>&1 ; } # cmd [args...]  # quiet a command

# Run a test setup command quietly, exit on failure
q_setup() { # cmd [args...]
  local out ; out=$("$@" 2>&1) || { echo "$out" ; exit ; }
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

setup_repo() { # repo remote ref
    local repo=$1 remote=$2 ref=$3
    git init "$repo"
    (
        cd "$repo"
        install_changeid_hook "$repo"
        git fetch "$remote" "$ref"
        git checkout FETCH_HEAD
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

create_repo_change() { # repo remote ref > change_num
    local repo=$1 remote=$2 ref=$3
    (
        q cd "$repo"
        q git add .
        q git commit -m 'Testing task plugin'
        git push "$remote" HEAD:"refs/for/$ref" 2>&1 | get_change_num
    )
}

query() { # query
    ssh -x -p "$PORT" "$SERVER" gerrit query "$@" \
            --format json | head -1 | python -c "import sys, json; \
            print json.dumps(json.loads(sys.stdin.read()), indent=3, \
            separators=(',', ' : '), sort_keys=True)"
}

query_plugins() { query "$@" | awk '$0=="   \"plugins\" : [",$0=="   ],"' ; }

test_tasks() { # name expected_file task_args...
    local name=$1 expected=$2 ; shift 2
    local output=$STATUSES.$name

    query_plugins "$@" > "$output"
    out=$(diff "$expected" "$output")
    result "$name" "$out"
}

test_file() { # name task_args...
    local name=$1 ; shift
    test_tasks "$name" "$MYDIR/$name" "$@"
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
REMOTE_ALL=ssh://$SERVER:$PORT/All-Projects
REMOTE_USERS=ssh://$SERVER:$PORT/All-Users

REF_ALL=refs/meta/config
REF_USERS=refs/users/self


mkdir -p "$OUT"
q_setup setup_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
q_setup setup_repo "$USERS" "$REMOTE_USERS" "$REF_USERS"

mkdir -p "$ALL_TASKS" "$USER_TASKS"

example 1 |sed -e"s/current-user/$USER/" > "$ROOT_CFG"
example 2 > "$COMMON_CFG"
example 3 > "$INVALIDS_CFG"
example 4 > "$USER_SPECIAL_CFG"

q_setup update_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
q_setup update_repo "$USERS" "$REMOTE_USERS" "$REF_USERS"

example 5 |tail -n +5| awk 'NR>1{print P};{P=$0}' > "$EXPECTED"

query="status:open limit:1"
test_tasks statuses "$EXPECTED" --task--applicable "$query"
test_file all --task--all "$query"

replace_user < "$MYDIR"/root.change > "$ROOT_CFG"
cnum=$(create_repo_change "$ALL" "$REMOTE_ALL" "$REF_ALL")
test_file preview --task--preview "$cnum,1" --task--all "$query"
test_file preview.invalid --task--preview "$cnum,1" --task--invalid "$query"

test_file invalid --task--invalid "$query"
test_file invalid-applicable --task--applicable --task--invalid "$query"
