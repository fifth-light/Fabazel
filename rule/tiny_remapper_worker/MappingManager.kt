package top.fifthlight.fabazel.remapper

import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.extras.MappingTreeRemapper
import net.fabricmc.mappingio.tree.MemoryMappingTree
import net.fabricmc.tinyremapper.IMappingProvider
import net.fabricmc.tinyremapper.TinyUtils
import org.objectweb.asm.commons.Remapper
import java.lang.AutoCloseable
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class MappingManager: AutoCloseable {
    private val cleanupTimeout = 30.seconds
    private val mappings = ConcurrentHashMap<Argument.Key, CacheEntry>()
    private val executor = ScheduledThreadPoolExecutor(1).apply {
        removeOnCancelPolicy = true
    }
    private val lock = ReentrantLock()
    private var scheduledFuture: ScheduledFuture<*>? = null

    data class CacheEntry(
        val provider: IMappingProvider,
        val remapper: Remapper,
        var lastUsed: Instant
    )

    data class Argument(
        val mapping: Path,
        val mappingHash: String,
        val fromNamespace: String,
        val toNamespace: String,
    ) {
        data class Key(
            val mappingHash: String,
            val fromNamespace: String,
            val toNamespace: String,
        )

        val key by lazy { Key(mappingHash, fromNamespace, toNamespace) }

        val mappingTree by lazy {
            MemoryMappingTree().also { tree ->
                MappingReader.read(mapping, tree)
            }
        }

        val remapper by lazy {
            MappingTreeRemapper(mappingTree, fromNamespace, toNamespace)
        }

        val mappingProvider: IMappingProvider by lazy {
            TinyUtils.createMappingProvider(mappingTree, fromNamespace, toNamespace)
        }
    }

    operator fun get(argument: Argument): CacheEntry {
        val now = Instant.now()
        val key = argument.key

        val entry = mappings.compute(key) { _, existing ->
            existing?.apply {
                lastUsed = now
            } ?: run {
                CacheEntry(
                    provider = argument.mappingProvider,
                    remapper = argument.remapper,
                    lastUsed = now
                )
            }
        }!!

        scheduleCleanupIfNeeded(entry.lastUsed.plusMillis(cleanupTimeout.inWholeMilliseconds))
        return entry
    }

    private fun scheduleCleanupIfNeeded(expiration: Instant) {
        lock.withLock {
            val currentDelay = scheduledFuture?.let {
                if (it.isDone) {
                    null
                } else {
                    Duration.of(it.getDelay(TimeUnit.MILLISECONDS), ChronoUnit.MILLIS)
                }
            }

            val newDelay = Duration.between(Instant.now(), expiration).takeIf { !it.isNegative } ?: Duration.ZERO

            if (currentDelay == null || newDelay < currentDelay) {
                scheduledFuture?.cancel(false)
                scheduledFuture = executor.schedule(
                    ::performCleanup,
                    newDelay.toMillis(),
                    TimeUnit.MILLISECONDS
                )
            }
        }
    }

    private fun performCleanup() {
        val now = Instant.now()
        val cutoff = now - cleanupTimeout.toJavaDuration()

        mappings.entries.removeIf { (_, entry) -> entry.lastUsed < cutoff }

        lock.withLock {
            val nextExpiration = mappings.values.minOfOrNull { it.lastUsed }?.plus(cleanupTimeout.toJavaDuration())
            nextExpiration?.let { scheduleCleanupIfNeeded(it) }
        }
    }

    override fun close() {
        scheduledFuture?.cancel(false)
        executor.shutdownNow()
        mappings.clear()
    }
}
