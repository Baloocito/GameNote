package cl.topingames.gamenote.domain.model

data class Note(
    val id: Long = 0,
    val gameId: Long? = null,
    val gameName: String? = null,
    val title: String,
    val content: String = "",
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val category: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
