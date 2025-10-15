package com.example.pumpcontrol.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.roundToInt

data class TankWidgetState(
    val nivelPercent: Float,   // 0..100 (0 = vacío, 100 = lleno)
    val setMinPercent: Float,  // 0..100
    val setMaxPercent: Float,  // 0..100
    val ultrasonic: Float,
    val pumpOn: Boolean
)

data class TankWidgetColors(
    val tankBorder: Color = Color(0xFF222222),
    val tankFill: Color = Color(0xFF49A7F5), // azul del agua
    val tankBg: Color = Color(0xFFF6F6F6),
    val pipeFill: Color = Color.White,
    val pipeOutline: Color = Color.Black,
    val droplet: Color = Color(0xFF1CA3FF),
    val spMin: Color = Color(0xFF2E7D32),
    val spMax: Color = Color(0xFFB00020)
)

@Composable
fun TankWidget(
    state: TankWidgetState,
    modifier: Modifier = Modifier,
    colors: TankWidgetColors = TankWidgetColors(),
    showLegend: Boolean = true,
    pipeThickness: Dp = 16.dp,
    pipeOutline: Dp = 2.5.dp,
    lastUpdateText: String? = null
) {
    val pct       = state.nivelPercent.coerceIn(0f, 100f)
    val spMin     = min(state.setMinPercent, state.setMaxPercent).coerceIn(0f, 100f)
    val spMax     = max(state.setMinPercent, state.setMaxPercent).coerceIn(0f, 100f)

    val dashed = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 12f)) }
    val infinite = rememberInfiniteTransition(label = "tank-anim")
    val phase by infinite.animateFloat(
        0f, (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    var spMinYdp by remember { mutableStateOf(0.dp) }
    var spMaxYdp by remember { mutableStateOf(0.dp) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(Modifier.padding(12.dp), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            text = "Nivel actual: ${"%.2f".format(pct)} %",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                // NUEVO: pill de fecha/hora justo debajo
                lastUpdateText?.let { ts ->
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                text = "Actualizado: $ts",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))


                if (showLegend) {
                    val on = state.pumpOn
                    val bg = if (on) Color(0xFF2E7D32) else Color(0xFFB00020)
                    Surface(color = bg, shape = RoundedCornerShape(8.dp), shadowElevation = 2.dp) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (on) "Bomba encendida" else "Bomba apagada",
                                color = Color.White, style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.size(10.dp).background(Color.White.copy(alpha = 0.9f), CircleShape))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Box {
                    var innerTopPx by remember { mutableFloatStateOf(0f) }
                    var innerBottomPx by remember { mutableFloatStateOf(0f) }
                    var innerLeftPx by remember { mutableFloatStateOf(0f) }
                    var innerRightPx by remember { mutableFloatStateOf(0f) }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            // ANCHO/ALTO
                            .aspectRatio(1.35f)
                    ) {
                        val pad = 16.dp.toPx()
                        val outlinePx = pipeOutline.toPx()
                        val pipeH = pipeThickness.toPx()

                        // Tubería
                        val pipeStartX = size.width * 0.10f
                        val pipeMidX   = size.width * 0.47f
                        val pipeY      = pad + 8.dp.toPx()
                        val elbowH     = pipeH * 1.8f
                        val nozzleX    = pipeMidX + pipeH * 0.5f
                        val nozzleY    = pipeY + elbowH - pipeH / 2f
                        val nozzleR    = pipeH * 0.55f

                        // Tanque (debajo de boquilla)
                        val widthFactor = 0.58f
                        val tankWidth   = size.width * widthFactor
                        val tankLeft    = (size.width - tankWidth) / 2f
                        val tankRight   = tankLeft + tankWidth
                        val tankTop     = max(pad + 40.dp.toPx(), nozzleY + nozzleR + 56.dp.toPx())
                        val tankBottom  = size.height - pad
                        val tankHeight  = tankBottom - tankTop

                        val domeH        = max(28.dp.toPx(), tankHeight * 0.12f)
                        val domeRectTop  = tankTop + domeH * 0.22f
                        val baseH        = max(16.dp.toPx(), tankHeight * 0.07f)

                        // Fondo interior
                        drawRect(colors.tankBg, Offset(tankLeft, domeRectTop), Size(tankWidth, tankBottom - domeRectTop))
                        drawOval(colors.tankBg, Offset(tankLeft, tankTop), Size(tankWidth, domeH))

                        // Contornos (laterales ahora llegan hasta el semi-círculo)
                        val sideDown = 6.dp.toPx()
                        val border = Stroke(width = 3.dp.toPx())
                        val sideEndY = tankBottom - baseH / 2f  // unión visual con la elipse inferior
                        drawLine(colors.tankBorder, Offset(tankLeft,  domeRectTop + sideDown), Offset(tankLeft,  sideEndY), border.width)
                        drawLine(colors.tankBorder, Offset(tankRight, domeRectTop + sideDown), Offset(tankRight, sideEndY), border.width)

                        // Domo y base
                        drawOval(colors.tankBorder, Offset(tankLeft, tankTop),             Size(tankWidth, domeH), style = border)
                        drawOval(colors.tankBorder, Offset(tankLeft, tankBottom - baseH), Size(tankWidth, baseH), style = border)

                        // Área útil (clip)
                        innerLeftPx   = tankLeft + 3.dp.toPx()
                        innerRightPx  = tankRight - 3.dp.toPx()
                        innerTopPx    = domeRectTop + 2.dp.toPx()
                        innerBottomPx = tankBottom - baseH / 2f - 2.dp.toPx()
                        val innerHeight = innerBottomPx - innerTopPx

                        // AGUA + onditas DENTRO del tanque (clipRect evita desbordes)
                        val waterHeight = innerHeight * (pct / 100f)
                        val surfaceY    = innerBottomPx - waterHeight

                        clipRect(
                            left = innerLeftPx,
                            top = innerTopPx,
                            right = innerRightPx,
                            bottom = innerBottomPx
                        ) {
                            if (waterHeight > 0f) {
                                // cuerpo de agua
                                drawRect(
                                    color = colors.tankFill,
                                    topLeft = Offset(innerLeftPx, surfaceY),
                                    size = Size(innerRightPx - innerLeftPx, innerBottomPx - surfaceY)
                                )

                                // onditas
                                val amp = min(12.dp.toPx(), innerHeight * 0.06f)
                                val lambda = 32.dp.toPx()
                                val wavePath = Path().apply {
                                    moveTo(innerLeftPx, surfaceY)
                                    var x = innerLeftPx
                                    while (x < innerRightPx) {
                                        val xMid = x + lambda / 2f
                                        val xEnd = min(innerRightPx, x + lambda)
                                        val upCtrl   = x    + lambda * 0.25f
                                        val downCtrl = xMid + lambda * 0.25f
                                        quadraticTo(upCtrl,   surfaceY - amp * kotlin.math.sin(phase + 0.6f), xMid, surfaceY)
                                        quadraticTo(downCtrl, surfaceY + amp * kotlin.math.sin(phase + 1.2f), xEnd, surfaceY)
                                        x = xEnd
                                    }
                                }
                                drawPath(wavePath, color = colors.tankFill, style = Stroke(width = max(2f, amp * 0.9f)))
                                drawPath(wavePath, color = Color.White.copy(alpha = 0.22f), style = Stroke(width = 1.5f))
                            }
                        }

                        // SETPOINTS
                        val spMinY = innerBottomPx - innerHeight * (spMin / 100f)
                        val spMaxY = innerBottomPx - innerHeight * (spMax / 100f)
                        drawLine(colors.spMin, Offset(innerLeftPx, spMinY), Offset(innerRightPx, spMinY), strokeWidth = 3.dp.toPx(), pathEffect = dashed)
                        drawLine(colors.spMax, Offset(innerLeftPx, spMaxY), Offset(innerRightPx, spMaxY), strokeWidth = 3.dp.toPx(), pathEffect = dashed)
                        spMinYdp = with(this) { spMinY.toDp() }
                        spMaxYdp = with(this) { spMaxY.toDp() }

                        // Tubería y boquilla
                        drawRoundRect(colors.pipeFill,   Offset(pipeStartX, pipeY - pipeH / 2f), Size(pipeMidX - pipeStartX, pipeH), CornerRadius(pipeH / 2f, pipeH / 2f))
                        drawRoundRect(colors.pipeOutline, Offset(pipeStartX, pipeY - pipeH / 2f), Size(pipeMidX - pipeStartX, pipeH), CornerRadius(pipeH / 2f, pipeH / 2f), style = Stroke(outlinePx))
                        drawRoundRect(colors.pipeFill,   Offset(pipeMidX,   pipeY - pipeH / 2f), Size(pipeH, elbowH), CornerRadius(pipeH / 2f, pipeH / 2f))
                        drawRoundRect(colors.pipeOutline, Offset(pipeMidX,   pipeY - pipeH / 2f), Size(pipeH, elbowH), CornerRadius(pipeH / 2f, pipeH / 2f), style = Stroke(outlinePx))
                        drawCircle(colors.pipeFill, nozzleR, Offset(nozzleX, nozzleY))
                        drawCircle(colors.pipeOutline, nozzleR, Offset(nozzleX, nozzleY), style = Stroke(outlinePx))

                        // Gotas
                        if (state.pumpOn) {
                            val dropR = 5.5.dp.toPx()
                            val span = max(0f, (pipeMidX - pipeStartX - pipeH))
                            repeat(4) { i ->
                                val t = (((phase / (2f * Math.PI).toFloat()) + i / 4f) % 1f)
                                val x = pipeStartX + pipeH / 2f + t * span
                                drawCircle(colors.droplet, dropR, Offset(x, pipeY))
                            }
                            val norm = ((phase / (2f * Math.PI).toFloat()) % 1f)
                            val fallStart = nozzleY + nozzleR + 2f
                            val fallEnd   = innerTopPx + 6f
                            val fy = fallStart + (fallEnd - fallStart) * norm
                            if (fy <= fallEnd) drawCircle(colors.droplet, dropR * 1.1f, Offset(nozzleX, fy))
                        }
                    }

                    // Etiquetas
                    Box(Modifier.matchParentSize()) {
                        Text(
                            text = "Min: ${"%.2f".format(spMin)} %",
                            color = colors.spMin,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset { IntOffset(6.dp.roundToPx(), (spMinYdp - 10.dp).roundToPx()) }
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "Max: ${"%.2f".format(spMax)} %",
                            color = colors.spMax,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset { IntOffset(6.dp.roundToPx(), (spMaxYdp - 10.dp).roundToPx()) }
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
