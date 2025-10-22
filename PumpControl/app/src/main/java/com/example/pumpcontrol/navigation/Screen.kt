package com.example.pumpcontrol.navigation

import com.example.pumpcontrol.R


sealed class Screen(val route: String, val titleResId: Int? = null) {
    data object AppStart : Screen("app_start")
    data object Login : Screen("login", R.string.login)
    data object Home : Screen("home", R.string.home)
    data object Perfil : Screen("perfil", R.string.perfil)
    data object Bomba : Screen("bomba", R.string.bomba)
    data object Nivel : Screen("nivel", R.string.nivel)


    data object Configuracion : Screen("configuracion", R.string.configuracion)
    data object Historial : Screen("historial", R.string.historial)
    data object Logout : Screen("logout", R.string.logout)
}
