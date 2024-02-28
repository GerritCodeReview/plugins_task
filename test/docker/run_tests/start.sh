#!/usr/bin/env bash

die() { echo "ERROR: $1" >&2 ; exit 1 ; } # errormsg

is_plugin_loaded() { # plugin_name
    ssh -p 29418 "$GERRIT_HOST" gerrit plugin ls | awk '{print $1}' | grep -q "^$1\$"
}

USER_RUN_TESTS_DIR="$USER_HOME"/"$RUN_TESTS_DIR"
mkdir "$USER_HOME"/task && cp -r /task/{src,test} "$USER_HOME"/task

RETEST=false
while (( "$#" )) ; do
   case "$1" in
       --retest)                          RETEST="true" ;;
       --root-config-project)             shift ; ROOT_CONFIG_PRJ=$1 ;;
       --root-config-branch)              shift ; ROOT_CONFIG_BRANCH=$1 ;;
       *)                                 die "invalid argument '$1'" ;;
   esac
   shift
done
[ -z "$ROOT_CONFIG_PRJ" ] && ROOT_CONFIG_PRJ=All-Projects
[ -z "$ROOT_CONFIG_BRANCH" ] && ROOT_CONFIG_BRANCH=refs/meta/config

if [ "$RETEST" = "true" ] ; then
    cd "$USER_RUN_TESTS_DIR"/../../ && \
        ./check_task_statuses.sh "$GERRIT_HOST" \
            --root-config-project "$ROOT_CONFIG_PRJ" --root-config-branch "$ROOT_CONFIG_BRANCH"
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
GROUP_NAME_WITHOUT_SPACE="test.group"
GROUP_NAME_WITH_SPACE="test group"
SECRET_GROUP="private_group"
"$USER_RUN_TESTS_DIR"/create-one-time-test-data.sh --non-secret-user "$NON_SECRET_USER" \
    --root-config-project "$ROOT_CONFIG_PRJ" --root-config-branch "$ROOT_CONFIG_BRANCH" \
    --untrusted-user "$UNTRUSTED_USER" --non-secret-group-without-space "$GROUP_NAME_WITHOUT_SPACE" \
    --non-secret-group-with-space "$GROUP_NAME_WITH_SPACE" --secret-group "$SECRET_GROUP"

echo "Running Task plugin tests ..."

RESULT=0

"$USER_RUN_TESTS_DIR"/../../check_task_statuses.sh \
    --server "$GERRIT_HOST" --non-secret-user "$NON_SECRET_USER" \
    --root-config-project "$ROOT_CONFIG_PRJ" --root-config-branch "$ROOT_CONFIG_BRANCH" \
    --untrusted-user "$UNTRUSTED_USER" --non-secret-group-without-space "$GROUP_NAME_WITHOUT_SPACE" \
    --non-secret-group-with-space "$GROUP_NAME_WITH_SPACE" || RESULT=1

"$USER_RUN_TESTS_DIR"/../../check_task_visibility.sh --server "$GERRIT_HOST" \
    --root-config-project "$ROOT_CONFIG_PRJ" --root-config-branch "$ROOT_CONFIG_BRANCH" \
    --non-secret-user "$NON_SECRET_USER" --non-secret-group "$GROUP_NAME_WITHOUT_SPACE" \
    --secret-group "$SECRET_GROUP" || RESULT=1

exit $RESULT
