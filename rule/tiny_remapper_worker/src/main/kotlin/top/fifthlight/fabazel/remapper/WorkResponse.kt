package top.fifthlight.fabazel.remapper

import kotlinx.serialization.Serializable

@Serializable
data class WorkResponse(
    val exitCode: Int,
    val output: String? = null,
    val requestId: Int,
)
