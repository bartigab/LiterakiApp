package com.example.literakiapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.literakiapp.navigation.Screen
import com.example.literakiapp.screens.CreateGameScreen
import com.example.literakiapp.screens.GameDetailsScreen
import com.example.literakiapp.screens.JoinGameScreen
import com.example.literakiapp.screens.MainMenuScreen
import com.example.literakiapp.screens.MyGamesScreen
import com.example.literakiapp.screens.NickScreen
import com.example.literakiapp.screens.OpenGamesScreen
import com.example.literakiapp.screens.SplashScreen
import com.example.literakiapp.ui.theme.LiterakiAppTheme
import com.example.literakiapp.ui.viewmodel.CreateGameViewModel
import com.example.literakiapp.ui.viewmodel.GameDetailsViewModel
import com.example.literakiapp.ui.viewmodel.GamesViewModel
import com.example.literakiapp.ui.viewmodel.JoinGameViewModel
import com.example.literakiapp.ui.viewmodel.MainMenuViewModel
import com.example.literakiapp.ui.viewmodel.NickViewModel
import com.example.literakiapp.ui.viewmodel.SplashDestination
import com.example.literakiapp.ui.viewmodel.SplashViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiterakiAppTheme {
                val navController = rememberNavController()
                LiterakiNavGraphHost(navController = navController)
            }
        }
    }
}

@Composable
private fun LiterakiNavGraphHost(navController: NavHostController) {
    val repository = LocalContext.current.appContainer.repository

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            val viewModel: SplashViewModel = viewModel(
                factory = SplashViewModel.factory(repository)
            )
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.destination) {
                when (uiState.destination) {
                    SplashDestination.NICK -> {
                        navController.navigate(Screen.Nick.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }

                    SplashDestination.MAIN_MENU -> {
                        navController.navigate(Screen.MainMenu.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }

                    null -> Unit
                }
            }

            SplashScreen(uiState = uiState)
        }

        composable(Screen.Nick.route) {
            val viewModel: NickViewModel = viewModel(
                factory = NickViewModel.factory(repository)
            )
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.loginCompleted) {
                if (uiState.loginCompleted) {
                    navController.navigate(Screen.MainMenu.route) {
                        popUpTo(Screen.Nick.route) { inclusive = true }
                    }
                }
            }

            NickScreen(
                uiState = uiState,
                onNickChanged = viewModel::onNickChanged,
                onSubmit = viewModel::signInAsGuest
            )
        }

        composable(Screen.MainMenu.route) {
            val viewModel: MainMenuViewModel = viewModel(
                factory = MainMenuViewModel.factory(repository)
            )
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.sessionExpired) {
                if (uiState.sessionExpired) {
                    navController.navigate(Screen.Nick.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            MainMenuScreen(
                uiState = uiState,
                onCreateGame = { navController.navigate(Screen.CreateGame.route) },
                onJoinGame = { navController.navigate(Screen.JoinGame.route) },
                onOpenGames = { navController.navigate(Screen.OpenGames.route) },
                onMyGames = { navController.navigate(Screen.MyGames.route) },
                onRefresh = viewModel::refresh,
                onLogout = viewModel::logout
            )
        }

        composable(Screen.CreateGame.route) {
            val viewModel: CreateGameViewModel = viewModel(
                factory = CreateGameViewModel.factory(repository)
            )
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.sessionExpired) {
                if (uiState.sessionExpired) {
                    navController.navigate(Screen.Nick.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            CreateGameScreen(
                uiState = uiState,
                onCreateGame = viewModel::createGame,
                onBack = { navController.popBackStack() },
                onOpenGame = { gameId ->
                    navController.navigate(Screen.GameDetails.createRoute(gameId))
                }
            )
        }

        composable(Screen.JoinGame.route) {
            val viewModel: JoinGameViewModel = viewModel(
                factory = JoinGameViewModel.factory(repository)
            )
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.sessionExpired) {
                if (uiState.sessionExpired) {
                    navController.navigate(Screen.Nick.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            JoinGameScreen(
                uiState = uiState,
                onCodeChanged = viewModel::onCodeChanged,
                onJoinGame = viewModel::joinGame,
                onBack = { navController.popBackStack() },
                onOpenGame = { gameId ->
                    navController.navigate(Screen.GameDetails.createRoute(gameId))
                }
            )
        }

        composable(Screen.OpenGames.route) {
            val viewModel: GamesViewModel = viewModel(
                factory = GamesViewModel.factory(repository)
            )
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.sessionExpired) {
                if (uiState.sessionExpired) {
                    navController.navigate(Screen.Nick.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            OpenGamesScreen(
                uiState = uiState,
                onRefresh = viewModel::refresh,
                onBack = { navController.popBackStack() },
                onOpenGame = { gameId ->
                    navController.navigate(Screen.GameDetails.createRoute(gameId))
                }
            )
        }

        composable(Screen.MyGames.route) {
            val viewModel: GamesViewModel = viewModel(
                factory = GamesViewModel.factory(repository)
            )
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.sessionExpired) {
                if (uiState.sessionExpired) {
                    navController.navigate(Screen.Nick.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            MyGamesScreen(
                uiState = uiState,
                onRefresh = viewModel::refresh,
                onBack = { navController.popBackStack() },
                onOpenGame = { gameId ->
                    navController.navigate(Screen.GameDetails.createRoute(gameId))
                }
            )
        }

        composable(
            route = Screen.GameDetails.route,
            arguments = listOf(
                navArgument(Screen.GameDetails.ARG_GAME_ID) {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getInt(Screen.GameDetails.ARG_GAME_ID)
                ?: return@composable

            val viewModel: GameDetailsViewModel = viewModel(
                key = "game-details-$gameId",
                factory = GameDetailsViewModel.factory(repository, gameId)
            )
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.sessionExpired) {
                if (uiState.sessionExpired) {
                    navController.navigate(Screen.Nick.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            GameDetailsScreen(
                uiState = uiState,
                onRefresh = viewModel::refresh,
                onStartGame = viewModel::startGame,
                onPassTurn = viewModel::passTurn,
                onResignGame = viewModel::resignGame,
                onRackTileSelectionChanged = viewModel::onRackTileSelectionChanged,
                onExchangeTiles = viewModel::exchangeSelectedTiles,
                onTileChanged = viewModel::onDraftTileChanged,
                onClearDraft = viewModel::clearDraftTiles,
                onPlaceTiles = viewModel::placeTiles,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
