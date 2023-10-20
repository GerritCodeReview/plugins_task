#!/usr/bin/env bash

echo "Updating All-Users project ..."

cd "$WORKSPACE" && git clone ssh://"$GERRIT_HOST":29418/All-Users allusers && cd allusers
git fetch origin refs/meta/config && git checkout FETCH_HEAD
git config -f project.config access."refs/users/*".push "group Administrators"

git config -f project.config access.'refs/users/${shardeduserid}'.read "group Registered Users"
git config -f project.config access.'refs/users/${shardeduserid}'.push "group Registered Users"
git config -f project.config access.'refs/users/${shardeduserid}'.create "group Registered Users"
git config -f "project.config" \
   access."refs/*".read "deny group Anonymous Users"
echo -e "global:Registered-Users\tRegistered Users" >> groups
echo -e "global:Anonymous-Users\tAnonymous Users" >> groups
git add . && git commit -m "project config update" && git push origin HEAD:refs/meta/config
