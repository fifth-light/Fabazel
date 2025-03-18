package top.fifthlight.fabazel.remapper

import net.fabricmc.tinyremapper.api.TrLogger
import java.io.PrintWriter

class PrintLogger(private val printStream: PrintWriter): TrLogger {
    override fun log(level: TrLogger.Level, string: String) {
        printStream.print('[')
        printStream.print(level.name)
        printStream.print("] ")
        printStream.println(string)
    }
}