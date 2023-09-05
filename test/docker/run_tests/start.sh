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

./"$USER_RUN_TESTS_DIR"/wait-for-it.sh "$GERRIT_HOST":29418 \
    -t -60 || die "Failed to start gerrit"
echo "gerrit is up"

echo "Update admin account ..."

cat "$USER_HOME"/.ssh/id_rsa.pub | ssh -p 29418 -i /server-ssh-key/ssh_host_rsa_key \
    "Gerrit Code Review@$GERRIT_HOST" suexec --as "admin@example.com" -- gerrit set-account \
    admin --add-ssh-key -

PASSWORD=$(uuidgen)
echo "machine $GERRIT_HOST login $USER password $PASSWORD" > "$USER_HOME"/.netrc
ssh -p 29418 "$GERRIT_HOST" gerrit set-account --http-password "$PASSWORD" "$USER"

is_plugin_loaded "task" || die "Task plugin is not installed"

NON_SECRET_USER="non_secret_user"
UNTRUSTED_USER="untrusted_user"
"$USER_RUN_TESTS_DIR"/create-one-time-test-data.sh --non-secret-user "$NON_SECRET_USER" \
    --untrusted-user "$UNTRUSTED_USER"

echo "Running Task plugin tests ..."

cd "$USER_RUN_TESTS_DIR"/../../ && ./check_task_statuses.sh \
    --server "$GERRIT_HOST" --non-secret-user "$NON_SECRET_USER" \
    --untrusted-user "$UNTRUSTED_USER"
