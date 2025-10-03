package com.example.pumpcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pumpcontrol.navigation.Screen
import com.example.pumpcontrol.ui.components.AppDrawer
import com.example.pumpcontrol.ui.screen.BombaScreen
import com.example.pumpcontrol.ui.screen.HomeScreen
import com.example.pumpcontrol.ui.screen.NivelScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawer(
                            navController = nav,
                            onDestinationClicked = { route ->
                                scope.launch { drawerState.close() }
                                nav.navigate(route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    // limpiamos si saltamos a Home
                                    if (route == Screen.Home.route) popUpTo(0)
                                }
                            }
                        )
                    }
                ) {
                    Scaffold { pad ->
                        NavHost(
                            navController = nav,
                            startDestination = Screen.Home.route,
                            modifier = Modifier.padding(pad)
                        ) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    onOpenMenu = { scope.launch { drawerState.open() } },
                                    onGoNivel = { nav.navigate(Screen.Nivel.route) },
                                    onGoBomba = { nav.navigate(Screen.Bomba.route) }
                                )
                            }
                            composable(Screen.Nivel.route) {
                                NivelScreen(
                                    onBack = {
                                        // volvemos a Home
                                        nav.popBackStack(route = Screen.Home.route, inclusive = false)
                                    }
                                )
                            }
                            composable(Screen.Bomba.route) {
                                BombaScreen(
                                    onBack = {
                                        nav.popBackStack(route = Screen.Home.route, inclusive = false)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
