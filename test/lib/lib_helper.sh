#!/usr/bin/env bash
#
# Copyright (C) 2021 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# ---- TEST RESULTS ----
result() { # test [error_message]
    local result=$?
    if [ $result -eq 0 ] ; then
        echo "PASSED - $1 test"
    else
        echo "*** FAILED *** - $1 test"
        RESULT=$result
        [ $# -gt 1 ] && echo "$2"
    fi
}

# output must match expected to pass
result_out() { # test expected actual
    local name=$1 expected=$2 actual=$3

    [ "$expected" = "$actual" ]
    result "$name" "$(diff <(echo "$expected") <(echo "$actual"))"
}

result_root() { # group root
    local name="$1 - $(echo "$2" | sed -es'/Root //')"
    result_out "$name" "${EXPECTED_ROOTS[$2]}" "${OUTPUT_ROOTS[$2]}"
}

# -------- Git Config

config() { git config -f "$CONFIG" "$@" ; } # [args]...
config_section_keys() { # section > keys ...
    # handlers.handler-filter filter.sh -> handler-filter
    config -l --name-only |\
        grep "^$1\." | \
        sed -es"/^$1\.//;s/\..*$//" |\
        awk '$0 != prev ; {prev = $0}'
}

# -------- Pre JSON --------
#
# pre_json is a "templated json" used in the test docs to express test results. It looks
# like json but has some extra comments to express when a certain output should be used.
# These comments look like: "# Only Test Suite: <suite>"
#

remove_suites() { # suites... < pre_json > json
    grep -vE "# Only Test Suite: ($(echo "$@" | sed "s/ /|/g"))" | \
         sed -e's/# Only Test Suite:.*$//; s/# Test Suite:.*$//; s/ *$//'
}

# pre_json is a "templated json" used in the test docs to express test results. It looks
# like json but has some extra comments to express when a certain output should be used.
# These comments look like: "# Test Suite: <suite>[, <suite>][, <suite>]..."
#

keep_suites() { # suites... < pre_json > json
    grep -E "# Test Suite: (.*, )?($(echo "$@" | sed "s/ /|/g"))(, .*)?$" | \
         sed -e's/# Only Test Suite:.*$//; s/# Test Suite:.*$//; s/ *$//'
}

remove_not_suite() { remove_suites !"$1" ; } # suite < pre_json > json

# -------- Test Doc Format --------
#
# Test Doc Format has intermixed git config task definitions with json roots. This
# makes it easy to define tests close to their outputs. Be aware that all of the
# config will get consolidated into a single file, so non root config will be shared
# amongst all the roots.
#

# Sample Test Doc for 2 roots:
#
# [root "Root PASS"]
#   pass = True
#
# {
#    "applicable" : true,
#    "hasPass" : true,
#    "name" : "Root PASS",
#    "status" : "PASS"
# }
#
# [root "Root FAIL"]
#   fail = True
#
# {
#    <other root>
# }

# Strip the json from Test Doc formatted text. For the sample above, the output would be:
#
# [root "Root PASS"]
#   pass = True
#
# [root "Root FAIL"]
#   fail = True
# ...
#
testdoc_2_cfg() { awk '/^\{/,/^$/ { next } ; 1' ; } # testdoc_format > task_config

# Strip the git config from Test Doc formatted text. For the sample above, the output would be:
#
# { "plugins" : [
#     { "name" : "task",
#       "roots" : [
#         {
#           "applicable" : true,
#           "hasPass" : true,
#           "name" : "Root PASS",
#           "status" : "PASS"
#        },
#        {
#           <other root>
#        },
#    ...
# }
testdoc_2_pjson() { # < testdoc_format > pjson_task_roots
    awk 'BEGIN { print "{ \"plugins\" : [ { \"name\" : \"task\", \"roots\" : [" }; \
         /^\{/  { open=1 }; \
         open && end { print "}," ; end=0 }; \
         /^\}/  { open=0 ; end=1 }; \
         open; \
         END   { print "}]}]}" }'
}

# ---- JSON PARSING ----

json_pp() { # < json > json
    python -c "import sys, json; \
            print json.dumps(json.loads(sys.stdin.read()), indent=3, \
            separators=(',', ' : '), sort_keys=True)"
}

json_val_by_key() {  # json key > value
    echo "$1" | jq -r --arg key "$2" '.[$key] // empty'
}
# --------

gssh() {  # [-l user] cmd [args]...
    local user_args=()
    [ "-l" = "$1" ] && { user_args=("-l" "$2") ; shift 2 ; }
    ssh -x -p "$PORT" "${user_args[@]}" "$SERVER" gerrit "$@"
}

q() { "$@" > /dev/null 2>&1 ; } # cmd [args...]  # quiet a command

gen_change_id() { echo "I$(uuidgen | sha1sum | awk '{print $1}')"; } # > change_id

commit_message() { printf "$1 \n\nChange-Id: $2" ; } # message change-id > commit_msg

err() { echo "ERROR: $1" >&2 ; exit 1 ; }

# Run a test setup command quietly, exit on failure
q_setup() { local out ; out=$("$@" 2>&1) || err "$out" ; } # cmd [args...]

ensure() { "$@" || err "$1 results are not valid" ; } # cmd [args]... < data > data

set_change() { # change_json
    { CHANGE=("$(json_val_by_key "$1" number)" \
        "$(json_val_by_key "$1" id)" \
        "$(json_val_by_key "$1" project)" \
        "refs/heads/$(json_val_by_key "$1" branch)" \
        "$(json_val_by_key "$1" status)" \
        "$(json_val_by_key "$1" topic)") ; } 2> /dev/null
}

# change_token change_number change_id project branch status topic < templated_txt > change_txt
replace_change_properties() {
    sed -e "s|_change$1_number|$2|g" \
        -e "s|_change$1_id|$3|g" \
        -e "s|_change$1_project|$4|g" \
        -e "s|_change$1_branch|$5|g" \
        -e "s|_change$1_status|$6|g" \
        -e "s|_change$1_topic|$7|g"
}

replace_default_changes() {
    replace_change_properties "1" "${CHANGE1[@]}" | replace_change_properties "2" "${CHANGE2[@]}"
}

replace_groups() { # < text_with_groups > test_with_expanded_groups
    local text="$(< /dev/stdin)"
    for placeholder in "${!GROUP_EXPANDED_BY_PLACEHOLDER[@]}" ; do
        text="${text//"$placeholder"/${GROUP_EXPANDED_BY_PLACEHOLDER["$placeholder"]}}"
    done
    echo "$text"
}

get_group_uuid() { # group_name > group_uuid
    gssh ls-groups -v | awk '-F\t' '$1 == "'"$1"'" {print $2}'
}

get_sharded_group_uuid() { # group_name > sharded_group_uuid
    local group_id=$(get_group_uuid "$1")
    echo "${group_id:0:2}/$group_id"
}

replace_users() { # < text_with_users > test_with_expanded_users
  local text="$(< /dev/stdin)"
  for user in "${!USERS[@]}" ; do
    text="${text//"$user"/${USERS["$user"]}}"
  done
  echo "$text"
}

replace_user() { # < text_with_testuser > text_with_$USER
    sed -e"s/testuser/$USER/"
}

get_user_ref() { # username > refs/users/<accountidshard>/<accountid>
    local user_account_id="$(curl --netrc --silent "http://$SERVER:$HTTP_PORT/a/accounts/$1" | \
    sed -e '1!b' -e "/^)]}'$/d" | jq ._account_id)"
    echo "refs/users/${user_account_id:(-2)}/$user_account_id"
}

