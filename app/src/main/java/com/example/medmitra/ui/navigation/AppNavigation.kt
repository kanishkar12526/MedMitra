package com.example.medmitra.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.medmitra.data.local.UserPreferencesManager
import com.example.medmitra.sos.SOSViewModel
import com.example.medmitra.ui.components.GlassBackground
import com.example.medmitra.ui.components.SOSDialog
import com.example.medmitra.ui.screens.CameraScreen
import com.example.medmitra.ui.screens.DashboardScreen
import com.example.medmitra.ui.screens.LoginScreen
import com.example.medmitra.ui.screens.SettingsScreen

enum class TopLevelDestination(val title: String, val icon: @Composable () -> Unit, val route: NavKey) {
    Dashboard("Dashboard", { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") }, DashboardRoute),
    Camera("Camera", { Icon(Icons.Filled.CameraAlt, contentDescription = "Camera") }, CameraRoute),
    Settings("Settings", { Icon(Icons.Filled.Settings, contentDescription = "Settings") }, SettingsRoute)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    sosViewModel: SOSViewModel = viewModel()
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferencesManager.getInstance(context) }
    val isLoggedIn by userPrefs.isLoggedInFlow.collectAsState()

    val initialRoute = if (isLoggedIn) DashboardRoute else LoginRoute
    val backStack = rememberNavBackStack(initialRoute)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    var currentDestination by remember {
        mutableStateOf(if (isLoggedIn) TopLevelDestination.Dashboard else null)
    }

    val sosState by sosViewModel.sosState.collectAsState()

    fun navigateTo(route: NavKey) {
        if (backStack.isEmpty()) {
            backStack.add(route)
        } else {
            backStack[0] = route
            while (backStack.size > 1) {
                backStack.removeAt(backStack.size - 1)
            }
        }
    }

    // Synchronize auth state changes safely
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            if (currentDestination == null) {
                currentDestination = TopLevelDestination.Dashboard
            }
            if (backStack.lastOrNull() == LoginRoute) {
                navigateTo(DashboardRoute)
            }
        } else {
            currentDestination = null
            if (backStack.lastOrNull() != LoginRoute) {
                navigateTo(LoginRoute)
            }
        }
    }

    // Global SOS overlay dialog
    SOSDialog(
        sosState = sosState,
        onCancel = { sosViewModel.cancelSOS() },
        onCallNow = { sosViewModel.triggerEmergencyImmediately(context) },
        onDismissDispatched = { sosViewModel.dismissDispatched() }
    )

    val globalEntryProvider = entryProvider {
        entry<LoginRoute> {
            LoginScreen(
                onLoginSuccess = {
                    currentDestination = TopLevelDestination.Dashboard
                    navigateTo(DashboardRoute)
                }
            )
        }
        entry<DashboardRoute> {
            DashboardScreen(
                onTriggerSOS = { sosViewModel.triggerSOS() }
            )
        }
        entry<CameraRoute> {
            CameraScreen(
                onNavigateToDashboard = {
                    currentDestination = TopLevelDestination.Dashboard
                    navigateTo(DashboardRoute)
                }
            )
        }
        entry<SettingsRoute> {
            SettingsScreen(
                sosViewModel = sosViewModel,
                onLogout = {
                    userPrefs.logout()
                    currentDestination = null
                    navigateTo(LoginRoute)
                }
            )
        }
    }

    val isSeniorModeEnabled by userPrefs.seniorModeFlow.collectAsState()
    val appLanguage by userPrefs.appLanguageFlow.collectAsState()
    val isTamil = appLanguage == "ta"

    GlassBackground {
        if (!isLoggedIn || currentDestination == null) {
            // Full-screen navigation display without bottom bar for unauthenticated login screen
            NavDisplay(
                backStack = backStack,
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeAt(backStack.size - 1)
                    }
                },
                sceneStrategy = listDetailStrategy,
                entryProvider = globalEntryProvider
            )
        } else {
            NavigationSuiteScaffold(
                modifier = modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = Color.White,
                navigationSuiteColors = NavigationSuiteDefaults.colors(
                    navigationBarContainerColor = Color(0xFF0F172A).copy(alpha = 0.92f),
                    navigationRailContainerColor = Color(0xFF0F172A).copy(alpha = 0.92f)
                ),
                navigationSuiteItems = {
                    TopLevelDestination.entries.forEach { dest ->
                        val localizedTitle = when (dest) {
                            TopLevelDestination.Dashboard -> if (isTamil) "முகப்பு" else "Dashboard"
                            TopLevelDestination.Camera -> if (isTamil) "கேமரா" else "Camera"
                            TopLevelDestination.Settings -> if (isTamil) "அமைப்புகள்" else "Settings"
                        }
                        item(
                            icon = {
                                Box(
                                    modifier = Modifier.padding(vertical = if (isSeniorModeEnabled) 4.dp else 0.dp)
                                ) {
                                    dest.icon()
                                }
                            },
                            label = {
                                Text(
                                    text = localizedTitle,
                                    style = if (isSeniorModeEnabled) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
                                    fontWeight = if (currentDestination == dest) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            },
                            selected = currentDestination == dest,
                            onClick = {
                                currentDestination = dest
                                navigateTo(dest.route)
                            }
                        )
                    }
                }
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    },
                    sceneStrategy = listDetailStrategy,
                    entryProvider = globalEntryProvider
                )
            }
        }
    }
}
