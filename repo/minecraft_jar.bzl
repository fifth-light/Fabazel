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

def _minecraft_repo_impl(rctx):
    version_repo_names = rctx.attr.version_repo_names
    version_libraries = rctx.attr.version_libraries

    content = ['package(default_visibility = ["//visibility:public"])', ""]

    for version_repo in version_repo_names:
        actual_repo = "@minecraft_%s//file" % version_repo
        if version_repo.endswith("mapping"):
            content.append("alias(")
            content.append('    name = "%s",' % version_repo)
            content.append('    actual = "%s",' % actual_repo)
            content.append(")")
        else:
            content.append("java_import(")
            content.append('    name = "%s",' % version_repo)
            content.append('    jars = ["%s"],' % actual_repo)
            content.append('    deps = ["%s_libraries"]' % version_repo)
            content.append(")")
            content.append("java_import(")
            content.append('    name = "%s_libraries",' % version_repo)
            content.append("    jars = [")
            content.append("        %s" % version_libraries[version_repo])
            content.append("    ],")
            content.append(")")
        content.append("")

    rctx.file(
        "BUILD.bazel",
        content = "\n".join(content),
    )

minecraft_registry = repository_rule(
    implementation = _minecraft_repo_impl,
    attrs = {
        "version_repo_names": attr.string_list(),
        "version_libraries": attr.string_dict(),
    },
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

    # Deduplicate version entries
    version_entries = {}
    for mod in mctx.modules:
        for minecraft_jar in mod.tags.minecraft_jar:
            key = (minecraft_jar.version, minecraft_jar.type)
            if key in version_entries:
                version_entries[key]["mapping"] |= minecraft_jar.mapping
            else:
                version_entries[key] = {
                    "version": minecraft_jar.version,
                    "type": minecraft_jar.type,
                    "mapping": minecraft_jar.mapping,
                }
    version_entries = version_entries.values()

    version_repo_names = []
    version_libraries = {}
    library_entries = {}

    def escape_library_name(name):
        return name.replace(".", "_").replace(":", "_")

    def get_library_repo_name(name):
        return "minecraft_%s" % escape_library_name(name)

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
            integrity = hex_sha1_to_sri(version_entry["sha1"]),
        )
        version_data = json.decode(mctx.read(version_json_path))

        # Extract JAR info
        jar_info = version_data["downloads"].get(target_type)
        if not jar_info:
            fail("Type '%s' not found in version %s's data" % (target_type, target_version))

        # Create repository for JAR
        repo_name = "%s_%s" % (target_version, target_type)
        http_file(
            name = "minecraft_%s" % repo_name,
            url = jar_info["url"],
            integrity = hex_sha1_to_sri(jar_info["sha1"]),
            downloaded_file_path = "%s.jar" % target_type,
        )
        version_repo_names.append(repo_name)

        # Create repository for mapping
        if target_mapping:
            mapping_info = version_data["downloads"].get("%s_mappings" % target_type)
            if mapping_info == None:
                fail("No mappings for version %s" % target_version)

            # Create mapping repository
            mapping_repo_name = "%s_%s_mapping" % (target_version, target_type)
            http_file(
                name = "minecraft_%s" % mapping_repo_name,
                url = mapping_info["url"],
                integrity = hex_sha1_to_sri(mapping_info["sha1"]),
                downloaded_file_path = "mappings.txt",
            )
            version_repo_names.append(mapping_repo_name)

        # Append library entries
        libraries = []
        for library in version_data["libraries"]:
            name = library["name"]
            if library_entries.get(name):
                continue

            escaped_name = escape_library_name(name)
            downloads = library["downloads"]["artifact"]
            library_entries[name] = {
                "name": escaped_name,
                "sha1": downloads["sha1"],
                "url": downloads["url"],
                "path": downloads["path"],
            }

            library_repo_name = get_library_repo_name(name)
            libraries.append('"@%s//file"' % library_repo_name)

        version_libraries[repo_name] = ",\n        ".join(libraries)

    # Create repositories for libraries
    for library_entry_name in library_entries.keys():
        library_entry = library_entries[library_entry_name]
        repo_name = get_library_repo_name(library_entry["name"])
        http_file(
            name = repo_name,
            url = library_entry["url"],
            integrity = hex_sha1_to_sri(library_entry["sha1"]),
            downloaded_file_path = library_entry["path"],
        )

    minecraft_registry(
        name = "minecraft",
        version_repo_names = version_repo_names,
        version_libraries = version_libraries,
    )

minecraft = module_extension(
    implementation = _minecraft_impl,
    tag_classes = {
        "minecraft_jar": minecraft_jar,
    },
)
