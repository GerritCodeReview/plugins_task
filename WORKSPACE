workspace(
    name = "task",
    managed_directories = {
        "@npm": ["node_modules"],
    },
)

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "7ff4605f48db148197675a0d2ea41ee07cb72fd3",
    #local_path = "/home/<user>/projects/bazlets",
)

# Polymer dependencies
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_polymer.bzl",
    "gerrit_polymer",
)

gerrit_polymer()

load("@build_bazel_rules_nodejs//:index.bzl", "yarn_install")

yarn_install(
    name = "npm",
    frozen_lockfile = False,
    package_json = "//:package.json",
    yarn_lock = "//:yarn.lock",
)

# Load plugin API
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
    "gerrit_api",
)

gerrit_api()
