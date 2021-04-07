load("//tools/bzl:plugin.bzl", "gerrit_plugin")
load("//tools/bzl:js.bzl", "gerrit_js_bundle")

gerrit_plugin(
    name = "task",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: task",
        "Gerrit-ApiVersion: 3.0-SNAPSHOT",
        "Implementation-Title: Task Plugin",
        "Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/task",
        "Gerrit-Module: com.googlesource.gerrit.plugins.task.Modules$Module",
    ],
    resource_jars = [":gr-task-plugin"],
    resources = glob(["src/main/resources/**/*"]),
)

gerrit_js_bundle(
    name = "gr-task-plugin",
    srcs = glob(["gr-task-plugin/*.js"]),
    entry_point = "gr-task-plugin/plugin.js",
)
