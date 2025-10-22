package com.example.pumpcontrol.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.pumpcontrol.R
import com.example.pumpcontrol.navigation.Screen
import com.example.pumpcontrol.ui.components.PumpTopAppBar
import com.example.pumpcontrol.viewmodel.PumpViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    onBackPress: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val openDialog = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val email = user?.email ?: stringResource(R.string.unauthenticated_user)

    // Etiqueta de proveedor REAL (Google vs Email/Password)
    val providerLabel = remember(user, context) {
        val providers = user?.providerData.orEmpty()
            .map { it.providerId }
            .filter { it != "firebase" } // provider interno

        when {
            "google.com" in providers -> "Google"
            "password" in providers   -> context.getString(R.string.provider_email) // "Correo y contraseña"
            providers.isNotEmpty()    -> providers.first().replaceFirstChar { it.uppercaseChar() }
            else -> "N/A"
        }
    }

    val photoUrl = user?.photoUrl?.toString()

    // VM si quieres registrar en historial, etc.
    val vm: PumpViewModel = hiltViewModel()

    // Rol desde Firestore
    val rolUsuario = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user?.uid) {
        val uid = user?.uid ?: return@LaunchedEffect
        val docRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(uid)

        // 1) Intenta servidor
        val rol = runCatching {
            val snap = docRef.get(com.google.firebase.firestore.Source.SERVER).await()
            snap.getString("rol")
        }.getOrNull()
        // 2) Si falla (offline), intenta cache
            ?: runCatching {
                val snapCache = docRef.get(com.google.firebase.firestore.Source.CACHE).await()
                snapCache.getString("rol")
            }.getOrNull()
            // 3) Fallback seguro
            ?: "desconocido"

        rolUsuario.value = rol
    }


    Scaffold(
        topBar = {
            PumpTopAppBar(
                title = stringResource(R.string.profile_title),
                onBackClick = onBackPress
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!photoUrl.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(photoUrl),
                            contentDescription = stringResource(R.string.profile_picture),
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(text = email, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.provider_info, providerLabel),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(
                            R.string.user_role,
                            rolUsuario.value ?: stringResource(R.string.loading)
                        ),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { openDialog.value = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(R.string.logout),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.logout), color = Color.White)
            }

            if (openDialog.value) {
                AlertDialog(
                    onDismissRequest = { openDialog.value = false },
                    confirmButton = {
                        TextButton(onClick = {
                            openDialog.value = false
                            scope.launch {
                                // vm.registrarLogout(context) // opcional si ya lo tienes
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }) {
                            Text(stringResource(R.string.confirm_logout))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { openDialog.value = false }) {
                            Text(stringResource(R.string.cancelar))
                        }
                    },
                    title = { Text(stringResource(R.string.logout_title)) },
                    text = { Text(stringResource(R.string.logout_message)) }
                )
            }
        }
    }
}
