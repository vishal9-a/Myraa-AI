package com.myraa.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myraa.assistant.data.NovaAppState
import com.myraa.assistant.data.rememberNovaAppState
import com.myraa.assistant.forge.ForgeController
import com.myraa.assistant.offline.MyraaListenService
import com.myraa.assistant.offline.ModeManager
import com.myraa.assistant.ui.components.MyraaBottomNavigation
import com.myraa.assistant.ui.fx.ListeningEdgeGlow
import com.myraa.assistant.ui.navigation.registerFlavorRoutes
import com.myraa.assistant.ui.screens.MemoryManagerScreen
import com.myraa.assistant.ui.screens.chat.ChatScreen
import com.myraa.assistant.ui.screens.forge.ForgeScreen
import com.myraa.assistant.ui.screens.history.HistoryScreen
import com.myraa.assistant.ui.screens.home.HomeScreen
import com.myraa.assistant.ui.screens.onboarding.OnboardingScreen
import com.myraa.assistant.ui.screens.quickactions.QuickActionsScreen
import com.myraa.assistant.ui.screens.settings.AboutScreen
import com.myraa.assistant.ui.screens.settings.DebugScreen
import com.myraa.assistant.ui.screens.settings.OrbStyleScreen
import com.myraa.assistant.ui.screens.settings.PermissionsScreen
import com.myraa.assistant.ui.screens.settings.PersonalityScreen
import com.myraa.assistant.ui.screens.settings.PrivacyScreen
import com.myraa.assistant.ui.screens.settings.ProfileScreen
import com.myraa.assistant.ui.screens.settings.SettingsScreen
import com.myraa.assistant.ui.screens.splash.MyraaSplash3D
import com.myraa.assistant.ui.screens.splash.SplashSession
import com.myraa.assistant.ui.screens.voice.VoiceScreen
import com.myraa.assistant.ui.theme.LocalBottomBarInset
import com.myraa.assistant.ui.theme.NovaTheme
import com.myraa.assistant.ui.theme.nightSky

class MainActivity : ComponentActivity() {

    companion object {
        var appContext: Context? = null
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appContext = applicationContext

        ModeManager.init(this)

        if (intent?.getBooleanExtra("EXTRA_ACTIVATE_HANDS_FREE", false) == true) {
            MyraaListenService.start(this)
        }

        setContent {
            val appState = rememberNovaAppState(this)

            NovaTheme(
                mode = appState.themeMode,
                reducedMotion = appState.reducedMotion
            ) {
                var showSplash by remember { mutableStateOf(!SplashSession.played) }

                Box(modifier = Modifier.fillMaxSize().nightSky()) {
                    NovaApp(
                        appState = appState,
                        onOpenVoice = { /* handled in NavHost */ }
                    )

                    ListeningEdgeGlow()

                    if (showSplash) {
                        MyraaSplash3D(
                            onFinished = { showSplash = false }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_OPEN_FORGE", false)) {
            ForgeController.requestOpen()
        }
        if (intent.getBooleanExtra("EXTRA_ACTIVATE_HANDS_FREE", false)) {
            MyraaListenService.start(this)
        }
    }
}

@Composable
fun NovaApp(
    appState: NovaAppState,
    onOpenVoice: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val pendingForgeOpen by ForgeController.pendingOpen.collectAsState()

    LaunchedEffect(pendingForgeOpen) {
        if (pendingForgeOpen) {
            ForgeController.consumePendingOpen()
            navController.navigate("forge") {
                launchSingleTop = true
            }
        }
    }

    val bottomNavRoutes = setOf("home", "chat", "history", "settings")
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                MyraaBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onTalkClick = {
                        navController.navigate("voice") {
                            launchSingleTop = true
                        }
                    }
                )
            }
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalBottomBarInset provides innerPadding.calculateBottomPadding()
        ) {
            NavHost(
                navController = navController,
                startDestination = if (appState.hasOnboarded) "home" else "onboarding",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("onboarding") {
                    OnboardingScreen(
                        appState = appState,
                        onComplete = {
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        appState = appState,
                        onNavigateToVoice = { navController.navigate("voice") { launchSingleTop = true } },
                        onNavigateToChat = { navController.navigate("chat") { launchSingleTop = true } },
                        onNavigateToProfile = { navController.navigate("profile") },
                        onNavigateToForge = { navController.navigate("forge") },
                        onPromptSelected = { phrase ->
                            navController.navigate("voice")
                        }
                    )
                }

                composable("voice") {
                    VoiceScreen(
                        appState = appState,
                        onClose = { navController.popBackStack() },
                        onOpenSettings = { navController.navigate("settings") }
                    )
                }

                composable("chat") {
                    ChatScreen(
                        appState = appState,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenForge = { navController.navigate("forge") }
                    )
                }

                composable("history") {
                    HistoryScreen(
                        appState = appState,
                        onNavigateBack = { navController.popBackStack() },
                        onStartNewChat = { navController.navigate("chat") }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        appState = appState,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPersonality = { navController.navigate("personality") },
                        onNavigateToOrbStyle = { navController.navigate("orb_style") },
                        onNavigateToPermissions = { navController.navigate("permissions") },
                        onNavigateToMemory = { navController.navigate("memory_manager") },
                        onNavigateToAbout = { navController.navigate("about") },
                        onNavigateToDebug = { navController.navigate("debug") }
                    )
                }

                composable("memory_manager") {
                    MemoryManagerScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("personality") {
                    PersonalityScreen(
                        appState = appState,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("orb_style") {
                    OrbStyleScreen(
                        appState = appState,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("permissions") {
                    PermissionsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("profile") {
                    ProfileScreen(
                        appState = appState,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("privacy") {
                    PrivacyScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("about") {
                    AboutScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("debug") {
                    DebugScreen(
                        appState = appState,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("quick_actions") {
                    QuickActionsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onOpenForge = { navController.navigate("forge") },
                        onTriggerVoice = { navController.navigate("voice") }
                    )
                }

                composable("forge") {
                    ForgeScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Register flavor-specific routes (e.g. accessibility disclosure in direct flavor)
                registerFlavorRoutes(navController)
            }
        }
    }
}
