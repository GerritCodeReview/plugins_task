load("//tools/bzl:plugin.bzl", "gerrit_plugin")
load("//tools/bzl:js.bzl", "gerrit_js_bundle")

plugin_name = "task"

gerrit_plugin(
    name = plugin_name,
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: " + plugin_name,
        "Gerrit-ApiVersion: 3.2.6",
        "Implementation-Title: Task Plugin",
        "Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/" + plugin_name,
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

sh_test(
    name = "docker-tests",
    size = "medium",
    srcs = ["test/docker/run.sh"],
    args = ["--task-plugin-jar", "$(location :task)"],
    data = [plugin_name] + glob(["test/**"]),
    local = True,
)
