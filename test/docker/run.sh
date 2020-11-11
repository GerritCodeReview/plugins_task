#!/usr/bin/env bash

readlink --canonicalize / &> /dev/null || readlink() { greadlink "$@" ; } # for MacOS
MYDIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
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
    local prog=$(basename -- "$0")

    cat <<-EOF
Usage:
    $prog [--task-plugin-jar|-t <FILE_PATH>] [--gerrit-war|-g <FILE_PATH>]

    This tool runs the plugin functional tests in a Docker environment built
    from the gerritcodereview/gerrit base Docker image.

    The task plugin JAR and optionally a Gerrit WAR are expected to be in the
    $ARTIFACTS dir;
    however, the --task-plugin-jar and --gerrit-war switches may be used as
    helpers to specify which files to copy there.

    Options:
    --help|-h
    --gerrit-war|-g            path to Gerrit WAR file
    --task-plugin-jar|-t       path to task plugin JAR file

EOF

    [ -n "$1" ] && echo -e "\nERROR: $1" && exit 1
    exit 0
}

check_prerequisite() {
    docker --version > /dev/null || die "docker is not installed"
    docker-compose --version > /dev/null || die "docker-compose is not installed"
}

build_images() {
    docker-compose "${COMPOSE_ARGS[@]}" build --quiet
}

run_task_plugin_tests() {
    docker-compose "${COMPOSE_ARGS[@]}" up --detach
    docker-compose "${COMPOSE_ARGS[@]}" exec -T --user=gerrit_admin run_tests \
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
PROJECT_NAME="task_$$"
COMPOSE_YAML="$MYDIR/docker-compose.yaml"
COMPOSE_ARGS=(--project-name "$PROJECT_NAME" -f "$COMPOSE_YAML")
check_prerequisite
mkdir -p -- "$ARTIFACTS"
[ -n "$TASK_PLUGIN_JAR" ] && cp -f "$TASK_PLUGIN_JAR" "$ARTIFACTS/task.jar"
if [ ! -e "$ARTIFACTS/task.jar" ] ; then
    MISSING="Missing $ARTIFACTS/task.jar"
    [ -n "$TASK_PLUGIN_JAR" ] && die "$MISSING, check for copy failure?"
    usage "$MISSING, did you forget --task-plugin-jar?"
fi
[ -n "$GERRIT_WAR" ] && cp -f "$GERRIT_WAR" "$ARTIFACTS/gerrit.war"
progress "Building docker images" build_images
run_task_plugin_tests ; RESULT=$?
cleanup

exit "$RESULT"
