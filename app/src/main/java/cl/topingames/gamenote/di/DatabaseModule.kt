package cl.topingames.gamenote.di

import android.content.Context
import androidx.room.Room
import cl.topingames.gamenote.data.local.GameNoteDatabase
import cl.topingames.gamenote.data.local.dao.GameDao
import cl.topingames.gamenote.data.local.dao.NoteDao
import cl.topingames.gamenote.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GameNoteDatabase =
        Room.databaseBuilder(
            context,
            GameNoteDatabase::class.java,
            "gamenote.db"
        ).build()

    @Provides
    fun provideGameDao(db: GameNoteDatabase): GameDao = db.gameDao()

    @Provides
    fun provideNoteDao(db: GameNoteDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideTagDao(db: GameNoteDatabase): TagDao = db.tagDao()
}
