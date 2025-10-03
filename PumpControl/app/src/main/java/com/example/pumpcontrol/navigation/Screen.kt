package com.example.pumpcontrol.navigation

import com.example.pumpcontrol.R

sealed class Screen(val route: String, val titleResId: Int? = null) {
    object Home : Screen("home", R.string.home)
    object Nivel : Screen("nivel", R.string.nivel)
    object Bomba : Screen("bomba", R.string.bomba)
}
