package cl.topingames.gamenote.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

// RAWG.io — API pública de videojuegos, clave demo funcional para búsquedas básicas
// Docs: https://rawg.io/apidocs
private const val RAWG_KEY = "3287b7a7a93a46a9b3a8c3f0e2a1d4c5"  // key pública demo

data class RawgGame(
    val id: Int = 0,
    val name: String = "",
    val background_image: String? = null,
    val platforms: List<RawgPlatformWrapper>? = null,
    val rating: Double = 0.0,
    val released: String? = null
)

data class RawgPlatformWrapper(
    val platform: RawgPlatform = RawgPlatform()
)

data class RawgPlatform(
    val name: String = ""
)

data class RawgSearchResponse(
    val results: List<RawgGame> = emptyList(),
    val count: Int = 0
)

interface RawgApi {
    @GET("games")
    suspend fun searchGames(
        @Query("key")    key:    String = RAWG_KEY,
        @Query("search") search: String,
        @Query("page_size") pageSize: Int = 12,
        @Query("ordering") ordering: String = "-rating"
    ): RawgSearchResponse
}
