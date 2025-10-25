package com.example.pumpcontrol.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pumpcontrol.R
import com.example.pumpcontrol.ui.components.TankWidget
import com.example.pumpcontrol.ui.components.TankWidgetState
import com.example.pumpcontrol.util.findActivity
import com.example.pumpcontrol.viewmodel.PumpViewModel
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    // Navegaciones que disparan los items del menú:
    onGoNivel: () -> Unit,
    onGoBomba: () -> Unit,
    onGoPerfil: () -> Unit = {},
    onGoHistorial: () -> Unit = {},
    onGoConfiguracion: () -> Unit = {},
    onLogout: () -> Unit = {},
    // (opcional) si quieres mandar al login cuando no haya sesión:
    onGoLogin: () -> Unit = {},
    vm: PumpViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val userRole by vm.userRole.collectAsState()          // <- flujo de rol del usuario
    val appBarBlue = Color(0xFF3B8EEC)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val activity = remember { context.findActivity() }

    BackHandler(enabled = true) { activity?.moveTaskToBack(true) }

    // ---- Menú condicionado por rol ----


    val txtHome = stringResource(R.string.home)
    val txtNivel = stringResource(R.string.nivel)
    val txtBomba = stringResource(R.string.bomba)
    val txtHist = stringResource(R.string.historial)
    val txtCfg  = stringResource(R.string.configuracion)
    val txtPerfil = stringResource(R.string.perfil)
    val txtLogout = stringResource(R.string.logout)
    data class DrawerItem(val label: String, val icon: ImageVector, val action: () -> Unit)

    val items = remember(
        userRole,           // cambia cuando cambia el rol
        txtHome, txtNivel, txtBomba, txtHist, txtCfg, txtPerfil, txtLogout
    ) {
        buildList {
            add(DrawerItem(txtHome, Icons.Default.Home) { /* ya estás aquí */ })
            add(DrawerItem(txtNivel, Icons.Default.WaterDrop) { onGoNivel() })
            add(DrawerItem(txtBomba, Icons.Default.Power) { onGoBomba() })
            if (userRole.name.equals("ADMIN", true) || userRole.name.equals("SUPERADMIN", true)) {
                add(DrawerItem(txtHist, Icons.Default.History) { onGoHistorial() })
            }
            if (userRole.name.equals("SUPERADMIN", true)) {
                add(DrawerItem(txtCfg, Icons.Default.Settings) { onGoConfiguracion() })
            }
            add(DrawerItem(txtPerfil, Icons.Default.AccountCircle) { onGoPerfil() })
            add(DrawerItem(txtLogout, Icons.AutoMirrored.Filled.ExitToApp) { onLogout() })
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(R.string.menu_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                items.forEach { (label, icon, action) ->
                    NavigationDrawerItem(
                        label = { Text(label) },
                        icon = { Icon(icon, contentDescription = label) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            action()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.titulo_control),
                            fontSize = 35.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (drawerState.isClosed) scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu_icon_description), tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
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

            // --- Alerta de sobre-nivel ---
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

            Spacer(Modifier.height(16.dp))

            // --- Widget del tanque ---
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

