package com.example.pumpcontrol.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.pumpcontrol.R
import com.example.pumpcontrol.ui.components.EditarSetPointDialog
import com.example.pumpcontrol.ui.theme.BombaApagadaFondo
import com.example.pumpcontrol.ui.theme.BombaApagadaTexto
import com.example.pumpcontrol.ui.theme.BombaEncendidaFondo
import com.example.pumpcontrol.ui.theme.BombaEncendidaTexto
import com.example.pumpcontrol.ui.theme.CustomRed
import com.example.pumpcontrol.viewmodel.PumpViewModel
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PumpDashboardScreen(
    isEditable: Boolean,
    onBackPress: () -> Unit,
    vm: PumpViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val ui by vm.ui.collectAsStateWithLifecycle()

    var showSetpointDialog by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val minForDialog = (ui.setMin ?: 0.0).toFloat()
    val maxForDialog = (ui.setMax ?: 100.0).toFloat()
    val nivelText = ui.nivelActual?.let { String.format(Locale.US, "%.2f", it) } ?: "--"

    val snackHost = remember { SnackbarHostState() }

    val appBarBlue = Color(0xFF3B8EEC)
    val nivelActualBlue = Color(0xFF0478FC)

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
                        text = stringResource(id = R.string.titulo_control),
                        fontSize = 35.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarBlue,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackHost) }
    ) { pad ->

        when {
            !ui.hasInternet -> {
                ErrorState(
                    padding = pad,
                    mensaje = stringResource(R.string.sin_internet),
                    onRetry = { vm.pingFirebase() }
                )
                return@Scaffold
            }
            !ui.connected -> {
                ErrorState(
                    padding = pad,
                    mensaje = stringResource(R.string.sin_conexion_firebase),
                    onRetry = { vm.pingFirebase() }
                )
                return@Scaffold
            }
            ui.loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(pad),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }
        }

        ui.error?.let { err ->
            ErrorState(pad, err, onRetry = { vm.pingFirebase() })
            return@Scaffold
        }

        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Modo
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.modo_operacion_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        val modoTxt = stringResource(
                            if (ui.modoAutomatico) R.string.modo_automatico else R.string.modo_manual
                        )
                        val descTxt = stringResource(
                            if (ui.modoAutomatico) R.string.modo_automatico_desc else R.string.modo_manual_desc
                        )
                        Text(text = modoTxt, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = descTxt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = ui.modoAutomatico,
                        onCheckedChange = { checked ->
                            if (isEditable) vm.setModoAutomatico(checked)
                            else scope.launch {
                                snackHost.showSnackbar(
                                    message = context.getString(R.string.sin_permisos)
                                )
                            }
                        }
                    )
                }
            }

            // Card Bomba
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
                        text = stringResource(
                            id = R.string.estado_prefijo,
                            stringResource(id = textoEstado)
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = estadoTextoColor
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            enabled = isEditable && ui.bomba != null,
                            onClick = {
                                if (ui.modoAutomatico) {
                                    autoBlockedBanner = true
                                } else {
                                    showConfirm = true
                                }
                            },
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    id = if (encendida) R.string.apagar else R.string.encender
                                )
                            )
                        }
                    }
                }
            }

            // Card Sensor + Setpoints
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
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

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.setpoint_min,
                                String.format(Locale.US, "%.2f", minForDialog)
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(
                                id = R.string.setpoint_max,
                                String.format(Locale.US, "%.2f", maxForDialog)
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        OutlinedButton(
                            enabled = isEditable,
                            onClick = { showSetpointDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.btn_editar_setpoints),
                                color = CustomRed
                            )
                        }
                    }
                }
            }

            // Aviso permanente rojo desde Firebase: alerta_sobrenivel == true
            AnimatedVisibility(visible = ui.alertaSobreNivel) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFB00020)) // Material error
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.alerta_sobre_nivel_text),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Banner rojo temporal si se intentó accionar en automático
            AnimatedVisibility(visible = autoBlockedBanner) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFB00020))
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.banner_bloqueo_auto),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            AnimatedVisibility(visible = !isEditable) {
                Text(
                    text = stringResource(id = R.string.sin_permisos),
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    // Confirmación (solo cuando no es automático)
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
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(id = R.string.cancelar))
                }
            }
        )
    }

    // Snackbar cuando cambie el estado de la bomba
    LaunchedEffect(ui.bomba) {
        ui.bomba?.let { encendida ->
            val msgRes = if (encendida) R.string.snack_bomba_encendida else R.string.snack_bomba_apagada
            snackHost.showSnackbar(message = context.getString(msgRes))
        }
    }

    // Diálogo de Setpoints
    if (showSetpointDialog) {
        EditarSetPointDialog(
            titulo = stringResource(id = R.string.dialog_editar_setpoints),
            valorActual = ui.nivelActual ?: 0.0,
            minInicial = minForDialog,
            maxInicial = maxForDialog,
            unidad = "%",
            range = 0f..100f,
            onGuardar = { nuevoMin, nuevoMax -> vm.guardarSetpoints(nuevoMin, nuevoMax) },
            onDismiss = { showSetpointDialog = false },
            onSinCambios = { showSetpointDialog = false },
            snackbarHost = snackHost
        )
    }
}

@Composable
private fun ErrorState(
    padding: PaddingValues,
    mensaje: String,
    onRetry: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Error: $mensaje", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry) { Text(stringResource(R.string.reintentar)) }
        }
    }
}
