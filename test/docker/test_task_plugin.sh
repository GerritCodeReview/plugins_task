#!/usr/bin/env bash

readlink -f / &> /dev/null || readlink() { greadlink "$@" ; } # for MacOS
MYDIR=$(dirname -- "$(readlink -f -- "$0")")
MYPROG=$(basename -- "$0")
source "$MYDIR/lib_options.sh"
ARTIFACTS=$MYDIR/gerrit/artifacts

die() { echo -e "\nERROR: $@" ; kill $$ ; exit 1 ; } # error_message

progress() { # message cmd [args]...
    message=$1 ; shift
    echo -n "$message"
    "$@" &
    pid=$!
    while kill -0 $pid 2> /dev/null ; do
        echo -n "."
        sleep 2
    done
    echo
    wait "$pid"
}

usage() { # [error_message]
    cat <<-EOF
Usage:
    "$MYPROG" --gerrit-war|-g <Gerrit WAR URL or file path> \
        --task-plugin-jar|-t <task plugin URL or file path> [--result-callback <cmd>]
        [--results-dir <dir>]

                    OR

    "$MYPROG" --gerrit-docker-image|-i <link> [--result-callback <cmd>] [--results-dir <dir>]

    --help|-h
    --gerrit-war|-g            gerrit WAR URL (or) the file path in local workspace
                               eg: file:///path/to/source/file
    --task-plugin-jar|-t       task plugin JAR URL (or) the file path in local workspace
                               eg: file:///path/to/source/file
    --gerrit-docker-image|-i   gerrit docker image link
    --results-dir              Path to a directory to save test results
    --result-callback          command to run at the time of test result
                               generation. Following args will be passed to
                               the callback <test_script> <test> PASS|FAIL
    --preserve                 To preserve the docker setup for debugging
EOF

    [ -n "$1" ] && die "$1"
    exit 0
}

check_prerequisite() {
    docker --version > /dev/null || die "docker is not installed"
    docker-compose --version > /dev/null || die "docker-compose is not installed"
}

fetch_artifact() { # source_location output_path
    curl --silent --fail --netrc "$1" --output "$2" --create-dirs || die "unable to fetch $1"
}

fetch_artifacts() {
    fetch_artifact "$GERRIT_WAR" "$ARTIFACTS/gerrit.war"
    fetch_artifact "$TASK_PLUGIN_JAR" "$ARTIFACTS/task.jar"
}

build_images() {
    local build_args=()
    if [ -n "$GERRIT_DOCKER_IMAGE" ] ; then
        build_args+=(--build-arg GERRIT_DOCKER_IMAGE="$GERRIT_DOCKER_IMAGE")
        COMPOSE_ARGS+=(-f "$MYDIR/docker-compose-override.yaml")
    else
       build_args+=(--build-arg GERRIT_WAR="/artifacts/gerrit.war" \
           --build-arg TASK_PLUGIN_JAR="/artifacts/task.jar")
    fi
    if [ -n "$RESULT_CALLBACK" ] && [ -n "$RESULTS_DIR" ] ; then
        build_args+=(--build-arg UID="$(id -u)" --build-arg GID="$(id -g)")
        COMPOSE_ARGS+=(-f "$MYDIR/docker-compose-result-callback-override.yaml")
    fi
    docker-compose "${COMPOSE_ARGS[@]}" build "${build_args[@]}" --quiet
    rm -rf "$ARTIFACTS"
}

run_task_plugin_tests() {
    docker-compose "${COMPOSE_ARGS[@]}" up --detach
    docker-compose "${COMPOSE_ARGS[@]}" exec -T --user=gerrit_admin \
        run_tests task/test/docker/run_tests/start.sh
}

retest() {
    docker-compose "${COMPOSE_ARGS[@]}" exec -T --user=gerrit_admin \
        run_tests task/test/docker/run_tests/start.sh retest
    RESULT=$?
    cleanup
}

get_run_test_container() {
    docker-compose "${COMPOSE_ARGS[@]}" ps | grep run_tests | awk '{ print $1 }'
}

cleanup() {
    if [ "$PRESERVE" = "true" ] ; then
        echo "Preserving the following docker setup"
        docker-compose "${COMPOSE_ARGS[@]}" ps
        echo ""
        echo "To exec into runtests container, use following command:"
        echo "docker exec -it $(get_run_test_container) /bin/bash"
        echo ""
        echo "Run the following command to bring down the setup:"
        echo "docker-compose ${COMPOSE_ARGS[@]} down -v --rmi local"
        echo ""
        options_prefix "COMPOSE_ARGS" "--compose-arg"
        echo "Use command below to re run tests after making changes to test scripts"
        echo " $MYDIR/$MYPROG --retest --preserve ${COMPOSE_ARGS[@]}"
    else
        docker-compose "${COMPOSE_ARGS[@]}" down -v --rmi local 2>/dev/null
    fi
}

PRESERVE="false"
RETEST="false"
COMPOSE_ARGS=()
while (( "$#" )) ; do
    case "$1" in
        --help|-h)                usage ;;
        --gerrit-war|-g)          shift ; GERRIT_WAR=$1 ;;
        --task-plugin-jar|-t)     shift ; TASK_PLUGIN_JAR=$1 ;;
        --gerrit-docker-image|-i) shift ; GERRIT_DOCKER_IMAGE=$1 ;;
        --result-callback)        shift ; export RESULT_CALLBACK=$1 ;;
        --results-dir)            shift ; export RESULTS_DIR=$1 ;;
        --preserve)               PRESERVE="true" ;;
        --retest)                 RETEST="true" ;;
        --compose-arg)            shift ; COMPOSE_ARGS+=("$1") ;;
        *)                        usage "invalid argument $1" ;;
    esac
    shift
done

[ "$RETEST" = "true" ] && { retest ; exit "$RESULT" ; }

if [ -z "$GERRIT_DOCKER_IMAGE"  ] ; then
    [ -n "$GERRIT_WAR" ] || usage "'--gerrit-war' not set"
    [ -n "$TASK_PLUGIN_JAR" ] || usage "'--task-plugin-jar' not set "
fi

PROJECT_NAME=task_$$
COMPOSE_YAML=$MYDIR/docker-compose.yaml
COMPOSE_ARGS=(--project-name "$PROJECT_NAME" -f "$COMPOSE_YAML")
check_prerequisite
[ -n "$GERRIT_DOCKER_IMAGE" ] || progress "Fetching artifacts" fetch_artifacts
progress "Building docker images" build_images
run_task_plugin_tests ; RESULT=$?
cleanup

exit "$RESULT"
