workspace(name = "task")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "f3b3eecf061193e8909298bc9c35027df3bf9d44",
    #local_path = "/home/<user>/projects/bazlets",
)

# Release Plugin API
#load(
#    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
#    "gerrit_api",
#)

# Snapshot Plugin API
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api_maven_local.bzl",
    "gerrit_api_maven_local",
)

# Load release Plugin API
#gerrit_api()

# Load snapshot Plugin API
gerrit_api_maven_local()
