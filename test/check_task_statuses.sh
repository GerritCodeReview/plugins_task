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

# Usage:
# All-Projects.git - must have 'Push' rights on refs/meta/config

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
         sed -e's/# Only Test Suite:.*$//; s/ *$//'
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

json_val_by() { # json index|'key' > value
    echo "$1" | python -c "import json,sys;print json.load(sys.stdin)[$2]"
}
json_val_by_key() { json_val_by "$1" "'$2'" ; }  # json key > value

# --------

gssh() {  # [-l user] cmd [args]...
    local user_args=()
    [ "-l" = "$1" ] && { user_args=("-l" "$2") ; shift 2 ; }
    ssh -x -p "$PORT" "${user_args[@]}" "$SERVER" gerrit "$@"
}

q() { "$@" > /dev/null 2>&1 ; } # cmd [args...]  # quiet a command

gen_change_id() { echo "I$(uuidgen | openssl dgst -sha1 -binary | xxd -p)"; } # > change_id

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
    replace_default_changes | replace_user_refs | replace_user
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
    python -c "import sys, json; \
        plugins={}; plugins['plugins']=json.loads(sys.stdin.read())['plugins']; \
        print json.dumps(plugins, indent=3, separators=(',', ' : '), sort_keys=True)"
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

test_2generated() { # name task_args...
    local name=$1 ; shift
    local out=$(query "$@")
    results_suite "$name" "$EXPECTED.$name" "$(echo "$out" | change_plugins 1)"
    results_suite "$name 2nd change" "$EXPECTED.$name"2 "$(echo "$out" | change_plugins 2)"
}

test_generated() { # name [-l query_user] task_args...
    local name=$1 ; shift
    query "$@" | change_plugins 1 > "$ACTUAL.$name"
    results_suite "$name" "$EXPECTED.$name" "$( < "$ACTUAL.$name")"
}

usage() { # [error_message]
    cat <<-EOF
Usage:
    "$MYPROG" --server <gerrit_host> --non-secret-user <non-secret user>
    --untrusted-user <untrusted user>

    --help|-h                     help text
    --server|-s                   gerrit host
    --non-secret-user             user who don't have permission
                                  to view other user refs.
    --untrusted-user              user who doesn't have permission
                                  to view refs/meta/config ref on All-Projects repo
EOF

    [ -n "$1" ] && { echo "Error: $1" ; exit 1 ; }
    exit 0
}

readlink -f / &> /dev/null || readlink() { greadlink "$@" ; } # for MacOS
MYDIR=$(dirname -- "$(readlink -f -- "$0")")
MYPROG=$(basename -- "$0")
DOCS=$MYDIR/.././src/main/resources/Documentation/test
OUT=$MYDIR/../target/tests

ALL=$OUT/All-Projects
ALL_TASKS=$ALL/task

USERS=$OUT/All-Users
USER_TASKS=$USERS/task

EXPECTED=$OUT/expected
ACTUAL=$OUT/actual

ROOT_CFG=$ALL/task.config
COMMON_CFG=$ALL_TASKS/common.config
USER_COMMON_CFG=$USER_TASKS/common.config
INVALIDS_CFG=$ALL_TASKS/invalids.config
USER_SPECIAL_CFG=$USER_TASKS/special.config

# --- Args ----

while (( "$#" )) ; do
    case "$1" in
        --help|-h)                usage ;;
        --server|-s)              shift ; SERVER=$1 ;;
        --non-secret-user)        shift ; NON_SECRET_USER=$1 ;;
        --untrusted-user)         shift ; UNTRUSTED_USER=$1 ;;
        *)                        usage "invalid argument $1" ;;
    esac
    shift
done

[ -z "$SERVER" ] && usage "You must specify --server"
[ -z "$NON_SECRET_USER" ] && usage "You must specify --non-secret-user"
[ -z "$UNTRUSTED_USER" ] && usage "You must specify --untrusted-user"


PORT=29418
HTTP_PORT=8080
PROJECT=test
BRANCH=master
REMOTE_ALL=ssh://$SERVER:$PORT/All-Projects
REMOTE_USERS=ssh://$SERVER:$PORT/All-Users
REMOTE_TEST=ssh://$SERVER:$PORT/$PROJECT

REF_ALL=refs/meta/config
REF_USERS=refs/users/self

CONFIG=$ROOT_CFG

declare -A USER_REFS
USER_REFS["{testuser_user_ref}"]="$(get_user_ref "$USER")"

mkdir -p "$OUT" "$ALL_TASKS" "$USER_TASKS"

q_setup setup_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
q_setup setup_repo "$USERS" "$REMOTE_USERS" "$REF_USERS" --initial-commit
q_setup setup_repo "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH"

changes=$(gssh query "status:open limit:2" --format json)
set_change "$(echo "$changes" | awk 'NR==1')" ; CHANGE1=("${CHANGE[@]}")
set_change "$(echo "$changes" | awk 'NR==2')" ; CHANGE2=("${CHANGE[@]}")

DOC_STATES=$(replace_tokens < "$DOCS/task_states.md")
DOC_PREVIEW=$(replace_tokens < "$DOCS/preview.md")
DOC_PATHS=$(replace_tokens < "$DOCS/paths.md")

