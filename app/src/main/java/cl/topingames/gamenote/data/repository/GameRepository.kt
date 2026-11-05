package cl.topingames.gamenote.data.repository

import cl.topingames.gamenote.data.local.dao.GameDao
import cl.topingames.gamenote.data.local.entities.GameEntity
import cl.topingames.gamenote.domain.model.Game
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val gameDao: GameDao
) {
    fun getAllGames(): Flow<List<Game>> =
        gameDao.getAllGames().map { list -> list.map { it.toDomain() } }

    fun getRecentGames(limit: Int = 5): Flow<List<Game>> =
        gameDao.getRecentGames(limit).map { list -> list.map { it.toDomain() } }

    suspend fun getGameById(id: Long): Game? =
        gameDao.getGameById(id)?.toDomain()

    suspend fun saveGame(game: Game): Long =
        gameDao.insertGame(game.toEntity())

    suspend fun updateGame(game: Game) =
        gameDao.updateGame(game.toEntity())

    suspend fun deleteGame(game: Game) =
        gameDao.deleteGame(game.toEntity())

    private fun GameEntity.toDomain() = Game(
        id = id,
        name = name,
        platform = platform,
        color = color,
        emoji = emoji,
        createdAt = createdAt
    )

    private fun Game.toEntity() = GameEntity(
        id = id,
        name = name,
        platform = platform,
        color = color,
        emoji = emoji,
        createdAt = createdAt
    )
}
