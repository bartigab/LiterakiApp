package com.example.literakiapp.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Nick : Screen("nick")
    object MainMenu : Screen("main_menu")
    object CreateGame : Screen("create_game")
    object JoinGame : Screen("join_game")
    object OpenGames : Screen("open_games")
    object MyGames : Screen("my_games")
    object GameDetails : Screen("game_details/{gameId}") {
        const val ARG_GAME_ID = "gameId"

        fun createRoute(gameId: Int): String = "game_details/$gameId"
    }
}

