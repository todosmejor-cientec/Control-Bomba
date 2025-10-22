package com.example.pumpcontrol.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.pumpcontrol.R
import com.example.pumpcontrol.navigation.Screen

@Composable
fun AppDrawer(
    navController: NavController,
    onDestinationClicked: (String) -> Unit,
    onLogout: () -> Unit,                    // ← NUEVO
    modifier: Modifier = Modifier
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    ModalDrawerSheet(
        modifier = modifier.fillMaxHeight(),
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(R.string.menu_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = primary
            )
        }

        DrawerItem(
            selected = currentRoute == Screen.Home.route,
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = stringResource(R.string.menu_inicio),
            color = primary,
            onClick = { onDestinationClicked(Screen.Home.route) }
        )
        DrawerItem(
            selected = currentRoute == Screen.Nivel.route,
            icon = { Icon(Icons.Outlined.WaterDrop, contentDescription = null) },
            label = stringResource(R.string.menu_nivel),
            color = onSurfaceVariant,
            onClick = { onDestinationClicked(Screen.Nivel.route) }
        )
        DrawerItem(
            selected = currentRoute == Screen.Bomba.route,
            icon = { Icon(Icons.Outlined.Power, contentDescription = null) },
            label = stringResource(R.string.menu_bomba),
            color = primary,
            onClick = { onDestinationClicked(Screen.Bomba.route) }
        )

        Spacer(Modifier.height(24.dp))

        // Cerrar sesión
        NavigationDrawerItem(
            selected = false,
            onClick = onLogout,
            icon = { Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = null) },
            label = { Text(stringResource(R.string.logout)) },
            modifier = Modifier.padding(horizontal = 12.dp),
            colors = NavigationDrawerItemDefaults.colors(
                unselectedTextColor = Color.Red,
                unselectedIconColor = Color.Red,
                unselectedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun DrawerItem(
    selected: Boolean,
    icon: @Composable () -> Unit,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = { Text(label) },
        modifier = Modifier.padding(horizontal = 12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = color.copy(alpha = 0.12f),
            selectedTextColor = color,
            selectedIconColor = color,
            unselectedTextColor = color,
            unselectedIconColor = color,
            unselectedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
