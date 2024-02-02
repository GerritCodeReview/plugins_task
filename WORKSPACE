workspace(
    name = "task",
)

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "e977766e5670dca125c502c37339e9f3da9b329b",
    #local_path = "/home/<user>/projects/bazlets",
)

# Polymer dependencies
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_polymer.bzl",
    "gerrit_polymer",
)

gerrit_polymer()

load("@build_bazel_rules_nodejs//:repositories.bzl", "build_bazel_rules_nodejs_dependencies")

build_bazel_rules_nodejs_dependencies()

load("@build_bazel_rules_nodejs//:index.bzl", "node_repositories", "yarn_install")

node_repositories(
    node_version = "17.9.1",
    yarn_version = "1.22.19",
)

yarn_install(
    name = "npm",
    exports_directories_only = False,
    frozen_lockfile = False,
    package_json = "//:package.json",
    symlink_node_modules = True,
    yarn_lock = "//:yarn.lock",
)

# Load plugin API
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
    "gerrit_api",
)

# Release Plugin API
gerrit_api()

load("//:external_plugin_deps.bzl", "external_plugin_deps")

external_plugin_deps()
