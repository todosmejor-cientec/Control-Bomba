// app/src/main/java/com/example/pumpcontrol/navigation/AppNavigation.kt
package com.example.pumpcontrol.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pumpcontrol.ui.screen.*

// Si usas tu sealed class Screen en com.example.pumpcontrol.navigation.Screen
// con: AppStart, Login, Home, Perfil, Bomba, Nivel, Configuracion, Historial, Logout

@Composable
fun AppNavigation(startDestination: String = Screen.AppStart.route) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- Arranque: decide si ir a Login o Home ---
        composable(Screen.AppStart.route) {
            AppStartScreen(navController = navController)
        }

        // --- Login ---
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        // --- Home ---
        // Tu HomeScreen (según tu código) usa callbacks: onOpenMenu, onGoNivel, onGoBomba
        composable(Screen.Home.route) {
            HomeScreen(
                onGoNivel = { navController.navigate(Screen.Nivel.route) },
                onGoBomba = { navController.navigate(Screen.Bomba.route) },
                onGoPerfil = { navController.navigate(Screen.Perfil.route) },
                onGoHistorial = { navController.navigate(Screen.Historial.route) },
                onGoConfiguracion = { navController.navigate(Screen.Configuracion.route) },
                onLogout = { navController.navigate(Screen.Logout.route) }
            )
        }

        // --- Perfil ---
        // AppNavigation.kt (o donde tengas tu NavHost)
        composable(Screen.Perfil.route) {
            ProfileScreen(
                navController = navController,
                onBackPress = { navController.popBackStack() }
            )
        }



        // --- Bomba ---
        // BombaScreen(onBack) -> usa onBack (según tu MainActivity previo)
        composable(Screen.Bomba.route) {
            BombaScreen(
                onBackPress = { navController.popBackStack() }
            )
        }

        // --- Nivel ---
        // NivelScreen(onBack) -> usa onBack (según tu MainActivity previo)
        composable(Screen.Nivel.route) {
            NivelScreen(
                onBackPress = { navController.popBackStack() }
            )
        }

        // --- Configuración ---
        // ConfiguracionScreen(onBackPress) -> usa onBackPress (como en tu snippet)
        composable(Screen.Configuracion.route) {
            ConfiguracionScreen(
                onBackPress = { navController.popBackStack() }
            )
        }

        // --- Historial ---


        composable(Screen.Historial.route) {
            HistorialScreen(
                onBackPress = { navController.popBackStack() }
            )
        }

        // --- Logout ---
        composable(Screen.Logout.route) {
            LogoutScreen(navController = navController)
        }
    }
}
