workspace(
    name = "task",
    managed_directories = {
        "@npm": ["node_modules"],
    },
)

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "1dd03e38f46e56defc91b6ab4a4d9879f6083de1",
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

# Load closure compiler with transitive dependencies
load("@io_bazel_rules_closure//closure:repositories.bzl", "rules_closure_dependencies", "rules_closure_toolchains")

rules_closure_dependencies()

rules_closure_toolchains()

# Load Gerrit npm_binary toolchain
load("@com_googlesource_gerrit_bazlets//tools:js.bzl", "GERRIT", "npm_binary")

npm_binary(
    name = "polymer-bundler",
    repository = GERRIT,
)

npm_binary(
    name = "crisper",
    repository = GERRIT,
)

load("//tools/bzl:maven_jar.bzl", "maven_jar")

AUTO_VALUE_VERSION = "1.7.4"

maven_jar(
    name = "auto-value",
    artifact = "com.google.auto.value:auto-value:" + AUTO_VALUE_VERSION,
    sha1 = "6b126cb218af768339e4d6e95a9b0ae41f74e73d",
)

maven_jar(
    name = "auto-value-annotations",
    artifact = "com.google.auto.value:auto-value-annotations:" + AUTO_VALUE_VERSION,
    sha1 = "eff48ed53995db2dadf0456426cc1f8700136f86",
)

# Load plugin API
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
    "gerrit_api",
)

# Release Plugin API
gerrit_api()
