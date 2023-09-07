workspace(
    name = "task",
    managed_directories = {
        "@npm": ["node_modules"],
    },
)

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "a52e3f381e2fe2a53f7641150ff723171a2dda1e",
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

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

ANTLR_VERSION = "4.0"

http_archive(
    name = "rules_antlr",
    sha256 = "26e6a83c665cf6c1093b628b3a749071322f0f70305d12ede30909695ed85591",
    strip_prefix = "rules_antlr-0.5.0",
    urls = ["https://github.com/marcohu/rules_antlr/archive/0.5.0.tar.gz"],
)

load("@rules_antlr//antlr:repositories.bzl", "rules_antlr_dependencies")

maven_jar(
    name = "antlr4_runtime",
    artifact = "org.antlr:antlr4-runtime:" + ANTLR_VERSION,
    sha1 = "02ddf21287c175a7f1d348745f3fbf43730faba3",
)

maven_jar(
    name = "antlr4_tool",
    artifact = "org.antlr:antlr4:" + ANTLR_VERSION,
    sha1 = "d74527f730ad45cb4ba1483eaf63110708c6df17",
)

maven_jar(
    name = "antlr3_runtime",
    artifact = "org.antlr:antlr-runtime:3.5.2",
    sha1 = "cd9cd41361c155f3af0f653009dcecb08d8b4afd",
)

maven_jar(
    name = "stringtemplate4",
    artifact = "org.antlr:ST4:4.0.8",
    sha1 = "0a1c55e974f8a94d78e2348fa6ff63f4fa1fae64",
)

maven_jar(
    name = "javax_json",
    artifact = "org.glassfish:javax.json:1.0.4",
    sha1 = "3178f73569fd7a1e5ffc464e680f7a8cc784b85a",
)

# Load plugin API
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
    "gerrit_api",
)

# Release Plugin API
gerrit_api()
