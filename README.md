# Fabazel

Minecraft Fabric mod built with Bazel.

## How to build

1. Install Bazel. [Bazelisk](https://github.com/bazelbuild/bazelisk) is recommended.
2. Run `bazel build @fabazel//mod/src/main:mod`.
3. Grab compiled JAR at `bazel-bin/mod/src/main/mod.jar`.
