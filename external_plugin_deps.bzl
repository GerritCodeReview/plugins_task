load("@bazel_tools//tools/build_defs/repo:maven_rules.bzl", "maven_jar")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def external_plugin_deps():
    AUTO_VALUE_VERSION = "1.7.4"

    maven_jar(
        name = "auto-value",
        artifact = "com.google.auto.value:auto-value:" + AUTO_VALUE_VERSION,
        sha1 = "6b126cb218af768339e4d6e95a9b0ae41f74e73d",
    )

    maven_jar(
        name = "auto-value-annotations",
        artifact = "com.google.auto.value:auto-value-annotations:" + AUTO_VALUE_VERSION,
        sha1 = "eff48ed53995db2dadf0456426cc1f8700136f86",
    )

    http_archive(
        name = "rules_antlr",
        sha256 = "26e6a83c665cf6c1093b628b3a749071322f0f70305d12ede30909695ed85591",
        strip_prefix = "rules_antlr-0.5.0",
        urls = ["https://github.com/marcohu/rules_antlr/archive/0.5.0.tar.gz"],
    )

    maven_jar(
        name = "antlr3_runtime",
        artifact = "org.antlr:antlr-runtime:3.5.2",
        sha1 = "cd9cd41361c155f3af0f653009dcecb08d8b4afd",
    )

    ANTLR_VERSION = "4.9.3"

    maven_jar(
        name = "antlr4_runtime",
        artifact = "org.antlr:antlr4-runtime:" + ANTLR_VERSION,
        sha1 = "81befc16ebedb8b8aea3e4c0835dd5ca7e8523a8",
    )

    maven_jar(
        name = "antlr4_tool",
        artifact = "org.antlr:antlr4:" + ANTLR_VERSION,
        sha1 = "9d47afaa75d70903b5b77413b034d6b201d7d5d6",
    )

    maven_jar(
        name = "stringtemplate4",
        artifact = "org.antlr:ST4:4.3.1",
        sha1 = "9c61ac6d17b7f450b4048742c2cc73787972518e",
    )

    maven_jar(
        name = "javax_json",
        artifact = "org.glassfish:javax.json:1.0.4",
        sha1 = "3178f73569fd7a1e5ffc464e680f7a8cc784b85a",
    )
