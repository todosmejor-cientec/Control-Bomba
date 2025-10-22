package com.example.pumpcontrol.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pumpcontrol.R
import com.example.pumpcontrol.model.EventoHistorial
import com.example.pumpcontrol.model.FiltrosHistorial
import com.example.pumpcontrol.ui.components.PumpTopAppBar
import com.example.pumpcontrol.ui.components.ScreenStateWrapper
import com.example.pumpcontrol.util.findActivity
import com.example.pumpcontrol.viewmodel.PumpViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    tipo: String = "pumpcontrol",
    viewModel: PumpViewModel = hiltViewModel(),
    onBackPress: () -> Unit
) {
    val context = LocalContext.current

    val historial by viewModel.historialCompleto.collectAsState()
    val historialFiltrado by viewModel.historialFiltrado.collectAsState()
    val textoBusqueda by viewModel.busquedaTexto.collectAsState()
    val cargando by viewModel.cargandoHistorial.collectAsState()
    val error by viewModel.errorHistorial.collectAsState()

    var showFilters by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportUri by remember { mutableStateOf<Uri?>(null) }

    var filtroEvento by remember { mutableStateOf("") }
    var filtroUsuario by remember { mutableStateOf("") }
    var filtroRol by remember { mutableStateOf("") }
    var fechaInicio by remember { mutableStateOf<LocalDate?>(null) }
    var fechaFin by remember { mutableStateOf<LocalDate?>(null) }

    val filtrosActivos by remember(filtroEvento, filtroUsuario, filtroRol, fechaInicio, fechaFin) {
        derivedStateOf {
            listOf(filtroEvento, filtroUsuario, filtroRol).count { it.isNotBlank() } +
                    if (fechaInicio != null || fechaFin != null) 1 else 0
        }
    }

    val eventosUnicos by remember {
        derivedStateOf { historial.map { viewModel.clasificarEvento(it) }.distinct() }
    }
    val usuariosUnicos by remember { derivedStateOf { historial.map { it.correo }.distinct() } }
    val rolesUnicos by remember { derivedStateOf { historial.map { it.rol }.distinct() } }

    var paginaActual by remember { mutableIntStateOf(0) }
    val tamanoPagina = 20
    val totalPaginas = (historialFiltrado.size + tamanoPagina - 1) / tamanoPagina
    val eventosPagina = historialFiltrado.drop(paginaActual * tamanoPagina).take(tamanoPagina)

    LaunchedEffect(tipo) { viewModel.cargarHistorial(tipo) }

    val tituloHistorial = stringResource(R.string.historial_general)

    Scaffold(
        topBar = {
            PumpTopAppBar(
                title = tituloHistorial,
                onBackClick = onBackPress,
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filtros))
                    }
                    if (filtrosActivos > 0) {
                        Text(
                            text = filtrosActivos.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.action_export))
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {

                OutlinedTextField(
                    value = textoBusqueda,
                    onValueChange = { viewModel.setBusquedaTexto(it) },
                    label = { Text(stringResource(R.string.label_search_events)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    ScreenStateWrapper(
                        datos = historialFiltrado,
                        cargando = cargando,
                        error = error,
                        onRetry = { viewModel.cargarHistorial(tipo) }
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Cabecera(stringResource(R.string.column_date))
                                Cabecera(stringResource(R.string.column_time))
                                Cabecera(stringResource(R.string.column_event))
                                Cabecera(stringResource(R.string.column_user))
                                Cabecera(stringResource(R.string.column_role))
                            }

                            HorizontalDivider(thickness = 1.dp)

                            LazyColumn(
                                modifier = Modifier.fillMaxHeight().weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                itemsIndexed(eventosPagina) { _, evento ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            CeldaLibre(evento.fecha)
                                            CeldaLibre(evento.hora)
                                            CeldaLibre("${evento.icono} ${viewModel.clasificarEvento(evento)} - ${evento.nombre}", evento.color)
                                            CeldaLibre(evento.correo)
                                            CeldaLibre(evento.rol)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(onClick = { if (paginaActual > 0) paginaActual-- }, enabled = paginaActual > 0) {
                                    Text(stringResource(R.string.prev))
                                }
                                Text(
                                    text = stringResource(R.string.pagina_de, paginaActual + 1, maxOf(totalPaginas, 1)),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Button(
                                    onClick = { if (paginaActual < totalPaginas - 1) paginaActual++ },
                                    enabled = paginaActual < totalPaginas - 1
                                ) { Text(stringResource(R.string.next)) }
                            }

                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.total_registros, historialFiltrado.size),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // filtros
    if (showFilters) {
        HistorialFiltrosBottomSheet(
            onDismiss = { showFilters = false },
            eventos = eventosUnicos,
            usuarios = usuariosUnicos,
            roles = rolesUnicos,
            filtroEvento = filtroEvento,
            onEventoChange = { filtroEvento = it },
            filtroUsuario = filtroUsuario,
            onUsuarioChange = { filtroUsuario = it },
            filtroRol = filtroRol,
            onRolChange = { filtroRol = it },
            fechaInicio = fechaInicio,
            fechaFin = fechaFin,
            onFechaInicioChange = { fechaInicio = it },
            onFechaFinChange = { fechaFin = it },
            onAplicar = {
                viewModel.setFiltros(
                    FiltrosHistorial(
                        evento = filtroEvento,
                        usuario = filtroUsuario,
                        rol = filtroRol,
                        fechaInicio = fechaInicio,
                        fechaFin = fechaFin
                    )
                )
                showFilters = false
            },
            onBorrar = {
                filtroEvento = ""; filtroUsuario = ""; filtroRol = ""
                fechaInicio = null; fechaFin = null
                viewModel.setFiltros(FiltrosHistorial())
            }
        )
    }

    // export
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    exportUri = exportarHistorialCSV(context, historialFiltrado, tipo)
                    showExportDialog = false
                    exportUri?.let {
                        Toast.makeText(context, context.getString(R.string.archivo_guardado, it.path), Toast.LENGTH_LONG).show()
                    }
                }) { Text(stringResource(R.string.exportar)) }
            },
            dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text(stringResource(R.string.cancelar)) } },
            title = { Text(stringResource(R.string.confirm_export)) },
            text = { Text(stringResource(R.string.prompt_export_csv)) }
        )
    }

    exportUri?.let { uri ->
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val activity = context.findActivity()
        LaunchedEffect(uri) {
            activity?.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_history)))
            exportUri = null
        }
    }
}

