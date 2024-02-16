#!/usr/bin/env bash

SSH_PORT=29418
HTTP_PORT=8080

gssh() { ssh -x -p "$SSH_PORT" "$GERRIT_HOST" gerrit "$@" ; } # cmd [args]...

create_project() { # project
    echo "Creating a test project ..."
    gssh create-project "$1" --owner "Administrators" --submit-type "MERGE_IF_NECESSARY"
    cd "$WORKSPACE" && git clone ssh://"$GERRIT_HOST":"$SSH_PORT"/"$1" "$1" && cd "$1"
    install_changeid_hook
}

install_changeid_hook() {
    local hook=$(git rev-parse --git-dir)/hooks/commit-msg
    mkdir -p "$(dirname "$hook")"
    curl -Lo "$hook" "http://$GERRIT_HOST:$HTTP_PORT/tools/hooks/commit-msg"
    chmod +x "$hook"
}

create_change() { # subject project
    touch readme.txt && echo "$(date)" >> readme.txt
    git add . && git commit -m "$1"
    git push ssh://"$GERRIT_HOST":"$SSH_PORT"/"$2" HEAD:refs/for/master
}

create_project 'test'
create_change 'Change 1' 'test'
create_change 'Change 2' 'test'
