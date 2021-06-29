#!/usr/bin/env bash

PORT=29418

gssh() { ssh -x -p "$PORT" "$GERRIT_HOST" gerrit "$@" ; } # cmd [args]...

create_project() { # project
    echo "Creating a test project ..."
    gssh create-project "$1" --owner "Administrators" --submit-type "MERGE_IF_NECESSARY"
    cd "$WORKSPACE" && git clone ssh://"$GERRIT_HOST":"$PORT"/"$1" "$1" && cd "$1"
    gitdir=$(git rev-parse --git-dir)
    scp -p -P "$PORT" "$USER"@"$GERRIT_HOST":hooks/commit-msg "$gitdir"/hooks/
}

create_change() { # subject project
    touch readme.txt && echo "$(date)" >> readme.txt
    git add . && git commit -m "$1"
    git push ssh://"$GERRIT_HOST":"$PORT"/"$2" HEAD:refs/for/master
}

create_project 'test'
create_change 'Change 1' 'test'
create_change 'Change 2' 'test'
