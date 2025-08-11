# Fabazel

Minecraft Fabric mod built with Bazel.

## How to build

1. Install Bazel. [Bazelisk](https://github.com/bazelbuild/bazelisk) is recommended.
2. Run `bazel build @//mod`.
3. Grab compiled JAR at `bazel-bin/mod/mod.jar`.

## How to run the dev client

1. Install Bazel.
2. Run `bazel run @//mod:client`.

## License

This project is licensed in Apache 2.0 license. Welcome to use this project as template of your next Fabric mod!
