#!/usr/bin/env bash

readlink --canonicalize / &> /dev/null || readlink() { greadlink "$@" ; } # for MacOS
MYDIR=$(dirname -- "$(readlink -f -- "$0")")
cd "$MYDIR"

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
    local error_msg
    docker --version >> /dev/null || error_msg="\nERROR: docker is not installed"
    docker-compose --version >> /dev/null || \
        error_msg+="\nERROR: docker-compose is not installed"
    if [ -n "$error_msg" ] ; then
        echo -e "$error_msg"
        exit 1
    fi
}

# source_location output_path
gcurl() { curl --fail --netrc "$1" --output "$2" --create-dirs || exit ; }

run_test() {
    local ret
    local compose_args=(--project-name "task_$$" -f "$MYDIR/docker-compose.yaml")
    local build_args=()

    if [ -n "$GERRIT_WAR" -a -n "$TASK_PLUGIN_JAR" ] ; then
        gcurl "$GERRIT_WAR" "./gerrit/artifacts/gerrit.war"
        gcurl "$TASK_PLUGIN_JAR" "./gerrit/artifacts/task.jar"
        build_args+=(--build-arg GERRIT_WAR="/artifacts/gerrit.war" \
            --build-arg TASK_PLUGIN_JAR="/artifacts/task.jar" \
            --build-arg UID="$(id -u)" --build-arg GID="$(id -g)")
    else
        message="please set '--gerrit-war' and '--task-plugin-jar'"
        usage "$message"
    fi

    docker-compose "${compose_args[@]}" build "${build_args[@]}" ; ret=$?
    if [ $ret -eq 0 ] ; then
        docker-compose "${compose_args[@]}" up --abort-on-container-exit \
            --exit-code-from run_tests ; ret=$?
    fi
    docker-compose "${compose_args[@]}" down -v --rmi local
    echo "exit code: $ret"
    return $ret
}

check_prerequisite
while (( "$#" )) ; do
    case "$1" in
        --help|-h)                usage ;;
        --gerrit-war|-g)          shift ; GERRIT_WAR=$1 ;;
        --task-plugin-jar|-t)     shift ; TASK_PLUGIN_JAR=$1 ;;
        *)                        usage "invalid argument '$1'" ;;
    esac
    shift
done

run_test