example "$DOC_STATES" 2 | testdoc_2_cfg > "$ROOT_CFG"
example "$DOC_STATES" 3 > "$COMMON_CFG"
example "$DOC_STATES" 3 > "$USER_COMMON_CFG"
example "$DOC_STATES" 4 > "$INVALIDS_CFG"
example "$DOC_STATES" 5 > "$USER_SPECIAL_CFG"

ROOTS=$(config_section_keys "root") || err "Invalid ROOTS"

q_setup update_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
q_setup update_repo "$USERS" "$REMOTE_USERS" "$REF_USERS"

change3_id=$(gen_change_id)
change4_id=$(gen_change_id)
change4_number=$(create_repo_change "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH" "$change4_id")
change3_number=$(create_repo_change "$OUT/$PROJECT" "$REMOTE_TEST" "$BRANCH" "$change3_id")

ex2_pjson=$(example "$DOC_STATES" 2 | testdoc_2_pjson)
all_pjson=$(echo "$ex2_pjson" | \
    replace_change_properties \
        "" \
        "$change3_number" \
        "$change3_id" \
        "$PROJECT" \
        "refs\/heads\/$BRANCH" \
        "NEW" \
        "")

all2_pjson=$(echo "$ex2_pjson" | \
    replace_change_properties \
        "" \
        "$change4_number" \
        "$change4_id" \
        "$PROJECT" \
        "refs\/heads\/$BRANCH" \
        "NEW" \
        "")

no_all_visible_json=$(echo "$all_pjson" | remove_suites "all" "!visible")
no_all_no_visible_json=$(echo "$all_pjson" | remove_suites "all" "visible")
no_all_visible2_json=$(echo "$all2_pjson" | remove_suites "all" "!visible")
no_all_no_visible2_json=$(echo "$all2_pjson" | remove_suites "all" "visible")

echo "$no_all_visible_json" | strip_non_applicable | \
    grep -v "\"applicable\" :" > "$EXPECTED".applicable

echo "$no_all_no_visible_json" | strip_non_applicable | \
    grep -v "\"applicable\" :" > "$EXPECTED".applicable-visibility

echo "$no_all_visible2_json" | strip_non_applicable | \
    grep -v "\"applicable\" :" > "$EXPECTED".applicable2

echo "$no_all_no_visible2_json" | strip_non_applicable | \
    grep -v "\"applicable\" :" > "$EXPECTED".applicable-visibility2

echo "$all_pjson" | remove_suites "!all" "!visible" | ensure json_pp > "$EXPECTED".all

echo "$no_all_visible_json" | strip_non_invalid > "$EXPECTED".invalid

strip_non_invalid < "$EXPECTED".applicable > "$EXPECTED".invalid-applicable


preview_pjson=$(echo "$DOC_PREVIEW" | testdoc_2_pjson)
echo "$preview_pjson" | remove_suites "invalid" "secret" | \
    ensure json_pp > "$EXPECTED".preview-non-secret
echo "$preview_pjson" | remove_suites "invalid" "!secret" | \
    ensure json_pp > "$EXPECTED".preview-admin
echo "$preview_pjson" | remove_suites "secret" "!invalid" | \
    strip_non_invalid > "$EXPECTED".preview-invalid

echo "$DOC_PREVIEW" | testdoc_2_cfg | replace_user > "$ROOT_CFG"
cnum=$(create_repo_change "$ALL" "$REMOTE_ALL" "$REF_ALL")
PREVIEW_ROOTS=$(config_section_keys "root")


RESULT=0
query="(change:$change3_number OR change:$change4_number) status:open"
test_2generated applicable --task--applicable "$query"
test_2generated applicable-visibility -l "$UNTRUSTED_USER" --task--applicable "$query"
test_generated all --task--all "$query"

test_generated invalid --task--invalid "$query"
test_generated invalid-applicable --task--applicable --task--invalid "$query"

ROOTS=$PREVIEW_ROOTS
test_generated preview-admin --task--preview "$cnum,1" --task--all "$query"
test_generated preview-non-secret -l "$NON_SECRET_USER" --task--preview "$cnum,1" --task--all "$query"
test_generated preview-invalid --task--preview "$cnum,1" --task--invalid "$query"

example "$DOC_PATHS" 1 | testdoc_2_cfg | replace_user > "$ROOT_CFG"
q_setup update_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
ROOTS=$(config_section_keys "root")
example "$DOC_PATHS" 1 | testdoc_2_pjson | ensure json_pp > "$EXPECTED".task-paths

test_generated task-paths --task--all --task--include-paths "$query"

example "$DOC_PATHS" 2 | testdoc_2_cfg > "$ROOT_CFG"
q_setup update_repo "$ALL" "$REMOTE_ALL" "$REF_ALL"
ROOTS=$(config_section_keys "root")
example "$DOC_PATHS" 2 | testdoc_2_pjson | ensure json_pp > "$EXPECTED".task-paths.non-secret
test_generated task-paths.non-secret -l "$NON_SECRET_USER" --task--all --task--include-paths "$query"

exit $RESULT
