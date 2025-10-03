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
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset

data class TankWidgetState(
    val nivelPercent: Float,   // valores tal cual Firebase (0..100)
    val setMinPercent: Float,
    val setMaxPercent: Float,
    val ultrasonic: Float,
    val pumpOn: Boolean
)

data class TankWidgetColors(
    val tankBorder: Color = Color(0xFF222222),
    val tankFill: Color = Color(0xFF49A7F5),
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
    tankAspect: Float = 10f,          // alto/ancho; puedes dejar 10f como lo estás usando
    pipeThickness: Dp = 16.dp,
    pipeOutline: Dp = 2.5.dp,
    inputIsTopDown: Boolean = false    // ← TRUE si tu entrada viene 0% arriba / 100% abajo (se invierte SOLO para dibujar)
) {
    // --- Normalización para DIBUJAR (0 = fondo, 100 = arriba) ---
    fun toBottomUp(p: Float) = (if (inputIsTopDown) 100f - p else p).coerceIn(0f, 100f)
    val drawNivel = toBottomUp(state.nivelPercent)
    val drawMin   = toBottomUp(state.setMinPercent)
    val drawMax   = toBottomUp(state.setMaxPercent)

    // --- Textos: se muestran tal cual llegan de Firebase (sin invertir) ---
    val labelNivel = state.nivelPercent.coerceIn(0f, 100f)
    val labelMin   = state.setMinPercent.coerceIn(0f, 100f)
    val labelMax   = state.setMaxPercent.coerceIn(0f, 100f)

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
                            text = "Nivel actual: ${"%.2f".format(labelNivel)} %",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
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
                    var innerTopPx by remember { mutableStateOf(0f) }
                    var innerBottomPx by remember { mutableStateOf(0f) }
                    var innerLeftPx by remember { mutableStateOf(0f) }
                    var innerRightPx by remember { mutableStateOf(0f) }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            //.aspectRatio(tankAspect)
                            .aspectRatio(1.35f)
                    ) {
                        val pad = 16.dp.toPx()
                        val outlinePx = pipeOutline.toPx()
                        val pipeH = pipeThickness.toPx()

                        // --- Tubería (arriba) ---
                        val pipeStartX = size.width * 0.10f
                        val pipeMidX   = size.width * 0.47f
                        val pipeY      = pad + 8.dp.toPx()
                        val elbowH     = pipeH * 1.8f
                        val nozzleX    = pipeMidX + pipeH * 0.5f
                        val nozzleY    = pipeY + elbowH - pipeH / 2f
                        val nozzleR    = pipeH * 0.55f




                        // --- TANQUE: más abajo de la boquilla (lo bajé más) ---
                        val widthFactor = 0.58f
                        val tankWidth   = size.width * widthFactor
                        val tankLeft    = (size.width - tankWidth) / 2f
                        val tankRight   = tankLeft + tankWidth
                        val tankTop     = max(pad + 40.dp.toPx(), nozzleY + nozzleR + 56.dp.toPx()) // ← MÁS BAJO
                        val tankBottom  = size.height - pad
                        val tankHeight  = tankBottom - tankTop

                        val domeH        = max(28.dp.toPx(), tankHeight * 0.12f)
                        val domeRectTop  = tankTop + domeH * 0.22f
                        val baseH        = max(16.dp.toPx(), tankHeight * 0.07f)

                        // fondo claro + domo
                        drawRect(colors.tankBg, Offset(tankLeft, domeRectTop), Size(tankWidth, tankBottom - domeRectTop))
                        drawOval(colors.tankBg, Offset(tankLeft, tankTop), Size(tankWidth, domeH))

                        // contornos (laterales un poco más bajos)
                        val sideDown = 6.dp.toPx()
                        val border = Stroke(width = 3.dp.toPx())
                        drawLine(colors.tankBorder, Offset(tankLeft,  domeRectTop + sideDown), Offset(tankLeft,  tankBottom - 18.dp.toPx()), border.width)
                        drawLine(colors.tankBorder, Offset(tankRight, domeRectTop + sideDown), Offset(tankRight, tankBottom - 18.dp.toPx()), border.width)
                        drawOval(colors.tankBorder, Offset(tankLeft, tankTop),              Size(tankWidth, domeH),  style = border)
                        drawOval(colors.tankBorder, Offset(tankLeft, tankBottom - baseH),  Size(tankWidth, baseH),  style = border)

                        // área útil
                        innerLeftPx   = tankLeft + 3.dp.toPx()
                        innerRightPx  = tankRight - 3.dp.toPx()
                        innerTopPx    = domeRectTop + 2.dp.toPx()
                        innerBottomPx = tankBottom - baseH / 2f - 2.dp.toPx()
                        val innerHeight = innerBottomPx - innerTopPx

                        // --- AGUA (bottom-up) con onditas ---
                        val waterHeight = innerHeight * (drawNivel / 100f)
                        val surfaceY    = innerBottomPx - waterHeight
                        val amp    = min(12.dp.toPx(), innerHeight * 0.06f)
                        val lambda = 32.dp.toPx()

                        val waterPath = Path().apply {
                            moveTo(innerLeftPx, innerBottomPx)
                            lineTo(innerRightPx, innerBottomPx)
                            lineTo(innerRightPx, surfaceY)
                            var x = innerRightPx
                            while (x > innerLeftPx) {
                                val xMid = x - lambda / 2f
                                val xEnd = max(innerLeftPx, x - lambda)
                                val upCtrl   = x    - lambda * 0.25f
                                val downCtrl = xMid - lambda * 0.25f
                                quadraticBezierTo(upCtrl,   surfaceY + amp * kotlin.math.sin(phase + 1.2f), xMid, surfaceY)
                                quadraticBezierTo(downCtrl, surfaceY - amp * kotlin.math.sin(phase + 0.6f), xEnd, surfaceY)
                                x = xEnd
                            }
                            lineTo(innerLeftPx, innerBottomPx)
                            close()
                        }
                        drawPath(waterPath, color = colors.tankFill, style = Fill)

                        // Anillos (decorativos)
                        val rings = 4
                        repeat(rings) { i ->
                            val y = tankTop + (i + 1) * (tankHeight / (rings + 1))
                            drawLine(
                                color = colors.tankBorder.copy(alpha = 0.25f),
                                start = Offset(tankLeft + 8.dp.toPx(), y),
                                end = Offset(tankRight - 8.dp.toPx(), y),
                                strokeWidth = 1.3.dp.toPx()
                            )
                        }

                        // brillo en crestas
                        val crest = Path().apply {
                            moveTo(innerLeftPx, surfaceY)
                            var x = innerLeftPx
                            while (x < innerRightPx) {
                                val xMid = x + lambda / 2f
                                val xEnd = min(innerRightPx, x + lambda)
                                val upCtrl   = x    + lambda * 0.25f
                                val downCtrl = xMid + lambda * 0.25f
                                quadraticBezierTo(upCtrl,   surfaceY - amp * 0.9f * kotlin.math.sin(phase + 0.6f), xMid, surfaceY)
                                quadraticBezierTo(downCtrl, surfaceY + amp * 0.9f * kotlin.math.sin(phase + 1.2f), xEnd, surfaceY)
                                x = xEnd
                            }
                        }
                        drawPath(crest, color = Color.White.copy(alpha = 0.25f), style = Stroke(width = 2f))

                        // --- SETPOINTS (bottom-up) ---
                        val spMinY = innerBottomPx - innerHeight * (drawMin / 100f)
                        val spMaxY = innerBottomPx - innerHeight * (drawMax / 100f)
                        drawLine(colors.spMin, Offset(innerLeftPx, spMinY), Offset(innerRightPx, spMinY), strokeWidth = 3.dp.toPx(), pathEffect = dashed)
                        drawLine(colors.spMax, Offset(innerLeftPx, spMaxY), Offset(innerRightPx, spMaxY), strokeWidth = 3.dp.toPx(), pathEffect = dashed)
                        spMinYdp = with(this) { spMinY.toDp() }
                        spMaxYdp = with(this) { spMaxY.toDp() }

                        // --- TUBERÍA y BOQUILLA (encima del tanque) ---
                        // horizontal
                        drawRoundRect(colors.pipeFill,   Offset(pipeStartX, pipeY - pipeH / 2f), Size(pipeMidX - pipeStartX, pipeH), CornerRadius(pipeH / 2f, pipeH / 2f))
                        drawRoundRect(colors.pipeOutline, Offset(pipeStartX, pipeY - pipeH / 2f), Size(pipeMidX - pipeStartX, pipeH), CornerRadius(pipeH / 2f, pipeH / 2f), style = Stroke(outlinePx))
                        // codo
                        drawRoundRect(colors.pipeFill,   Offset(pipeMidX, pipeY - pipeH / 2f), Size(pipeH, elbowH), CornerRadius(pipeH / 2f, pipeH / 2f))
                        drawRoundRect(colors.pipeOutline, Offset(pipeMidX, pipeY - pipeH / 2f), Size(pipeH, elbowH), CornerRadius(pipeH / 2f, pipeH / 2f), style = Stroke(outlinePx))
                        // boquilla
                        drawCircle(colors.pipeFill, nozzleR, Offset(nozzleX, nozzleY))
                        drawCircle(colors.pipeOutline, nozzleR, Offset(nozzleX, nozzleY), style = Stroke(outlinePx))








                        // --- Gotas (solo con bomba encendida) ---
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

                    // --- Etiquetas (valores SIN invertir, como llegan de Firebase) ---
                    Box(Modifier.matchParentSize()) {
                        Text(
                            text = "Min: ${"%.2f".format(labelMin)} %",
                            color = colors.spMin,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 6.dp, y = spMinYdp - 10.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "Max: ${"%.2f".format(labelMax)} %",
                            color = colors.spMax,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 6.dp, y = spMaxYdp - 10.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