replace_user_refs() { # < text_with_user_refs > test_with_expanded_user_refs
    local text="$(< /dev/stdin)"
    for user in "${!USER_REFS[@]}" ; do
        text="${text//"$user"/${USER_REFS["$user"]}}"
    done
    echo "$text"
}

replace_tokens() { # < text > text with replacing all tokens(changes, user)
    replace_default_changes | replace_user_refs | replace_user | replace_groups
}

strip_non_applicable() { ensure "$MYDIR"/strip_non_applicable.py ; } # < json > json
strip_non_invalid() { ensure "$MYDIR"/strip_non_invalid.py ; } # < json > json

define_jsonByRoot() { # task_plugin_ouptut > jsonByRoot_array_definition
    local record root=''
    local -A jsonByRoot
    while IFS= read -r -d '' record ; do
        if [ -z "$root" ] ; then
            root=$record
        else
            jsonByRoot[$root]=$record
            root=''
        fi
    done < <(python -c "if True: # NOP to start indent
        import sys, json

        roots=json.loads(sys.stdin.read())['plugins'][0]['roots']
        for root in roots:
            root_json = json.dumps(root, indent=3, separators=(',', ' : '), sort_keys=True)
            sys.stdout.write(root['name'] + '\x00' + root_json + '\x00')"
    )

    local def=$(declare -p jsonByRoot)
    echo "${def#*=}" # declare -A jsonByRoot='(...)' > '(...)'
}

