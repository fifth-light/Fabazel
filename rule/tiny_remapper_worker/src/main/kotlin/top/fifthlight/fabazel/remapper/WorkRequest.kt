package top.fifthlight.fabazel.remapper

import kotlinx.serialization.Serializable

@Serializable
data class WorkRequest(
    val arguments: List<String>,
    val inputs: List<Input>,
    val requestId: Int = 0,
) {
    @Serializable
    data class Input(
        val path: String,
        val digest: String,
    )
}
