package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.NeoBottomBar
import com.example.ui.screens.CaptureScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ItemDetailScreen
import com.example.ui.screens.ProcessingScreen
import com.example.ui.screens.ReviewSaveScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.CreamBackground
import com.example.ui.viewmodel.MemoraViewModel

object Destinations {
    const val HOME = "home"
    const val TIMELINE = "timeline"
    const val CAPTURE = "capture"
    const val PROCESSING = "processing"
    const val REVIEW_SAVE = "review_save"
    const val SAVED = "saved"
    const val CATEGORIES = "categories"
    const val ITEM_DETAIL = "item_detail/{itemId}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    fun itemDetailRoute(itemId: Long): String = "item_detail/$itemId"
}

@Composable
fun MemoraApp(
    viewModel: MemoraViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Destinations.HOME,
        Destinations.TIMELINE,
        Destinations.CATEGORIES,
        Destinations.SEARCH
    )

    val currentDestination = when (currentRoute) {
        Destinations.TIMELINE -> BottomNavDestination.TIMELINE
        Destinations.CATEGORIES -> BottomNavDestination.CATEGORIES
        Destinations.SEARCH -> BottomNavDestination.SEARCH
        else -> BottomNavDestination.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CreamBackground,
        bottomBar = {
            if (showBottomBar) {
                NeoBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { destination ->
                        val targetRoute = when (destination) {
                            BottomNavDestination.HOME -> Destinations.HOME
                            BottomNavDestination.TIMELINE -> Destinations.TIMELINE
                            BottomNavDestination.CAPTURE -> Destinations.CAPTURE
                            BottomNavDestination.CATEGORIES -> Destinations.CATEGORIES
                            BottomNavDestination.SEARCH -> Destinations.SEARCH
                        }

                        if (targetRoute == Destinations.CAPTURE) {
                            navController.navigate(Destinations.CAPTURE)
                        } else {
                            navController.navigate(targetRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier
                .fillMaxSize()
                .then(if (showBottomBar) Modifier.padding(bottom = innerPadding.calculateBottomPadding()) else Modifier)
        ) {
            composable(Destinations.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCapture = { navController.navigate(Destinations.CAPTURE) },
                    onNavigateToDetail = { id -> navController.navigate(Destinations.itemDetailRoute(id)) },
                    onNavigateToSettings = { navController.navigate(Destinations.SETTINGS) },
                    onNavigateToFriends = { navController.navigate(Destinations.SEARCH) }
                )
            }

            composable(Destinations.TIMELINE) {
                TimelineScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> navController.navigate(Destinations.itemDetailRoute(id)) }
                )
            }

            composable(Destinations.CAPTURE) {
                CaptureScreen(
                    viewModel = viewModel,
                    onClose = { navController.popBackStack() },
                    onNavigateToProcessing = { navController.navigate(Destinations.PROCESSING) },
                    onNavigateToReview = {
                        navController.navigate(Destinations.REVIEW_SAVE) {
                            popUpTo(Destinations.PROCESSING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Destinations.PROCESSING) {
                ProcessingScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToReview = {
                        navController.navigate(Destinations.REVIEW_SAVE) {
                            popUpTo(Destinations.PROCESSING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Destinations.REVIEW_SAVE) {
                ReviewSaveScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSavedSuccess = {
                        navController.navigate(Destinations.SAVED) {
                            popUpTo(Destinations.HOME)
                        }
                    }
                )
            }

            composable(Destinations.SAVED) {
                SavedScreen(
                    viewModel = viewModel,
                    onViewItem = { id ->
                        navController.navigate(Destinations.itemDetailRoute(id)) {
                            popUpTo(Destinations.HOME)
                        }
                    },
                    onCaptureAnother = {
                        navController.navigate(Destinations.CAPTURE) {
                            popUpTo(Destinations.HOME)
                        }
                    },
                    onGoHome = {
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.HOME) { inclusive = true }
                        }
                    }
                )
            }

            composable(Destinations.CATEGORIES) {
                CategoriesScreen(
                    viewModel = viewModel,
                    onCategoryClick = { categoryId ->
                        viewModel.setSelectedFilter("All")
                        viewModel.setSearchQuery("")
                        navController.navigate(Destinations.TIMELINE)
                    }
                )
            }

            composable(
                route = Destinations.ITEM_DETAIL,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
                ItemDetailScreen(
                    itemId = itemId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Destinations.SEARCH) {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> navController.navigate(Destinations.itemDetailRoute(id)) }
                )
            }

            composable(Destinations.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
