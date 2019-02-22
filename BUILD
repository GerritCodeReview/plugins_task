load("//tools/bzl:plugin.bzl", "gerrit_plugin")
load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/bzl:js.bzl", "polygerrit_plugin")

gerrit_plugin(
    name = "task",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: task",
        "Gerrit-ApiVersion: 2.16",
        "Implementation-Title: Task Plugin",
        "Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/task",
        "Gerrit-Module: com.googlesource.gerrit.plugins.task.Modules$Module",
        "Gerrit-SshModule: com.googlesource.gerrit.plugins.task.Modules$SshModule",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.task.Modules$HttpModule",
    ],
    resource_jars = [":gr-task-static"],
    resources = glob(["src/main/resources/**/*"]),
)

genrule2(
    name = "gr-task-static",
    srcs = [":gr-task"],
    outs = ["gr-task-static.jar"],
    cmd = " && ".join([
        "mkdir $$TMP/static",
        "cp -r $(locations :gr-task) $$TMP/static",
        "cd $$TMP",
        "zip -Drq $$ROOT/$@ -g .",
    ]),
)

polygerrit_plugin(
    name = "gr-task",
    srcs = glob([
        "gr-task/*.html",
        "gr-task/*.js",
    ]),
    app = "plugin.html",
)
