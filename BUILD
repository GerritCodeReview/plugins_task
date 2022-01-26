load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)
load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/bzl:js.bzl", "polygerrit_plugin")
load("//tools/js:eslint.bzl", "eslint")
load("//tools/bzl:junit.bzl", "junit_tests")
load("@rules_java//java:defs.bzl", "java_library", "java_plugin")

plugin_name = "task"

java_plugin(
    name = "auto-value-plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = [
        "@auto-value-annotations//jar",
        "@auto-value//jar",
    ],
)

java_library(
    name = "auto-value",
    exported_plugins = [
        ":auto-value-plugin",
    ],
    visibility = ["//visibility:public"],
    exports = ["@auto-value//jar"],
)

gerrit_plugin(
    name = plugin_name,
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: " + plugin_name,
        "Implementation-Title: Task Plugin",
        "Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/" + plugin_name,
        "Gerrit-Module: com.googlesource.gerrit.plugins.task.Modules$Module",
    ],
    resource_jars = [":gr-task-plugin-static"],
    resources = glob(["src/main/resources/**/*"]),
    deps = [":auto-value"],
    javacopts = [ "-Werror", "-Xlint:all", "-Xlint:-classfile", "-Xlint:-processing"],
)

genrule2(
    name = "gr-task-plugin-static",
    srcs = [":gr-task-plugin"],
    outs = ["gr-task-plugin-static.jar"],
    cmd = " && ".join([
        "mkdir $$TMP/static",
        "cp -r $(locations :gr-task-plugin) $$TMP/static",
        "cd $$TMP",
        "zip -Drq $$ROOT/$@ -g .",
    ]),
)

polygerrit_plugin(
    name = "gr-task-plugin",
    srcs = glob([
        "gr-task-plugin/*.html",
        "gr-task-plugin/*.js",
    ]),
    app = "plugin.html",
)

junit_tests(
    name = "junit-tests",
    size = "small",
    srcs = glob(["src/test/java/**/*Test.java"]),
    deps = PLUGIN_TEST_DEPS + PLUGIN_DEPS + [plugin_name],
)

sh_test(
    name = "docker-tests",
    size = "medium",
    srcs = ["test/docker/run.sh"],
    args = ["--task-plugin-jar", "$(location :task)"],
    data = [plugin_name] + glob(["test/**"]) + glob(["src/main/resources/Documentation/*"]),
    local = True,
)

eslint(
    name = "lint",
    srcs = glob([
        "gr-task-plugin/**/*.js",
    ]),
    config = ".eslintrc.json",
    data = [],
    extensions = [
        ".js",
    ],
    ignore = ".eslintignore",
    plugins = [
        "@npm//eslint-config-google",
        "@npm//eslint-plugin-html",
        "@npm//eslint-plugin-import",
        "@npm//eslint-plugin-jsdoc",
    ],
)
