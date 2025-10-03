package com.example.pumpcontrol

import android.content.Context

data class BombaState(
    val estado: String = "AUTO", // "ON", "OFF", "AUTO"
    val nivelActual: Int = 0,
    val setpointMinimo: Int = 30,
    val setpointMaximo: Int = 80
)



// ✅ Estado de actuadores (bombas)
data class ActuadoresData(
    val bomba: Boolean? = null,
    val luz: Boolean = false
)

// ✅ Estado de sensores de nivel
data class SensoresNivelData(
    val ultrasonico: Double = 0.0,
    val setPointMin: Double = 0.0,
    val setPointMax: Double = 0.0
)
// ✅ Resultado al guardar Set Points
enum class ResultadoGuardadoSetPoints {
    Exito, Fallo, DatosIncorrectos, DatosInvalidos
}
