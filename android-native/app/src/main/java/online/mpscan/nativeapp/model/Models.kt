package online.mpscan.nativeapp.model

data class Work(
    val id: String,
    val title: String,
    val alternativeTitle: String = "",
    val synopsis: String = "",
    val cover: String = "",
    val banner: String = "",
    val type: String = "",
    val status: String = "",
    val author: String = "",
    val genres: List<String> = emptyList(),
    val sensitive: Boolean = false,
    val adult: Boolean = false,
    val published: Boolean = true,
    val updatedAt: Long = 0L,
    val reads: Long = 0L
)

data class Chapter(
    val id: String,
    val workId: String,
    val number: Double?,
    val title: String = "",
    val published: Boolean = true,
    val updatedAt: Long = 0L
) {
    val label: String get() = number?.let { if (it % 1.0 == 0.0) "Capítulo ${it.toInt()}" else "Capítulo $it" } ?: "Capítulo"
}
