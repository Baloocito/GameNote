package cl.topingames.gamenote.data.local.dao

import androidx.room.*
import cl.topingames.gamenote.data.local.entities.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

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
