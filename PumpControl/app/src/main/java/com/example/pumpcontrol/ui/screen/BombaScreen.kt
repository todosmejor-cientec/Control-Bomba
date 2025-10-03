package com.example.pumpcontrol.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pumpcontrol.R
import com.example.pumpcontrol.ui.theme.BombaApagadaFondo
import com.example.pumpcontrol.ui.theme.BombaApagadaTexto
import com.example.pumpcontrol.ui.theme.BombaEncendidaFondo
import com.example.pumpcontrol.ui.theme.BombaEncendidaTexto
import com.example.pumpcontrol.viewmodel.PumpViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BombaScreen(
    onBack: () -> Unit,
    vm: PumpViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    val appBarBlue = Color(0xFF3B8EEC)
    var showConfirm by remember { mutableStateOf(false) }

    // Banner temporal para bloqueo por modo automático
    var autoBlockedBanner by remember { mutableStateOf(false) }
    LaunchedEffect(autoBlockedBanner) {
        if (autoBlockedBanner) {
            delay(2500)
            autoBlockedBanner = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.bomba_titulo),
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
                Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }; return@Scaffold
            }
        }

        val encendida = (ui.bomba == true)
        val cardBg = if (encendida) BombaEncendidaFondo else BombaApagadaFondo
        val estadoTextoColor = if (encendida) BombaEncendidaTexto else BombaApagadaTexto
        val textoEstado = if (encendida) R.string.estado_encendida else R.string.estado_apagada
        val iconoRes =
            when (ui.bomba) {
                true -> R.drawable.ic_blue_light
                false -> R.drawable.ic_green_light
                else -> R.drawable.ic_gray_light
            }

        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card de Bomba
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.bomba_titulo),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            painter = painterResource(id = iconoRes),
                            contentDescription = stringResource(
                                R.string.estado_bomba_icono,
                                stringResource(R.string.bomba_titulo)
                            ),
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(id = R.string.estado_prefijo, stringResource(id = textoEstado)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = estadoTextoColor
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            enabled = ui.bomba != null,
                            onClick = {
                                if (ui.modoAutomatico) {
                                    // Bloquear en automático y mostrar banner temporal
                                    autoBlockedBanner = true
                                } else {
                                    showConfirm = true
                                }
                            },
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Text(text = stringResource(id = if (encendida) R.string.apagar else R.string.encender))
                        }
                    }
                }
            }

            // 🔴 Alerta persistente de sobre-nivel (lectura desde Firebase)
            //val showManualAlert = !ui.modoAutomatico && (ui.bomba == true)
            AnimatedVisibility(visible = ui.alertaSobreNivel) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFB00020))
                        .padding(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.alerta_sobre_nivel_text),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 🔴 Banner temporal: intento de control manual estando en automático
            AnimatedVisibility(visible = autoBlockedBanner) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFB00020))
                        .padding(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.banner_bloqueo_auto),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // Confirmación sólo si NO es automático
    if (showConfirm) {
        val encendida = (ui.bomba == true)
        val pregunta = if (encendida) R.string.pregunta_apagar else R.string.pregunta_encender
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(id = R.string.confirmar_accion)) },
            text = { Text(stringResource(id = pregunta, stringResource(id = R.string.bomba_titulo))) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    vm.toggleBomba()
                }) { Text(stringResource(id = R.string.si)) }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text(stringResource(id = R.string.cancelar)) } }
        )
    }
}

@Composable
private fun ErrorState(padding: PaddingValues, mensaje: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Error: $mensaje", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry) { Text(stringResource(R.string.reintentar)) }
        }
    }
}
