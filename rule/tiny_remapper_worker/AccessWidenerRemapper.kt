package top.fifthlight.fabazel.remapper

import net.fabricmc.accesswidener.AccessWidenerReader
import net.fabricmc.accesswidener.AccessWidenerRemapper
import net.fabricmc.accesswidener.AccessWidenerWriter
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import org.objectweb.asm.commons.Remapper
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.writeText

class AccessWidenerRemapper(
    private val remapper: Remapper,
    private val fromNamespace: String,
    private val toNamespace: String,
) : OutputConsumerPath.ResourceRemapper {
    override fun canTransform(remapper: TinyRemapper, path: Path): Boolean =
        path.fileName.extension.equals("accesswidener", true)

    override fun transform(
        destinationDirectory: Path,
        relativePath: Path,
        input: InputStream,
        tinyRemapper: TinyRemapper,
    ) {
        val outputFile = destinationDirectory.resolve(relativePath.toString())

        val writer = AccessWidenerWriter()
        val accessWidenerRemapper = AccessWidenerRemapper(
            writer,
            remapper,
            fromNamespace,
            toNamespace,
        )
        AccessWidenerReader(accessWidenerRemapper).read(input.bufferedReader())

        outputFile.writeText(writer.writeString())
    }
}