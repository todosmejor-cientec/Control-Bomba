package com.example.pumpcontrol.ui.screen


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pumpcontrol.model.UserRole
import com.example.pumpcontrol.viewmodel.PumpViewModel
import com.example.pumpcontrol.R
import com.example.pumpcontrol.model.UsuarioData // ✅
import com.example.pumpcontrol.ui.components.PumpTopAppBar
import com.example.pumpcontrol.ui.theme.AdminGreen
import com.example.pumpcontrol.ui.theme.ErrorRed
import com.example.pumpcontrol.ui.theme.InvitadoGray
import com.example.pumpcontrol.ui.theme.SuperAdminPurple

import kotlinx.coroutines.launch


@Composable
fun ConfiguracionScreen(
    viewModel: PumpViewModel = hiltViewModel(),
    onBackPress: () -> Unit
) {
    val usuarios by viewModel.usuarios.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current


    LaunchedEffect(userRole) {
        if (userRole == UserRole.SUPERADMIN) {
            viewModel.cargarUsuarios()
        }
    }

    Scaffold(
        topBar = {
            PumpTopAppBar(
                title = stringResource(R.string.user_management),
                onBackClick = onBackPress
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // ✅ Scroll agregado
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (userRole != UserRole.SUPERADMIN) {
                        Text(
                            text = stringResource(R.string.restricted_access),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        return@Column
                    }

                    Text(
                        text = stringResource(R.string.registered_users),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (usuarios.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_users_found),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        usuarios.forEach { usuario ->
                            UsuarioItem(
                                usuario = usuario,
                                onRolChange = { nuevoRol ->
                                    viewModel.cambiarRolUsuario(usuario.uid, nuevoRol) { exito ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (exito) context.getString(R.string.role_changed, nuevoRol)
                                                else context.getString(R.string.role_change_failed)
                                            )
                                        }
                                    }
                                },
                                onDelete = {
                                    viewModel.eliminarUsuario(usuario.uid) { exito ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (exito) context.getString(R.string.user_deleted)
                                                else context.getString(R.string.user_delete_failed)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsuarioItem(
    usuario: UsuarioData,
    onRolChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val opcionesCambioRol = when (usuario.rol.lowercase()) {
        "invitado" -> listOf("admin")
        "admin" -> listOf("invitado", "superadmin")
        "superadmin" -> listOf("admin")
        else -> emptyList()
    }

    val (colorRol, rolTag) = when (usuario.rol.lowercase()) {
        "invitado" -> InvitadoGray to stringResource(R.string.role_invitado)
        "admin" -> AdminGreen to stringResource(R.string.role_admin)
        "superadmin" -> SuperAdminPurple to stringResource(R.string.role_superadmin)
        else -> Color.Gray to usuario.rol.uppercase()
    }

    var showDialogEliminar by remember { mutableStateOf(false) }
    var showDialogCambioRol by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var nuevoRolSeleccionado by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.user_icon_description),
                    tint = colorRol,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(usuario.correo, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.current_role, rolTag),
                        color = colorRol,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()

            // Cambiar rol
            Text(
                text = stringResource(R.string.change_role),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { showMenu = true },
                    enabled = opcionesCambioRol.isNotEmpty()
                ) {
                    Text(stringResource(R.string.select_new_role))
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    opcionesCambioRol.forEach { nuevoRol ->
                        DropdownMenuItem(
                            text = { Text(nuevoRol.uppercase()) },
                            onClick = {
                                nuevoRolSeleccionado = nuevoRol
                                showMenu = false
                                showDialogCambioRol = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.delete_user),
                fontWeight = FontWeight.Bold,
                color = ErrorRed
            )
            OutlinedButton(
                onClick = { showDialogEliminar = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    }

    if (showDialogCambioRol && nuevoRolSeleccionado != null) {
        AlertDialog(
            onDismissRequest = { showDialogCambioRol = false },
            title = { Text(stringResource(R.string.confirm_role_change)) },
            text = {
                Text(stringResource(R.string.change_role_confirmation, nuevoRolSeleccionado!!.uppercase(), usuario.correo))
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialogCambioRol = false
                    onRolChange(nuevoRolSeleccionado!!)
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogCambioRol = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    if (showDialogEliminar) {
        AlertDialog(
            onDismissRequest = { showDialogEliminar = false },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_text, usuario.correo)) },
            confirmButton = {
                TextButton(onClick = {
                    showDialogEliminar = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.delete), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogEliminar = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }
}
