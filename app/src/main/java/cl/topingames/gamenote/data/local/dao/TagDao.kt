package cl.topingames.gamenote.data.local.dao

import androidx.room.*
import cl.topingames.gamenote.data.local.entities.NoteTagEntity
import cl.topingames.gamenote.data.local.entities.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN note_tags nt ON t.id = nt.tagId
        WHERE nt.noteId = :noteId
        ORDER BY t.name ASC
    """)
    fun getTagsForNote(noteId: Long): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTag(noteTag: NoteTagEntity)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteTagsForNote(noteId: Long)

    @Query("DELETE FROM tags WHERE id NOT IN (SELECT DISTINCT tagId FROM note_tags)")
    suspend fun deleteUnusedTags()

    @Transaction
    suspend fun setTagsForNote(noteId: Long, tagNames: List<String>) {
        deleteTagsForNote(noteId)
        tagNames.forEach { name ->
            val existing = getTagByName(name)
            val tagId = existing?.id ?: insertTag(TagEntity(name = name))
            if (tagId > 0) insertNoteTag(NoteTagEntity(noteId, tagId))
        }
        deleteUnusedTags()
    }

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?
}
