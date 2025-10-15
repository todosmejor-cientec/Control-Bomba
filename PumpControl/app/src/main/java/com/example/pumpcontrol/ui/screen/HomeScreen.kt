package com.example.pumpcontrol.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pumpcontrol.R
import com.example.pumpcontrol.ui.components.TankWidget
import com.example.pumpcontrol.ui.components.TankWidgetState
import com.example.pumpcontrol.viewmodel.PumpViewModel
// HomeScreen.kt (añade estos imports)
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


// ...imports y cabecera como ya los tienes...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenMenu: () -> Unit,
    onGoNivel: () -> Unit,
    onGoBomba: () -> Unit,
    vm: PumpViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val appBarBlue = Color(0xFF3B8EEC)

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
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarBlue,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { pad ->

        when {
            !ui.hasInternet -> {
                ErrorState(pad, stringResource(R.string.sin_internet)) { vm.pingFirebase() }
                return@Scaffold
            }
            !ui.connected -> {
                ErrorState(pad, stringResource(R.string.sin_conexion_firebase)) { vm.pingFirebase() }
                return@Scaffold
            }
            ui.loading -> {
                Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }
        }

        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Card de Modo ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    Modifier
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
                        Text(
                            text = stringResource(if (ui.modoAutomatico) R.string.modo_automatico else R.string.modo_manual),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                if (ui.modoAutomatico) R.string.modo_automatico_desc
                                else R.string.modo_manual_desc
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = ui.modoAutomatico,
                        onCheckedChange = { vm.setModoAutomatico(it) }
                    )
                }
            }

            // --- Alerta de sobre-nivel cuando está en manual y bomba encendida ---
            //val showManualAlert = !ui.modoAutomatico && (ui.bomba == true)
            //AnimatedVisibility(visible = showManualAlert) {
            AnimatedVisibility(visible = ui.alertaSobreNivel) {
                Box(
                    Modifier
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

            // Empujar el widget hacia ABAJO
            //Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(16.dp))

            // --- WIDGET DEL TANQUE (más angosto y al fondo) ---
            TankWidget(
                state = TankWidgetState(
                    nivelPercent = (ui.nivelActual ?: 0.0).toFloat().coerceIn(0f, 100f),
                    setMinPercent = (ui.setMin ?: 0.0).toFloat().coerceIn(0f, 100f),
                    setMaxPercent = (ui.setMax ?: 100.0).toFloat().coerceIn(0f, 100f),
                    ultrasonic = (ui.nivelActual ?: 0.0).toFloat(),
                    pumpOn = (ui.bomba == true)
                ),
                lastUpdateText = formatFechaHoraMx(ui.fechaHora) ?: "Sin fecha",
                modifier = Modifier.fillMaxSize(),
                pipeThickness = 16.dp,
                pipeOutline = 3.dp
            )
        }
    }
}

@Composable
private fun ErrorState(
    padding: PaddingValues,
    mensaje: String,
    onRetry: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Error: $mensaje", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry) { Text(stringResource(R.string.reintentar)) }
        }
    }
}


// Entrada esperada: "yyyy-MM-dd HH:mm:ss"
// Salida MX: "12 de octubre de 2025, 22:08:20"
fun formatFechaHoraMx(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        val inFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            isLenient = false
            // Usa la zona de origen de TU cadena. Si la ESP32 guarda hora local, mejor Default.
            timeZone = TimeZone.getDefault()
        }
        val date: Date = inFmt.parse(raw.trim()) ?: return raw

        val outFmt = SimpleDateFormat("d 'de' MMMM 'de' yyyy, HH:mm:ss", Locale("es", "MX")).apply {
            timeZone = TimeZone.getDefault()
        }
        outFmt.format(date)
    } catch (_: ParseException) {
        raw
    }
}
