load("//tools/bzl:plugin.bzl", "gerrit_plugin")

plugin_name = "names-factory-provider"

gerrit_plugin(
    name = plugin_name,
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: " + plugin_name,
        "Gerrit-Module: com.googlesource.gerrit.plugins.names_factory_provider.Module",
        "Implementation-Title: Names Factory Provider",
    ],
    resources = glob(["src/main/resources/**/*"]),
)
