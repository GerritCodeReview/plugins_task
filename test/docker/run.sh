#!/usr/bin/env bash

readlink -f / &> /dev/null || readlink() { greadlink "$@" ; } # for MacOS
MYDIR=$(dirname -- "$(readlink -f -- "$0")")
MYPROG=$(basename -- "$0")
source "$MYDIR/lib_options.sh"
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
    cat <<-EOF
Usage:
    $MYPROG [--task-plugin-jar|-t <FILE_PATH>] [--gerrit-war|-g <FILE_PATH>]

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
    --preserve                 To preserve the docker setup for debugging

EOF

    [ -n "$1" ] && echo -e "\nERROR: $1" && exit 1
    exit 0
}

check_prerequisite() {
    docker --version > /dev/null || die "docker is not installed"
    docker-compose --version > /dev/null || die "docker-compose is not installed"
}

build_images() { # use_custom_root_cfg
    local use_custom_root_cfg="${1:-false}"
    if "$use_custom_root_cfg"; then
        docker-compose "${CUSTOM_ROOT_CFG_COMPOSE_ARGS[@]}" build \
            --build-arg ROOT_CFG_PRJ="$CUSTOM_ROOT_CFG_PRJ" \
            --build-arg ROOT_CFG_BRANCH="$CUSTOM_ROOT_CFG_BRANCH" \
            --quiet
    else
        docker-compose "${COMPOSE_ARGS[@]}" build --quiet
    fi
}

run_task_plugin_tests() { # use_custom_root_cfg
    local use_custom_root_cfg="${1:-false}"
    if "$use_custom_root_cfg"; then
        docker-compose "${CUSTOM_ROOT_CFG_COMPOSE_ARGS[@]}" up --detach
        docker-compose "${CUSTOM_ROOT_CFG_COMPOSE_ARGS[@]}" exec -T --user=admin run_tests \
            sh -c "/task/test/docker/run_tests/start.sh \
                --root-config-project $CUSTOM_ROOT_CFG_PRJ \
                --root-config-branch $CUSTOM_ROOT_CFG_BRANCH"
    else
        docker-compose "${COMPOSE_ARGS[@]}" up --detach
        docker-compose "${COMPOSE_ARGS[@]}" exec -T --user=admin run_tests \
            sh -c "/task/test/docker/run_tests/start.sh"
    fi
}

retest() {
    docker-compose "${COMPOSE_ARGS[@]}" exec -T --user=admin run_tests \
        sh -c "/task/test/docker/run_tests/start.sh --retest" || RESULT=1
    docker-compose "${CUSTOM_ROOT_CFG_COMPOSE_ARGS[@]}" exec -T --user=admin run_tests \
        sh -c "/task/test/docker/run_tests/start.sh \
            --root-config-project $CUSTOM_ROOT_CFG_PRJ \
            --root-config-branch $CUSTOM_ROOT_CFG_BRANCH \
            --retest" || RESULT=1
    cleanup
}

get_run_test_container() { # use_custom_root_cfg
    local use_custom_root_cfg="${1:-false}"
    if "$use_custom_root_cfg"; then
        docker-compose "${CUSTOM_ROOT_CFG_COMPOSE_ARGS[@]}" ps | grep run_tests | awk '{ print $1 }'
    else
        docker-compose "${COMPOSE_ARGS[@]}" ps | grep run_tests | awk '{ print $1 }'
    fi
}

