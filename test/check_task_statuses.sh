#!/bin/bash

# Usage:
# All-Projects.git - must have 'Push' rights on refs/meta/config

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
            --format json --task--applicable| head -1 | python -c "import sys, json; \
            print json.dumps(json.loads(sys.stdin.read()), indent=3, \
            separators=(',', ' : '), sort_keys=True)"
}

query_plugins() { query "$1" | awk '$0=="   \"plugins\" : [",$0=="   ],"' ; }

MYDIR=$(dirname "$0")
DOCS=$MYDIR/.././src/main/resources/Documentation
OUT=$MYDIR/../target/tests

ALL=$OUT/All-Projects

DOC_STATES=$DOCS/task_states.md
EXPECTED=$OUT/expected
STATUSES=$OUT/statuses

ROOT_CFG=$ALL/task.config

# --- Args ----
SERVER=$1
[ -z "$SERVER" ] && { echo "You must specify a server" ; exit ; }

PORT=29418
REMOTE_ALL=ssh://$SERVER:$PORT/All-Projects

REF_ALL=refs/meta/config


mkdir -p "$OUT"
setup_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"

example 1 > "$ROOT_CFG"

update_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"

example 2 |tail -n +5| awk 'NR>1{print P};{P=$0}' > "$EXPECTED"

query_plugins "status:open limit:1" > "$STATUSES"
diff "$EXPECTED" "$STATUSES"
