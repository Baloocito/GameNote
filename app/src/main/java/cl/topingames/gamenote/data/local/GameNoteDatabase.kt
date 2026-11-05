package cl.topingames.gamenote.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import cl.topingames.gamenote.data.local.dao.GameDao
import cl.topingames.gamenote.data.local.dao.NoteDao
import cl.topingames.gamenote.data.local.dao.TagDao
import cl.topingames.gamenote.data.local.entities.GameEntity
import cl.topingames.gamenote.data.local.entities.NoteEntity
import cl.topingames.gamenote.data.local.entities.NoteTagEntity
import cl.topingames.gamenote.data.local.entities.TagEntity

@Database(
    entities = [GameEntity::class, NoteEntity::class, TagEntity::class, NoteTagEntity::class],
    version = 1,
    exportSchema = true
)
abstract class GameNoteDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
}
