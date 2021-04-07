load("@npm//@bazel/rollup:index.bzl", "rollup_bundle")
load("@npm//@bazel/terser:index.bzl", "terser_minified")
load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/bzl:plugin.bzl", "gerrit_plugin")

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
    resource_jars = [":gr-task-plugin-static"],
    resources = glob(["src/main/resources/**/*"]),
)

genrule2(
    name = "gr-task-plugin-static",
    srcs = [":gr-task-plugin.min"],
    outs = ["gr-task-plugin-static.jar"],
    cmd = " && ".join([
        "mkdir $$TMP/static",
        "cp -r $(locations :gr-task-plugin.min) $$TMP/static",
        "cd $$TMP",
        "zip -Drq $$ROOT/$@ -g .",
    ]),
)

terser_minified(
    name = "gr-task-plugin.min",
    src = "gr-task-plugin-bundle.js",
    sourcemap = False,
)

rollup_bundle(
    name = "gr-task-plugin-bundle",
    srcs = glob(["gr-task-plugin/*.js"]),
    entry_point = "gr-task-plugin/plugin.js",
    format = "iife",
    sourcemap = "hidden",
)
