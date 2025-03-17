load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file", "http_jar")
load("//private:bytes_util.bzl", "hex_sha1_to_sri")

minecraft_jar = tag_class(
    attrs = {
        "version": attr.string(
            doc = "The Minecraft version to be used",
        ),
        "type": attr.string(
            doc = "The type of JAR or mappings you want to use.",
            values = ["server", "client"],
            default = "client",
        ),
        "mapping": attr.bool(
            doc = "Download mappings",
            default = False,
        ),
    },
)

def _minecrat_repo_impl(rctx):
    entries = rctx.attr.entries

    content = ["""package(default_visibility = ["//visibility:public"])"""]

    for entry in entries:
        file_type = "file" if entry.endswith("mapping") else "jar"
        content.append("""
alias(
    name = "{repo}",
    actual = "@minecraft_{repo}//{file_type}",
)""".format(repo = entry, file_type = file_type))

    rctx.file(
        "BUILD.bazel",
        content = "\n".join(content),
    )

minecraft_registry = repository_rule(
    implementation = _minecrat_repo_impl,
    attrs = {"entries": attr.string_list()},
)

def _minecraft_impl(mctx):
    manifest_url = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    manifest_path = "version_manifest.json"

    mctx.report_progress("Downloading version manifest")
    mctx.download(
        url = manifest_url,
        output = manifest_path,
    )
    manifest = json.decode(mctx.read(manifest_path))

    version_entries = []

    # Deduplicate version entries
    for mod in mctx.modules:
        for minecraft_jar in mod.tags.minecraft_jar:
            version_entry = {
                "version": minecraft_jar.version,
                "type": minecraft_jar.type,
                "mapping": minecraft_jar.mapping,
            }
            found = False
            for entry in version_entries:
                if entry["version"] == version_entry["version"] and entry["type"] == version_entry["type"]:
                    entry["mapping"] = entry["mapping"] or version_entry["mapping"]
                    found = True
            if not found:
                version_entries.append(version_entry)

    repo_names = []

    for version_entry in version_entries:
        target_version = version_entry["version"]
        target_type = version_entry["type"]
        target_mapping = version_entry["mapping"]

        # Find version metadata
        version_entry = None
        for entry in manifest["versions"]:
            if entry["id"] == target_version:
                version_entry = entry
                break
        if not version_entry:
            fail("Version %s not found in manifest" % target_version)

        # Download version JSON
        version_json_path = "version_{}.json".format(target_version)
        mctx.report_progress("Downloading %s manifest" % target_version)
        mctx.download(
            url = version_entry["url"],
            output = version_json_path,
        )
        version_data = json.decode(mctx.read(version_json_path))

        # Extract JAR info
        jar_info = version_data["downloads"].get(target_type)
        if not jar_info:
            fail("Type '%s' not found in version %s's data" % (target_type, target_version))

        # Create JAR repository
        repo_name = "%s_%s" % (target_version, target_type)
        http_jar(
            name = "minecraft_%s" % repo_name,
            url = jar_info["url"],
            integrity = hex_sha1_to_sri(jar_info["sha1"]),
            downloaded_file_name = "%s.jar" % target_type,
        )
        repo_names.append(repo_name)

        if target_mapping:
            mapping_info = version_data["downloads"].get("%s_mappings" % target_type)
            if mapping_info == None:
                fail("No mappings for version %s" % target_version)

            # Create mapping repository
            repo_name = "%s_%s_mapping" % (target_version, target_type)
            http_file(
                name = "minecraft_%s" % repo_name,
                url = mapping_info["url"],
                integrity = hex_sha1_to_sri(mapping_info["sha1"]),
                downloaded_file_path = "mappings.txt",
            )
            repo_names.append(repo_name)

    if repo_names:
        minecraft_registry(
            name = "minecraft",
            entries = repo_names,
        )

minecraft = module_extension(
    implementation = _minecraft_impl,
    tag_classes = {
        "minecraft_jar": minecraft_jar,
    },
)
