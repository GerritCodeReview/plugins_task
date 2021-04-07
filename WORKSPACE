workspace(
    name = "task",
    managed_directories = {
        "@npm": ["node_modules"],
    },
)

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "bd28cc1eaeb01b2118e85e76e00ef4f39a6c9958",
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
