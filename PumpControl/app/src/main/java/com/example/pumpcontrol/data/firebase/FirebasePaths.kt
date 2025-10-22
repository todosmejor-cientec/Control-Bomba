package com.example.pumpcontrol.data.firebase

object FirebasePaths {
    const val ROOT = "pumpControl"   // Solo para el getReference

    object Actuadores {
        const val BOMBA = "actuadores/bomba"
        const val LUZ   = "actuadores/luz"
    }

    object Sensor {
        private const val NIVEL = "sensor/nivel"
        const val ULTRASONICO = "$NIVEL/ultrasonico"
        const val SET_MIN     = "$NIVEL/setpoint_ultrasonico_min"
        const val SET_MAX     = "$NIVEL/setpoint_ultrasonico_max"
        const val FECHA_HORA  = "$NIVEL/fecha_hora"
    }

    object Control {
        const val MODE_AUTO         = "control/mode_auto"
        const val ALERTA_SOBRENIVEL = "control/alerta_sobrenivel"
    }
}
