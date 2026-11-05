package cl.topingames.gamenote.data.repository

import cl.topingames.gamenote.data.local.dao.NoteDao
import cl.topingames.gamenote.data.local.dao.TagDao
import cl.topingames.gamenote.data.local.entities.NoteEntity
import cl.topingames.gamenote.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val tagDao: TagDao
) {
    fun getAllNotes(): Flow<List<Note>> =
        noteDao.getAllNotes().map { list -> list.map { it.toDomain() } }

    fun getNotesByGame(gameId: Long): Flow<List<Note>> =
        noteDao.getNotesByGame(gameId).map { list -> list.map { it.toDomain() } }

    fun getRecentNotes(limit: Int = 10): Flow<List<Note>> =
        noteDao.getRecentNotes(limit).map { list -> list.map { it.toDomain() } }

    fun getFavoriteNotes(): Flow<List<Note>> =
        noteDao.getFavoriteNotes().map { list -> list.map { it.toDomain() } }

    fun searchNotes(query: String): Flow<List<Note>> =
        noteDao.searchNotes(query).map { list -> list.map { it.toDomain() } }

    fun searchNotesInGame(gameId: Long, query: String): Flow<List<Note>> =
        noteDao.searchNotesInGame(gameId, query).map { list -> list.map { it.toDomain() } }

    suspend fun getNoteById(id: Long): Note? =
        noteDao.getNoteById(id)?.toDomain()

    suspend fun saveNote(note: Note): Long {
        val entity = note.toEntity()
        val id = noteDao.insertNote(entity)
        if (note.tags.isNotEmpty()) {
            tagDao.setTagsForNote(id, note.tags)
        }
        return id
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note.toEntity().copy(updatedAt = System.currentTimeMillis()))
        tagDao.setTagsForNote(note.id, note.tags)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note.toEntity())
    }

    private fun NoteEntity.toDomain() = Note(
        id = id,
        gameId = gameId,
        title = title,
        content = content,
        isFavorite = isFavorite,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Note.toEntity() = NoteEntity(
        id = id,
        gameId = gameId,
        title = title,
        content = content,
        isFavorite = isFavorite,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
