load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS_NEVERLINK")
load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "extensions",
    srcs = glob(["**/*.java"]),
    visibility = ["//visibility:public"],
    deps = PLUGIN_DEPS_NEVERLINK,
)
