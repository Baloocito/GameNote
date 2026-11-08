package cl.topingames.gamenote.data.local.dao

import androidx.room.*
import cl.topingames.gamenote.data.local.entities.NoteEntity
import kotlinx.coroutines.flow.Flow

/** Proyección de nota con nombre del juego resuelto via JOIN. */
data class NoteWithGameName(
    val id: Long,
    val gameId: Long?,
    val gameName: String?,
    val title: String,
    val content: String,
    val isFavorite: Boolean,
    val category: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface NoteDao {

    @Query("""
        SELECT n.*, g.name AS gameName
        FROM notes n
        LEFT JOIN games g ON n.gameId = g.id
        ORDER BY n.updatedAt DESC
    """)
    fun getAllNotesWithGame(): Flow<List<NoteWithGameName>>

    @Query("""
        SELECT n.*, g.name AS gameName
        FROM notes n
        LEFT JOIN games g ON n.gameId = g.id
        ORDER BY n.updatedAt DESC
        LIMIT :limit
    """)
    fun getRecentNotesWithGame(limit: Int = 10): Flow<List<NoteWithGameName>>

    @Query("""
        SELECT n.*, g.name AS gameName
        FROM notes n
        LEFT JOIN games g ON n.gameId = g.id
        WHERE n.gameId = :gameId
        ORDER BY n.updatedAt DESC
    """)
    fun getNotesByGameWithName(gameId: Long): Flow<List<NoteWithGameName>>

    @Query("""
        SELECT n.*, g.name AS gameName
        FROM notes n
        LEFT JOIN games g ON n.gameId = g.id
        WHERE n.isFavorite = 1
        ORDER BY n.updatedAt DESC
    """)
    fun getFavoriteNotesWithGame(): Flow<List<NoteWithGameName>>

    @Query("""
        SELECT n.*, g.name AS gameName
        FROM notes n
        LEFT JOIN games g ON n.gameId = g.id
        WHERE n.title LIKE '%' || :query || '%'
           OR n.content LIKE '%' || :query || '%'
        ORDER BY n.updatedAt DESC
    """)
    fun searchNotesWithGame(query: String): Flow<List<NoteWithGameName>>

    @Query("""
        SELECT n.*, g.name AS gameName
        FROM notes n
        LEFT JOIN games g ON n.gameId = g.id
        WHERE n.gameId = :gameId
          AND (n.title LIKE '%' || :query || '%' OR n.content LIKE '%' || :query || '%')
        ORDER BY n.updatedAt DESC
    """)
    fun searchNotesInGameWithName(gameId: Long, query: String): Flow<List<NoteWithGameName>>

    // ── Queries legacy (sin JOIN) — se mantienen para compatibilidad interna ──

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE gameId = :gameId ORDER BY updatedAt DESC")
    fun getNotesByGame(gameId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE gameId IS NULL ORDER BY updatedAt DESC")
    fun getNotesWithoutGame(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentNotes(limit: Int = 10): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE title LIKE '%' || :query || '%'
           OR content LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE gameId = :gameId
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun searchNotesInGame(gameId: Long, query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE gameId = :gameId AND category = :category ORDER BY updatedAt DESC")
    fun getNotesByCategory(gameId: Long, category: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE gameId = :gameId")
    suspend fun deleteNotesByGame(gameId: Long)
}
