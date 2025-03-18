package top.fifthlight.fabazel.remapper

import kotlinx.serialization.json.Json
import net.fabricmc.tinyremapper.NonClassCopyMode
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension
import kotlin.io.path.Path

object TinyRemapperWorker {
    private val format = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    private val mappings = MappingManager()

    @JvmStatic
    fun main(args: Array<String>) {
        while (true) {
            val line = readlnOrNull() ?: break
            System.err.println(line)
            if (line.isEmpty()) {
                continue
            }
            val workRequest: WorkRequest = format.decodeFromString(line)
            val workResponse = handleRequest(workRequest)
            println(format.encodeToString(workResponse))
            System.gc()
        }

        mappings.close()
    }

    private fun handleRequest(request: WorkRequest): WorkResponse {
        val inputs = request.inputs.associate { Pair(it.path, it) }
        fun getInputFileHash(file: String): String = inputs[file]?.digest ?: error("Bad input file: $file")
        try {
            val (parameters, arguments) = request.arguments.partition { it.startsWith("--") }

            var mixin = false
            for (parameter in parameters) {
                val name = parameter.removePrefix("--")
                when (name) {
                    "mixin" -> mixin = true
                }
            }

            if (arguments.size < 5) {
                return WorkResponse(
                    requestId = request.requestId,
                    exitCode = 1,
                    output = "Bad count of arguments: ${arguments.size}, at least 5",
                )
            }

            val (inputJar, outputJar, mappingPath, fromNamespace, toNamespace) = arguments
            val classpath = arguments.subList(5, arguments.size).map(::Path)

            val mappingArgument = MappingManager.Argument(
                mapping = Path(mappingPath),
                mappingHash = getInputFileHash(mappingPath),
                fromNamespace = fromNamespace,
                toNamespace = toNamespace,
            )
            val mapping = mappings[mappingArgument]
            val remapper = TinyRemapper
                .newRemapper()
                .apply {
                    withMappings(mapping)
                    if (mixin) {
                        extension(MixinExtension())
                    }
                }
                .build()
            val input = Path(inputJar)
            try {
                OutputConsumerPath.Builder(Path(outputJar)).build().use { output ->
                    output.addNonClassFiles(input, NonClassCopyMode.FIX_META_INF, remapper)
                    remapper.readInputs(input)
                    remapper.readClassPath(*classpath.toTypedArray())
                    remapper.apply(output)
                }
            } finally {
                remapper.finish()
            }
            return WorkResponse(
                requestId = request.requestId,
                exitCode = 0,
            )
        } catch (ex: Exception) {
            return WorkResponse(
                requestId = request.requestId,
                exitCode = 1,
                output = ex.stackTraceToString(),
            )
        }
    }
}
