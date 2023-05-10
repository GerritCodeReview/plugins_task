#!/usr/bin/env bash

die() { echo -e "\nERROR:" "$@" ; kill $$ ; exit 1 ; } # error_message

q() { "$@" > /dev/null 2>&1 ; } # cmd [args...]  # quiet a command

gssh() { ssh -x -p "$SSH_PORT" "$GERRIT_HOST" gerrit "$@" ; } # run a gerrit ssh command

create_test_user() {
    echo "Creating test user ..."
    gssh create-account "$NON_SECRET_USER" --full-name "$NON_SECRET_USER" \
       --email "$NON_SECRET_USER"@example.com --ssh-key - < ~/.ssh/id_rsa.pub
}

setup_all_projects_repo() {
    echo "Updating All-Projects repo ..."

    ( cd "$WORKSPACE"
      q git clone ssh://"$GERRIT_HOST":"$SSH_PORT"/All-Projects allProjects
      cd allProjects
      q git fetch origin refs/meta/config ; q git checkout FETCH_HEAD
      git config -f "project.config" --add access."refs/meta/config".read "group Registered Users"
      git config -f "project.config" --add capability.viewTaskPaths "group Administrators"
      q git add . && q git commit -m "project config update"
      q git push origin HEAD:refs/meta/config
    )
}

SSH_PORT=29418
USER_RUN_TESTS_DIR="$USER_HOME"/"$RUN_TESTS_DIR"
while (( "$#" )) ; do
   case "$1" in
       --non-secret-user)               shift ; NON_SECRET_USER="$1" ;;
       *)                               die "invalid argument '$1'" ;;
   esac
   shift
done

[ -z "$NON_SECRET_USER" ] && die "non-secret-user not set"

"$USER_RUN_TESTS_DIR"/create-test-project-and-changes.sh
"$USER_RUN_TESTS_DIR"/update-all-users-project.sh
create_test_user
setup_all_projects_repo