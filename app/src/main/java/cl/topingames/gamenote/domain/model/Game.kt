package cl.topingames.gamenote.domain.model

data class Game(
    val id: Long = 0,
    val name: String,
    val platform: String? = null,
    val color: Int? = null,
    val emoji: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val noteCount: Int = 0
)