cleanup() {
    if [ "$PRESERVE" = "true" ] ; then
        echo -e "\n\nPreserving the following docker setups:"

        echo -e "\n\nDefault root project configuration"
        echo "----------------------------------"
        docker-compose "${COMPOSE_ARGS[@]}" ps
        echo ""
        echo "To exec into runtests container, use following command:"
        echo "docker exec -it $(get_run_test_container) /bin/bash"
        echo ""
        echo "Run the following command to bring down the setup:"
        echo "docker-compose ${COMPOSE_ARGS[@]} down -v --rmi local"
        echo ""

        echo -e "\n\nCustom root project configuration"
        echo "---------------------------------"
        docker-compose "${CUSTOM_ROOT_CFG_COMPOSE_ARGS[@]}" ps
        echo ""
        echo "To exec into runtests container, use following command:"
        echo "docker exec -it $(get_run_test_container true) /bin/bash"
        echo ""
        echo "Run the following command to bring down the setup:"
        echo "docker-compose ${CUSTOM_ROOT_CFG_COMPOSE_ARGS[@]} down -v --rmi local"
        echo ""

        echo -e "\n\nUse command below to re run tests after making changes to test scripts"
        options_prefix "COMPOSE_ARGS" "--compose-arg"
        options_prefix "CUSTOM_ROOT_CFG_COMPOSE_ARGS" "--custom-root-cfg-compose-arg"
        echo "$MYDIR/$MYPROG --retest --preserve ${COMPOSE_ARGS[@]} ${CUSTOM_ROOT_CFG_COMPOSE_ARGS[@]}"
    else
        docker-compose "${COMPOSE_ARGS[@]}" down -v --rmi local 2>/dev/null
        docker-compose "${CUSTOM_ROOT_CFG_COMPOSE_ARGS[@]}" down -v --rmi local 2>/dev/null
    fi
}

PRESERVE="false"
RETEST="false"
COMPOSE_ARGS=()
while (( "$#" )) ; do
    case "$1" in
        --help|-h)                     usage ;;
        --gerrit-war|-g)               shift ; GERRIT_WAR=$1 ;;
        --task-plugin-jar|-t)          shift ; TASK_PLUGIN_JAR=$1 ;;
        --preserve)                    PRESERVE="true" ;;
        --retest)                      RETEST="true" ;;
        --compose-arg)                 shift ; COMPOSE_ARGS+=("$1") ;;
        --custom-root-cfg-compose-arg) shift ; CUSTOM_ROOT_CFG_COMPOSE_ARGS+=("$1") ;;
        *)                             usage "invalid argument $1" ;;
    esac
    shift
done

CUSTOM_ROOT_CFG_PRJ=task-config
CUSTOM_ROOT_CFG_BRANCH=refs/heads/master

[ "$RETEST" = "true" ] && { retest ; exit "$RESULT" ; }

COMPOSE_YAML="$MYDIR/docker-compose.yaml"
COMPOSE_ARGS=(--project-name "task_$$" -f "$COMPOSE_YAML")
CUSTOM_ROOT_CFG_COMPOSE_ARGS=(--project-name "task_custom_root_cfg_$$" -f "$COMPOSE_YAML")

check_prerequisite
mkdir -p -- "$ARTIFACTS"
[ -n "$TASK_PLUGIN_JAR" ] && cp -f "$TASK_PLUGIN_JAR" "$ARTIFACTS/task.jar"
if [ ! -e "$ARTIFACTS/task.jar" ] ; then
    MISSING="Missing $ARTIFACTS/task.jar"
    [ -n "$TASK_PLUGIN_JAR" ] && die "$MISSING, check for copy failure?"
    usage "$MISSING, did you forget --task-plugin-jar?"
fi
[ -n "$GERRIT_WAR" ] && cp -f "$GERRIT_WAR" "$ARTIFACTS/gerrit.war"
( trap cleanup EXIT SIGTERM
    USE_CUSTOM_ROOT_CFG="false"
    progress "Building docker images" build_images "$USE_CUSTOM_ROOT_CFG"
    run_task_plugin_tests "$USE_CUSTOM_ROOT_CFG"

    USE_CUSTOM_ROOT_CFG="true"
    progress "Building docker images" build_images "$USE_CUSTOM_ROOT_CFG"
    run_task_plugin_tests "$USE_CUSTOM_ROOT_CFG"
)
