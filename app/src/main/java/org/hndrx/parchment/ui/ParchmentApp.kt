package org.hndrx.parchment.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import org.hndrx.parchment.ui.details.DetailsScreen
import org.hndrx.parchment.ui.library.LibraryScreen
import org.hndrx.parchment.ui.library.CollectionsScreen
import org.hndrx.parchment.ui.reader.ReaderScreen
import org.hndrx.parchment.ui.settings.SettingsScreen
import org.hndrx.parchment.ui.settings.AboutScreen
import org.hndrx.parchment.ui.theme.ParchmentTheme
import org.hndrx.parchment.ui.settings.AppearanceScreen
import org.hndrx.parchment.ui.settings.CollectionAppearanceScreen
import org.hndrx.parchment.ui.settings.RecoveryScreen
import org.hndrx.parchment.ui.profile.ProfileScreen

@Composable
fun ParchmentApp(viewModel: LibraryViewModel = viewModel(), onAnimationsChanged: (Boolean) -> Unit = {}) {
    val preferences by viewModel.readerPreferences.collectAsStateWithLifecycle()
    val library by viewModel.state.collectAsStateWithLifecycle()
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()
    SideEffect { onAnimationsChanged(preferences.animationsEnabled) }
    ParchmentTheme(preferences.appTheme, preferences.palette) {
        // Keep an opaque themed surface underneath every navigation transition.
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var showSplash by rememberSaveable { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                if (showSplash) {
                    delay(1_200)
                    showSplash = false
                }
            }
            val nav = rememberNavController()
            if (showSplash || !library.initialized) {
                SplashScreen(animationsEnabled = preferences.animationsEnabled, profile = profile)
            } else {
                NavHost(
                    navController = nav, startDestination = "library",
                    enterTransition = { if (preferences.animationsEnabled) fadeIn() else EnterTransition.None },
                    exitTransition = { if (preferences.animationsEnabled) fadeOut() else ExitTransition.None },
                    popEnterTransition = { if (preferences.animationsEnabled) fadeIn() else EnterTransition.None },
                    popExitTransition = { if (preferences.animationsEnabled) fadeOut() else ExitTransition.None }
                ) {
                    composable("library") {
                        LibraryScreen(
                            viewModel = viewModel,
                            openBook = { nav.navigate("reader/$it") },
                            editBook = { nav.navigate("details/$it") },
                            openSettings = { nav.navigate("settings") },
                            openProfile = { nav.navigate("profile") },
                            openCollections = { nav.navigate("collections") { launchSingleTop = true } }
                        )
                    }
                    composable("collections") {
                        CollectionsScreen(viewModel, back = nav::popBackStack, openCollection = { nav.navigate("collection/$it") { launchSingleTop = true } }, customize = { nav.navigate("collectionAppearance") })
                    }
                    composable("collection/{id}") {
                        LibraryScreen(
                            viewModel = viewModel,
                            openBook = { nav.navigate("reader/$it") },
                            editBook = { nav.navigate("details/$it") },
                            openSettings = { nav.navigate("settings") },
                            openProfile = { nav.navigate("profile") },
                            openCollections = { nav.popBackStack("collections", false) },
                            collectionId = it.arguments?.getString("id"), back = nav::popBackStack
                        )
                    }
                    composable("reader/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                        ReaderScreen(id = it.arguments?.getString("id")!!, viewModel = viewModel, back = nav::popBackStack)
                    }
                    composable("details/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                        DetailsScreen(id = it.arguments?.getString("id")!!, viewModel = viewModel, back = nav::popBackStack)
                    }
                    composable("settings") {
                        SettingsScreen(viewModel = viewModel, back = nav::popBackStack, openAbout = { nav.navigate("about") },
                            openAppearance = { nav.navigate("appearance") }, openCollectionAppearance = { nav.navigate("collectionAppearance") }, openRecovery = { nav.navigate("recovery") })
                    }
                    composable("about") { AboutScreen(back = nav::popBackStack) }
                    composable("appearance") { AppearanceScreen(viewModel, nav::popBackStack) }
                    composable("collectionAppearance") { CollectionAppearanceScreen(viewModel, nav::popBackStack) }
                    composable("recovery") { RecoveryScreen(viewModel, nav::popBackStack) }
                    composable("profile") { ProfileScreen(viewModel, nav::popBackStack, { nav.navigate("reader/$it") }) }
                }
            }
        }
    }
}
