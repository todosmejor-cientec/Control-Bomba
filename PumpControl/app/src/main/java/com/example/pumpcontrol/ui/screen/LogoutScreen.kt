package com.example.pumpcontrol.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController

import com.example.pumpcontrol.navigation.Screen
import com.example.pumpcontrol.viewmodel.PumpViewModel
import com.google.firebase.auth.FirebaseAuth

import androidx.compose.ui.platform.LocalContext

@Composable
fun LogoutScreen(
    navController: NavHostController,
    viewModel: PumpViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.registrarLogout(context) {
            // ¡Solo una vez!
            FirebaseAuth.getInstance().signOut()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
