#!/usr/bin/env bash

readlink --canonicalize / &> /dev/null || readlink() { greadlink "$@" ; } # for MacOS
MYDIR=$(dirname -- "$(readlink -f -- "$0")")
ARTIFACTS=$MYDIR/gerrit/artifacts

die() { echo -e "\nERROR: $@" ; kill $$ ; exit 1 ; } # error_message

progress() { # message cmd [args]...
    local message=$1 ; shift
    echo -n "$message"
    "$@" &
    local pid=$!
    while kill -0 $pid 2> /dev/null ; do
        echo -n "."
        sleep 2
    done
    echo
    wait "$pid"
}

usage() { # [error_message]
    local prog=$(basename "$0")

    cat <<-EOF
Usage:
    "$prog" --gerrit-war|-g <Gerrit WAR URL or file path> \
        --task-plugin-jar|-t <task plugin URL or file path>

    --help|-h
    --gerrit-war|-g            gerrit WAR URL (or) the file path in local workspace
                               eg: file:///path/to/source/file
    --task-plugin-jar|-t       task plugin JAR URL (or) the file path in local workspace
                               eg: file:///path/to/source/file

EOF

    [ -n "$1" ] && echo -e "\nERROR: $1" && exit 1
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
    local build_args=(--build-arg GERRIT_WAR="/artifacts/gerrit.war" \
        --build-arg TASK_PLUGIN_JAR="/artifacts/task.jar" \
        --build-arg UID="$(id -u)" --build-arg GID="$(id -g)")
    docker-compose "${COMPOSE_ARGS[@]}" build "${build_args[@]}" --quiet
    rm -r "$ARTIFACTS"
}

run_task_plugin_tests() {
    docker-compose "${COMPOSE_ARGS[@]}" up --detach
    local runtests_container=$(docker ps | grep "$PROJECT_NAME"_run_tests | \
        awk '{print $1}')
    docker exec --user=gerrit_admin "$runtests_container" \
            '/task/test/docker/run_tests/start.sh'
}

cleanup() {
    docker-compose "${COMPOSE_ARGS[@]}" down -v --rmi local 2>/dev/null
}

while (( "$#" )) ; do
    case "$1" in
        --help|-h)                usage ;;
        --gerrit-war|-g)          shift ; GERRIT_WAR=$1 ;;
        --task-plugin-jar|-t)     shift ; TASK_PLUGIN_JAR=$1 ;;
        *)                        usage "invalid argument $1" ;;
    esac
    shift
done
[ -n "$GERRIT_WAR" ] || usage "'--gerrit-war' not set"
[ -n "$TASK_PLUGIN_JAR" ] || usage "'--task-plugin-jar' not set"
PROJECT_NAME="task_$$"
COMPOSE_YAML="$MYDIR/docker-compose.yaml"
COMPOSE_ARGS=(--project-name "$PROJECT_NAME" -f "$COMPOSE_YAML")
check_prerequisite
progress "fetching artifacts" fetch_artifacts
progress "Building docker images" build_images
run_task_plugin_tests ; RESULT=$?
cleanup

exit "$RESULT"
