
package com.example.pumpcontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pumpcontrol.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun EditarSetPointDialog(
    titulo: String,
    valorActual: Double,
    minInicial: Float,
    maxInicial: Float,
    unidad: String,
    range: ClosedFloatingPointRange<Float>,
    onGuardar: (Float, Float) -> Unit,
    onDismiss: () -> Unit,
    onSinCambios: () -> Unit,
    snackbarHost: SnackbarHostState? = null
) {
    var min by remember { mutableFloatStateOf(minInicial.coerceIn(range)) }
    var max by remember { mutableFloatStateOf(maxInicial.coerceIn(range)) }

    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var mensajeAdvertencia by remember { mutableStateOf<String?>(null) }

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(titulo, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = ctx.getString(
                        R.string.valor_actual_fmt,
                        String.format(Locale.US, "%.2f", valorActual),
                        unidad
                    ),
                    style = MaterialTheme.typography.titleMedium
                )

                SliderSetPointInput(
                    label = stringResource(id = R.string.minimo),
                    value = min,
                    range = range,
                    unidad = unidad
                ) { nuevoMin ->
                    val a = nuevoMin.coerceIn(range.start, range.endInclusive)
                    min = a
                    if (a > max) max = a
                }

                SliderSetPointInput(
                    label = stringResource(id = R.string.maximo),
                    value = max,
                    range = range,
                    unidad = unidad
                ) { nuevoMax ->
                    val a = nuevoMax.coerceIn(range.start, range.endInclusive)
                    max = a
                    if (a < min) min = a
                }

                mensajeAdvertencia?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minRounded = String.format(Locale.US, "%.2f", min).toFloat()
                val maxRounded = String.format(Locale.US, "%.2f", max).toFloat()
                val noChanges = (minRounded == minInicial && maxRounded == maxInicial)
                if (noChanges) {
                    mensajeAdvertencia = ctx.getString(R.string.setpoints_no_modificados)
                    onSinCambios()
                    return@TextButton
                }
                mostrarConfirmacion = true
            }) {
                Text(
                    text = stringResource(id = R.string.guardar_setpoints),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancelar)) }
        }
    )

    mensajeAdvertencia?.let {
        LaunchedEffect(it) {
            delay(2500)
            mensajeAdvertencia = null
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text(stringResource(id = R.string.titulo_confirmar)) },
            text = { Text(stringResource(id = R.string.confirmar_guardado)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                    val minRounded = String.format(Locale.US, "%.2f", min).toFloat()
                    val maxRounded = String.format(Locale.US, "%.2f", max).toFloat()
                    onGuardar(minRounded, maxRounded)
                    snackbarHost?.let { host ->
                        scope.launch { host.showSnackbar(ctx.getString(R.string.setpoints_guardados_ok)) }
                    }
                    onDismiss()
                }) { Text(stringResource(id = R.string.si)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) {
                    Text(stringResource(id = R.string.cancelar))
                }
            }
        )
    }
}

@Composable
fun SliderSetPointInput(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unidad: String,
    step: Float = 0.2f,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ${"%.2f".format(value)} $unidad",
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = ((range.endInclusive - range.start) / step).toInt().coerceAtLeast(0) - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