get_plugins() { # < change_json > plugins_json
    jq --indent 3 --sort-keys '{plugins: .plugins}'
}

example() { # doc example_num > text_for_example_num
    echo "$1" | awk '/```/{Q++;E=(Q+1)/2};E=='"$2" | grep -v '```'
}

get_change_num() { # < gerrit_push_response > changenum
    local url=$(awk '$NF ~ /\[NEW\]/ { print $2 }')
    echo "${url##*\/}" | tr -d -c '[:digit:]'
}

install_changeid_hook() { # repo
    local hook=$(git rev-parse --git-dir)/hooks/commit-msg
    scp -p -P "$PORT" "$SERVER":hooks/commit-msg "$hook"
    chmod +x "$hook"
}

setup_repo() { # repo remote ref [--initial-commit]
    local repo=$1 remote=$2 ref=$3 init=$4
    git init "$repo"
    (
        cd "$repo"
        install_changeid_hook "$repo"
        git fetch "$remote" "$ref"
        if ! git checkout FETCH_HEAD ; then
            if [ "$init" = "--initial-commit" ] ; then
                git commit --allow-empty -a -m "Initial Commit"
            fi
        fi
    )
}

update_repo() { # repo remote ref
    local repo=$1 remote=$2 ref=$3
    (
        cd "$repo"
        git add .
        git commit -m 'Testing task plugin'
        git push "$remote" HEAD:"$ref"
    )
}

create_repo_change() { # repo remote ref [change_id] > change_num
    local repo=$1 remote=$2 ref=$3 change_id=$4 msg="Test change"
    (
        q cd "$repo"
        uuidgen > file
        q git add .
        [ -n "$change_id" ] && msg=$(commit_message "$msg" "$change_id")
        q git commit -m "$msg"
        git push "$remote" HEAD:"refs/for/$ref" 2>&1 | get_change_num
    )
}

query() {  # [-l user] query > json lines
  local user_args=()
  [ "-l" = "$1" ] && { user_args=("-l" "$2") ; shift 2 ; }
  gssh "${user_args[@]}" query "$@" --format json
}

# N < json lines > changeN_json
change_plugins() { awk "NR==$1" | get_plugins | json_pp ; }

results_suite() { # name expected_file plugins_json
    local name=$1 expected=$2 actual=$3

    local -A EXPECTED_ROOTS=$(define_jsonByRoot < "$expected")
    local -A OUTPUT_ROOTS=$(echo "$actual" | define_jsonByRoot)

    local out root
    echo "$ROOTS" | while read root ; do
        result_root "$name" "$root"
    done
    out=$(diff "$expected" <(echo "$actual") | head -15)
    [ -z "$out" ]
    result "$name - Full Test Suite" "$out"
}
