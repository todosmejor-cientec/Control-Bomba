package com.example.pumpcontrol


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import com.example.pumpcontrol.navigation.AppNavigation
import com.example.pumpcontrol.ui.theme.PumpControlPumpTheme
import com.example.pumpcontrol.navigation.Screen

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PumpControlPumpTheme {
                AppNavigation(Screen.AppStart.route)
            }
        }
    }
}
