package cl.topingames.gamenote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cl.topingames.gamenote.ui.games.GameDetailScreen
import cl.topingames.gamenote.ui.games.GamesScreen
import cl.topingames.gamenote.ui.home.HomeScreen
import cl.topingames.gamenote.ui.notes.NoteEditorScreen
import cl.topingames.gamenote.ui.search.SearchScreen
import cl.topingames.gamenote.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Games : Screen("games")
    object GameDetail : Screen("game/{gameId}") {
        fun createRoute(gameId: Long) = "game/$gameId"
    }
    object NoteEditor : Screen("note?noteId={noteId}&gameId={gameId}") {
        fun createRoute(noteId: Long? = null, gameId: Long? = null): String {
            val noteParam = noteId?.let { "noteId=$it" } ?: ""
            val gameParam = gameId?.let { "gameId=$it" } ?: ""
            val params = listOfNotNull(
                noteParam.takeIf { it.isNotEmpty() },
                gameParam.takeIf { it.isNotEmpty() }
            ).joinToString("&")
            return "note${if (params.isNotEmpty()) "?$params" else ""}"
        }
    }
    object Search : Screen("search")
    object Settings : Screen("settings")
}

@Composable
fun GameNoteNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGame = { gameId ->
                    navController.navigate(Screen.GameDetail.createRoute(gameId))
                },
                onNavigateToNewNote = {
                    navController.navigate(Screen.NoteEditor.createRoute())
                },
                onNavigateToNote = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId = noteId))
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }

        composable(Screen.Games.route) {
            GamesScreen(
                onNavigateToGame = { gameId ->
                    navController.navigate(Screen.GameDetail.createRoute(gameId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.GameDetail.route,
            arguments = listOf(navArgument("gameId") { type = NavType.LongType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getLong("gameId") ?: return@composable
            GameDetailScreen(
                gameId = gameId,
                onNavigateToNote = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId = noteId))
                },
                onNewNote = {
                    navController.navigate(Screen.NoteEditor.createRoute(gameId = gameId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("gameId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId")?.takeIf { it != -1L }
            val gameId = backStackEntry.arguments?.getLong("gameId")?.takeIf { it != -1L }
            NoteEditorScreen(
                noteId = noteId,
                gameId = gameId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateToNote = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId = noteId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
