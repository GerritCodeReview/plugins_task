load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS", "gerrit_plugin")

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
    resources = glob(["src/main/resources/**/*"]),
)
