package com.example.pumpcontrol.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pumpcontrol.R
import com.example.pumpcontrol.ui.components.EditarSetPointDialog
import com.example.pumpcontrol.ui.theme.CustomRed
import com.example.pumpcontrol.viewmodel.PumpViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NivelScreen(
    onBack: () -> Unit,
    vm: PumpViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showSetpointDialog by remember { mutableStateOf(false) }

    val nivelActualBlue = Color(0xFF0478FC)
    val appBarBlue = Color(0xFF3B8EEC)

    val minForDialog = (ui.setMin ?: 0.0).toFloat()
    val maxForDialog = (ui.setMax ?: 100.0).toFloat()
    val nivelText = ui.nivelActual?.let { String.format(Locale.US, "%.2f", it) } ?: "--"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.sensor_titulo),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.atras),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarBlue)
            )
        }
    ) { pad ->
        when {
            !ui.hasInternet -> {
                ErrorState(pad, stringResource(R.string.sin_internet), onRetry = { vm.pingFirebase() }); return@Scaffold
            }
            !ui.connected -> {
                ErrorState(pad, stringResource(R.string.sin_conexion_firebase), onRetry = { vm.pingFirebase() }); return@Scaffold
            }
            ui.loading -> {
                Box(Modifier.fillMaxSize().padding(pad), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }; return@Scaffold
            }
        }

        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Sensor + Setpoints (igual que tu tercer card)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(id = R.string.sensor_titulo),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(id = R.string.nivel_actual, nivelText),
                        style = MaterialTheme.typography.bodyLarge,
                        color = nivelActualBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = stringResource(id = R.string.setpoint_min, String.format(Locale.US, "%.2f", minForDialog)),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(id = R.string.setpoint_max, String.format(Locale.US, "%.2f", maxForDialog)),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        OutlinedButton(
                            onClick = { showSetpointDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = stringResource(id = R.string.btn_editar_setpoints), color = CustomRed)
                        }
                    }
                }
            }
        }
    }

    if (showSetpointDialog) {
        EditarSetPointDialog(
            titulo = stringResource(id = R.string.dialog_editar_setpoints),
            valorActual = ui.nivelActual ?: 0.0,
            minInicial = (ui.setMin ?: 0.0).toFloat(),
            maxInicial = (ui.setMax ?: 100.0).toFloat(),
            unidad = "%",
            range = 0f..100f,
            onGuardar = { nuevoMin, nuevoMax -> vm.guardarSetpoints(nuevoMin, nuevoMax) },
            onDismiss = { showSetpointDialog = false },
            onSinCambios = { showSetpointDialog = false }
        )
    }
}

@Composable
private fun ErrorState(padding: PaddingValues, mensaje: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Error: $mensaje", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry) { Text(stringResource(R.string.reintentar)) }
        }
    }
}
