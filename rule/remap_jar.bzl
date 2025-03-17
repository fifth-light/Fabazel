load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _remap_jar_impl(ctx):
    input_java_info = ctx.attr.input[JavaInfo]
    input_jar = input_java_info.outputs.jars[0].class_jar
    output_jar = ctx.actions.declare_file(ctx.attr.name + ".jar")
    args = ctx.actions.args()

    target = {
        "JavaInfo": JavaInfo(
            output_jar = output_jar,
            compile_jar = output_jar,
        ),
        "DefaultInfo": DefaultInfo(
            files = depset([output_jar]),
        ),
    }

    if ctx.attr.mixin:
        args.add("--mixin")

    args.add_all([
        input_jar.path,
        output_jar.path,
        ctx.file.mapping.path,
        ctx.attr.from_namespace,
        ctx.attr.to_namespace,
    ])

    classpath_depsets = []
    classpath_depsets.append(input_java_info.transitive_compile_time_jars)
    for item in ctx.attr.classpath:
        java_info = item[JavaInfo]
        if java_info:
            classpath_depsets.append(java_info.compile_jars)
            continue
        default_info = item[DefaultInfo]
        classpath_depsets.append(default_info.files)

    classpath = depset(transitive = classpath_depsets)
    for item in classpath.to_list():
        args.add(item)

    inputs = depset([input_jar, ctx.file.mapping], transitive = [classpath])

    ctx.actions.run(
        inputs = inputs,
        outputs = [output_jar],
        executable = ctx.executable._tiny_remapper_bin,
        arguments = [args],
        progress_message = "Remapping %s" % ctx.label.name,
    )

    return target.values()

remap_jar = rule(
    implementation = _remap_jar_impl,
    attrs = {
        "input": attr.label(
            providers = [JavaInfo],
            mandatory = True,
            doc = "Input JAR file. Should be a label with JavaInfo",
        ),
        "mapping": attr.label(
            allow_single_file = [".tiny"],
            mandatory = True,
            doc = "Mapping file. Must be .tiny file",
        ),
        "classpath": attr.label_list(
            allow_empty = True,
            doc = "Classpath of input file",
        ),
        "from_namespace": attr.string(
            mandatory = True,
            doc = "Map from this namespace",
        ),
        "to_namespace": attr.string(
            mandatory = True,
            doc = "Map to this namespace",
        ),
        "mixin": attr.bool(
            default = False,
            doc = "Handle mixin mappings",
        ),
        "_tiny_remapper_bin": attr.label(
            default = Label("//rule/tiny_remapper"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Remap a JAR using a tiny (v1 / v2) mapping.",
)
