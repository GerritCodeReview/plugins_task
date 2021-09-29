#!/usr/bin/env bash

USER_RUN_TESTS_DIR="$USER_HOME"/"$RUN_TESTS_DIR"
cp -r /task "$USER_HOME"/

if [ "$1" = "retest" ] ; then
    cd "$USER_RUN_TESTS_DIR"/../../ && ./check_task_statuses.sh "$GERRIT_HOST"
    exit $?
fi

./"$USER_RUN_TESTS_DIR"/wait-for-it.sh "$GERRIT_HOST":29418 -t 60 -- echo "gerrit is up"

echo "Creating a default user account ..."

cat "$USER_HOME"/.ssh/id_rsa.pub | ssh -p 29418 -i /server-ssh-key/ssh_host_rsa_key \
  "Gerrit Code Review@$GERRIT_HOST" suexec --as "admin@example.com" -- gerrit create-account \
     --ssh-key - --email "gerrit_admin@localdomain"  --group "Administrators" "gerrit_admin"

./"$USER_RUN_TESTS_DIR"/create-test-project-and-changes.sh
./"$USER_RUN_TESTS_DIR"/update-all-users-project.sh

echo "Running Task plugin tests ..."
cd "$USER_RUN_TESTS_DIR"/../../ && ./check_task_statuses.sh "$GERRIT_HOST"
