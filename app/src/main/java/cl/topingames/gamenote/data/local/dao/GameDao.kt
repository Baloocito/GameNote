package cl.topingames.gamenote.data.local.dao

import androidx.room.*
import cl.topingames.gamenote.data.local.entities.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games ORDER BY name ASC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: Long): GameEntity?

    @Query("SELECT * FROM games ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentGames(limit: Int = 5): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("SELECT COUNT(*) FROM notes WHERE gameId = :gameId")
    fun getNoteCountForGame(gameId: Long): Flow<Int>
}
