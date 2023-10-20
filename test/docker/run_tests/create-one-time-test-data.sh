#!/usr/bin/env bash

die() { echo -e "\nERROR:" "$@" ; kill $$ ; exit 1 ; } # error_message

q() { "$@" > /dev/null 2>&1 ; } # cmd [args...]  # quiet a command

gssh() { ssh -x -p "$SSH_PORT" "$GERRIT_HOST" gerrit "$@" ; } # run a gerrit ssh command

create_test_users_and_group() {
    echo "Creating test users and group ..."
    gssh create-account "$NON_SECRET_USER" --full-name "$NON_SECRET_USER" \
           --email "$NON_SECRET_USER"@example.com --ssh-key - < ~/.ssh/id_rsa.pub

    gssh create-account "$UNTRUSTED_USER" --full-name "$UNTRUSTED_USER" \
        --email "$UNTRUSTED_USER"@example.com --ssh-key - < ~/.ssh/id_rsa.pub

    gssh create-group "Visible-All-Projects-Config" --member "$NON_SECRET_USER"

    local secret_user=$USER
    gssh create-group "$NON_SECRET_GROUP_NAME_WITHOUT_SPACE" \
        --member "$NON_SECRET_USER" --member "$secret_user"
    gssh create-group "\"$NON_SECRET_GROUP_NAME_WITH_SPACE\"" \
        --member "$NON_SECRET_USER" --member "$secret_user"
    gssh create-group "$SECRET_GROUP_NAME" --member "$secret_user"
}

setup_all_projects_repo() {
    echo "Updating All-Projects repo ..."

    local uuid=$(gssh ls-groups -v | awk '-F\t' '$1 == "Visible-All-Projects-Config" {print $2}')
    ( cd "$WORKSPACE"
      q git clone ssh://"$GERRIT_HOST":"$SSH_PORT"/All-Projects allProjects
      cd allProjects
      q git fetch origin refs/meta/config ; q git checkout FETCH_HEAD
      echo -e "$uuid\tVisible-All-Projects-Config" >> groups
      git config -f "project.config" \
          --add access."refs/meta/config".read "group Visible-All-Projects-Config"
      git config -f "project.config" \
          --add capability.viewTaskPaths "group Administrators"
#     After migrating to version 3.5, it is no longer feasible to assign read permissions to
#     Administrators for another user's ref. To address this, add the 'accessDatabase' capability,
#     allowing admins to read the user ref of other users
      git config -f "project.config" \
                --add capability.accessDatabase "group Administrators"
      q git add . && q git commit -m "project config update"
      q git push origin HEAD:refs/meta/config
    )
}

SSH_PORT=29418
USER_RUN_TESTS_DIR="$USER_HOME"/"$RUN_TESTS_DIR"
while (( "$#" )) ; do
   case "$1" in
       --non-secret-user)                 shift ; NON_SECRET_USER="$1" ;;
       --untrusted-user)                  shift ; UNTRUSTED_USER="$1" ;;
       --non-secret-group-without-space)  shift ; NON_SECRET_GROUP_NAME_WITHOUT_SPACE="$1" ;;
       --non-secret-group-with-space)     shift ; NON_SECRET_GROUP_NAME_WITH_SPACE="$1" ;;
       --secret-group)                    shift ; SECRET_GROUP_NAME="$1" ;;
       *)                                 die "invalid argument '$1'" ;;
   esac
   shift
done

[ -z "$NON_SECRET_USER" ] && die "non-secret-user not set"
[ -z "$UNTRUSTED_USER" ] && die "untrusted-user not set"
[ -z "$NON_SECRET_GROUP_NAME_WITHOUT_SPACE" ] && die "non-secret-group-without-space not set"
[ -z "$NON_SECRET_GROUP_NAME_WITH_SPACE" ] && die "non-secret-group-with-space not set"
[ -z "$SECRET_GROUP_NAME" ] && die "secret-group not set"

"$USER_RUN_TESTS_DIR"/create-test-project-and-changes.sh
"$USER_RUN_TESTS_DIR"/update-all-users-project.sh
create_test_users_and_group
setup_all_projects_repo
