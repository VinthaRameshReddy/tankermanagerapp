package com.tankermanager.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tankermanager.app.data.api.ApiClient
import com.tankermanager.app.navigation.Routes
import com.tankermanager.app.ui.SplashScreen
import com.tankermanager.app.ui.admin.SuperAdminScreen
import com.tankermanager.app.ui.auth.AuthScreen
import com.tankermanager.app.ui.auth.SecuritySetupScreen
import com.tankermanager.app.ui.auth.UnlockScreen
import com.tankermanager.app.ui.driver.DriverHomeScreen
import com.tankermanager.app.ui.manager.ManagerShell
import com.tankermanager.app.ui.manager.TripDetailScreen
import com.tankermanager.app.ui.theme.TankerTheme
import com.tankermanager.app.ui.track.TrackScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepToken = intent?.data?.lastPathSegment
        setContent {
            TankerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TankerNav(deepToken)
                }
            }
        }
    }
}

@Composable
private fun TankerNav(deepToken: String?) {
    val nav = rememberNavController()
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as TankerApp
    val repo = app.repository
    val scope = rememberCoroutineScope()

    fun goHome(role: String?) {
        val dest = when (role) {
            "SUPER_ADMIN" -> Routes.Admin.route
            "DRIVER" -> Routes.Driver.route
            else -> Routes.Manager.route
        }
        nav.navigate(dest) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun logout() {
        scope.launch {
            repo.session().clear()
            ApiClient.reset()
            nav.navigate(Routes.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = nav,
        startDestination = Routes.Splash.route,
        enterTransition = {
            fadeIn(tween(280)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start, tween(280)
            )
        },
        exitTransition = {
            fadeOut(tween(220))
        }
    ) {
        composable(Routes.Splash.route) {
            SplashScreen(repo) { dest ->
                if (deepToken != null) {
                    nav.navigate(Routes.Track.create(deepToken)) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                } else {
                    nav.navigate(dest) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                }
            }
        }
        composable(Routes.Auth.route) {
            AuthScreen(
                repo = repo,
                onLoggedIn = { role, needsSecuritySetup ->
                    if (needsSecuritySetup) {
                        nav.navigate(Routes.SecuritySetup.route) {
                            popUpTo(Routes.Auth.route) { inclusive = true }
                        }
                    } else {
                        goHome(role)
                    }
                },
                onTrackTap = { nav.navigate(Routes.Track.create()) }
            )
        }
        composable(Routes.Unlock.route) {
            UnlockScreen(
                repo = repo,
                onUnlocked = { goHome(it) },
                onUsePassword = {
                    scope.launch {
                        repo.session().clear()
                        ApiClient.reset()
                        nav.navigate(Routes.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Routes.SecuritySetup.route) {
            SecuritySetupScreen(
                repo = repo,
                onDone = {
                    scope.launch {
                        val role = repo.session().role.first()
                        goHome(role)
                    }
                }
            )
        }
        composable(Routes.Admin.route) {
            SuperAdminScreen(repo = repo, onLogout = { logout() })
        }
        composable(Routes.Manager.route) {
            ManagerShell(
                repo = repo,
                onLogout = { logout() },
                onOpenTrip = { id -> nav.navigate(Routes.TripDetail.create(id)) },
                onOpenTrack = { token -> nav.navigate(Routes.Track.create(token)) }
            )
        }
        composable(Routes.Driver.route) {
            DriverHomeScreen(repo = repo, onLogout = { logout() })
        }
        composable(
            route = "trip/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            TripDetailScreen(
                tripId = id,
                repo = repo,
                onBack = { nav.popBackStack() },
                onTrack = { token -> nav.navigate(Routes.Track.create(token)) }
            )
        }
        composable(
            route = "track?token={token}",
            arguments = listOf(navArgument("token") {
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            })
        ) { entry ->
            TrackScreen(
                repo = repo,
                initialToken = entry.arguments?.getString("token"),
                onBack = {
                    if (!nav.popBackStack()) {
                        nav.navigate(Routes.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