@Composable
fun Cabecera(texto: String) {
    Text(
        text = texto,
        modifier = Modifier.width(180.dp).padding(6.dp),
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun CeldaLibre(texto: String, color: Color = Color.Unspecified) {
    Text(
        text = texto,
        modifier = Modifier.width(180.dp).padding(horizontal = 6.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            color = color.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onBackground
        )
    )
}

fun exportarHistorialCSV(context: Context, eventos: List<EventoHistorial>, tipo: String): Uri? = try {
    val csv = buildString {
        append(context.getString(R.string.csv_header))
        eventos.forEach {
            append("${it.fecha},${it.hora},\"${it.tipo} - ${it.nombre}\",${it.correo},${it.rol}\n")
        }
    }
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val file = File(context.getExternalFilesDir(null), "PumpControl_historial_${tipo}_$ts.csv")
    file.writeText(csv)
    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
} catch (_: Exception) { null }

/* ---- Filtros y DatePicker: iguales a tu versión, solo cambia el paquete/strings ---- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltroCampo(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = value, onValueChange = onChange,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { op ->
                DropdownMenuItem(text = { Text(op) }, onClick = { onChange(op); onExpandedChange(false) })
            }
        }
    }
}

@Composable
fun SelectorFechaConIcono(titulo: String, fecha: LocalDate?, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(8.dp)) {
        Text(titulo, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Color.Gray)
            Spacer(Modifier.width(6.dp))
            Text(
                text = fecha?.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) ?: stringResource(R.string.seleccionar),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoFecha(
    titulo: String,
    fechaActual: LocalDate?,
    onFechaSeleccionada: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val hoy = LocalDate.now()
    val minFecha = hoy.minusMonths(4)
    val millisInicial = fechaActual?.toEpochDay()?.times(86_400_000)

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = millisInicial,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val f = LocalDate.ofEpochDay(utcTimeMillis / 86_400_000)
                return !f.isBefore(minFecha) && !f.isAfter(hoy)
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onFechaSeleccionada(LocalDate.ofEpochDay(millis / 86_400_000))
                }
                onDismiss()
            }) { Text(stringResource(R.string.aceptar)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancelar)) } }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialFiltrosBottomSheet(
    onDismiss: () -> Unit,
    eventos: List<String>,
    usuarios: List<String>,
    roles: List<String>,
    filtroEvento: String,
    onEventoChange: (String) -> Unit,
    filtroUsuario: String,
    onUsuarioChange: (String) -> Unit,
    filtroRol: String,
    onRolChange: (String) -> Unit,
    fechaInicio: LocalDate?,
    fechaFin: LocalDate?,
    onFechaInicioChange: (LocalDate) -> Unit,
    onFechaFinChange: (LocalDate) -> Unit,
    onAplicar: () -> Unit,
    onBorrar: () -> Unit
) {
    var expandedEvento by remember { mutableStateOf(false) }
    var expandedUsuario by remember { mutableStateOf(false) }
    var expandedRol by remember { mutableStateOf(false) }
    var mostrarDialogoInicio by remember { mutableStateOf(false) }
    var mostrarDialogoFin by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.filtrar_por), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))

            FiltroCampo(stringResource(R.string.column_event), filtroEvento, onEventoChange, eventos, expandedEvento) { expandedEvento = it }
            Spacer(Modifier.height(12.dp))
            FiltroCampo(stringResource(R.string.column_user), filtroUsuario, onUsuarioChange, usuarios, expandedUsuario) { expandedUsuario = it }
            Spacer(Modifier.height(12.dp))
            FiltroCampo(stringResource(R.string.column_role), filtroRol, onRolChange, roles, expandedRol) { expandedRol = it }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.seleccionar_rango_fechas), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))

            SelectorFechaConIcono(stringResource(R.string.fecha_inicial), fechaInicio) { mostrarDialogoInicio = true }
            SelectorFechaConIcono(stringResource(R.string.fecha_final), fechaFin) { mostrarDialogoFin = true }

            if (mostrarDialogoInicio) {
                DialogoFecha(stringResource(R.string.seleccionar_fecha_inicial), fechaActual = fechaInicio,
                    onFechaSeleccionada = onFechaInicioChange, onDismiss = { mostrarDialogoInicio = false })
            }
            if (mostrarDialogoFin) {
                DialogoFecha(stringResource(R.string.seleccionar_fecha_final), fechaActual = fechaFin,
                    onFechaSeleccionada = onFechaFinChange, onDismiss = { mostrarDialogoFin = false })
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.borrar_filtros),
                    modifier = Modifier.clickable { onBorrar() }.padding(12.dp),
                    color = MaterialTheme.colorScheme.primary)
                Text(text = stringResource(R.string.aplicar_filtros),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                        .clickable { onAplicar() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
