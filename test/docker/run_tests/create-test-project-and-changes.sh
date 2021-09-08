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
    echo "$(date)" >> readme.txt
    git add . >&2 && git commit -m "$1" >&2
    git push ssh://"$GERRIT_HOST":"$PORT"/"$2" HEAD:refs/for/master >&2
    git rev-parse HEAD
}

submit_change() { # commit_revision
    gssh review --code-review +2 "$1"
sleep 10
    gssh review --submit "$1"
}

create_project 'test'
commit1Revision=$(create_change 'Change 1' 'test')
commit2Revision=$(create_change 'Change 2' 'test')

submit_change "$commit2Revision"
#sleep to avoid race conditions
sleep 60
submit_change "$commit1Revision"
