#!/usr/bin/env bash

die() { echo "ERROR: $1" >&2 ; exit 1 ; } # errormsg

is_plugin_loaded() { # plugin_name
    ssh -p 29418 "$GERRIT_HOST" gerrit plugin ls | awk '{print $1}' | grep -q "^$1\$"
}

USER_RUN_TESTS_DIR="$USER_HOME"/"$RUN_TESTS_DIR"
mkdir "$USER_HOME"/task && cp -r /task/{src,test} "$USER_HOME"/task

if [ "$1" = "retest" ] ; then
    cd "$USER_RUN_TESTS_DIR"/../../ && ./check_task_statuses.sh "$GERRIT_HOST"
    exit $?
fi

./"$USER_RUN_TESTS_DIR"/wait-for-it.sh "$GERRIT_HOST":29418 -t 60 -- echo "gerrit is up"

echo "Creating a default user account ..."

cat "$USER_HOME"/.ssh/id_rsa.pub | ssh -p 29418 -i /server-ssh-key/ssh_host_rsa_key \
  "Gerrit Code Review@$GERRIT_HOST" suexec --as "admin@example.com" -- gerrit create-account \
     --ssh-key - --email "gerrit_admin@localdomain"  --group "Administrators" "gerrit_admin"

is_plugin_loaded "task" || die "Task plugin is not installed"

./"$USER_RUN_TESTS_DIR"/create-test-project-and-changes.sh
./"$USER_RUN_TESTS_DIR"/update-all-users-project.sh

echo "Running Task plugin tests ..."
untrusted_user="untrusted_user"
ssh -p 29418 "$GERRIT_HOST" gerrit create-account "$untrusted_user" --full-name "$untrusted_user" \
      --email "$untrusted_user"@example.com --ssh-key - < ~/.ssh/id_rsa.pub

RESULT=0

"$USER_RUN_TESTS_DIR"/../../check_task_statuses.sh \
    --server "$GERRIT_HOST" --untrusted-user "$untrusted_user" || RESULT=1

"$USER_RUN_TESTS_DIR"/../../check_task_visibility.sh --server "$GERRIT_HOST" \
    --non-secret-user "$untrusted_user" || RESULT=1

exit $RESULT