#!/usr/bin/env bash

echo "Updating All-Users project ..."

cd "$WORKSPACE" && git clone ssh://"$GERRIT_HOST":29418/All-Users allusers && cd allusers
git fetch origin refs/meta/config && git checkout FETCH_HEAD
git config -f project.config access."refs/users/*".read "group Administrators"
git config -f project.config access."refs/users/*".push "group Administrators"
git config -f project.config access."refs/users/*".create "group Administrators"
git add . && git commit -m "project config update" && git push origin HEAD:refs/meta/config
