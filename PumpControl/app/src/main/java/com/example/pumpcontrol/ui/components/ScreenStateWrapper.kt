package com.example.pumpcontrol.ui.components


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pumpcontrol.R

@Composable
fun <T> ScreenStateWrapper(
    datos: List<T>,
    cargando: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    contenido: @Composable () -> Unit
) {
    when {
        cargando -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        error != null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.screen_state_error, error), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                onRetry?.let { Button(onClick = it) { Text(stringResource(R.string.reintentar)) } }
            }
        }
        datos.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onBackground)
        }
        else -> contenido()
    }
}
