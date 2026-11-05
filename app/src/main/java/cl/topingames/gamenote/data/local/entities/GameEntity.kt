package cl.topingames.gamenote.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    indices = [Index(value = ["name"], unique = true)]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val platform: String? = null,
    val color: Int? = null,
    val emoji: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
