load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)
load("//tools/bzl:js.bzl", "gerrit_js_bundle")
load("//tools/js:eslint.bzl", "eslint")
load("//tools/bzl:junit.bzl", "junit_tests")
load("@rules_java//java:defs.bzl", "java_library", "java_plugin")
load("@rules_antlr//antlr:antlr4.bzl", "antlr")

plugin_name = "task"
test_factory_provider_plugin_name = "names-factory-provider"

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

antlr(
    name = "task_reference",
    srcs = ["src/main/antlr4/com/googlesource/gerrit/plugins/task/TaskReference.g4"],
    package = "com.googlesource.gerrit.plugins.task",
    visibility = ["//visibility:public"],
)

java_library(
    name = "task_reference_parser",
    srcs = [":task_reference"],
    deps = ["@antlr4_runtime//jar"],
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
    resource_jars = [":gr-task-plugin"],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        ":auto-value",
        ":task_reference_parser",
        "@antlr4_runtime//jar",
    ],
    javacopts = [ "-Werror", "-Xlint:all", "-Xlint:-classfile", "-Xlint:-processing"],
)

gerrit_js_bundle(
    name = "gr-task-plugin",
    srcs = glob(["gr-task-plugin/*.js"]),
    entry_point = "gr-task-plugin/plugin.js",
)

junit_tests(
    name = "junit-tests",
    size = "small",
    srcs = glob(["src/test/java/**/*Test.java"]),
    deps = PLUGIN_TEST_DEPS + PLUGIN_DEPS + [plugin_name],
)

gerrit_plugin(
    name = test_factory_provider_plugin_name,
    srcs = ["src/main/java/com/googlesource/gerrit/plugins/task/extensions/PluginProvidedTaskNamesFactory.java"] + glob(["src/test/java/**/names_factory_provider/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: " + test_factory_provider_plugin_name,
        "Gerrit-Module: com.googlesource.gerrit.plugins.names_factory_provider.Module",
        "Implementation-Title: Names Factory Provider",
    ],
)

sh_test(
    name = "docker-tests",
    size = "medium",
    srcs = ["test/docker/run.sh"],
    args = ["--task-plugin-jar", "$(location :task)", "--names-factory-provider-plugin-jar", "$(location :names-factory-provider)"],
    data = [plugin_name, test_factory_provider_plugin_name] + glob(["test/**"]) + glob(["src/main/resources/Documentation/*"]),
    local = True,
    tags = ["docker"],
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
    plugins = [],
)
