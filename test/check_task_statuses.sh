#!/bin/bash

example() { # example_num
    awk '/```/{Q++;E=(Q+1)/2};E=='"$1" < "$DOC_STATES" | grep -v '```'
}

setup_repo() { # repo remote ref
    local repo=$1 remote=$2 ref=$3
    git init "$repo"
    (
        cd "$repo"
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

query() { # query
    ssh -x -p "$PORT" "$SERVER" gerrit query "$1" \
            --format json --task--applicable| head -1 | json_pp
}

query_plugins() { query "$1" | awk '$0=="   \"plugins\" : [",$0=="   ],"' ; }

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
setup_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
setup_repo "$USERS" "$REMOTE_USERS" "$REF_USERS"

mkdir -p "$ALL_TASKS" "$USER_TASKS"

example 1 > "$ROOT_CFG"
example 2 > "$COMMON_CFG"
example 3 > "$INVALIDS_CFG"
example 4 > "$USER_SPECIAL_CFG"

update_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
update_repo "$USERS" "$REMOTE_USERS" "$REF_USERS"


example 5 |tail -n +5| awk 'NR>1{print P};{P=$0}' > "$EXPECTED"
query_plugins has:draft > "$STATUSES"

diff "$EXPECTED" "$STATUSES"
