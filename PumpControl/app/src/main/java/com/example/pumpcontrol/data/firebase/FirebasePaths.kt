package com.example.pumpcontrol.data.firebase

object FirebasePaths {
    const val ROOT = "pumpControl"

    object Actuadores {
        const val BOMBA = "actuadores/bomba"
        const val LUZ = "actuadores/luz"
    }

    object Sensor {
        const val NIVEL = "sensor/nivel"
        const val ULTRASONICO = "$NIVEL/ultrasonico"
        const val SET_MIN = "$NIVEL/setpoint_ultrasonico_min"
        const val SET_MAX = "$NIVEL/setpoint_ultrasonico_max"
        // NUEVO:
        const val FECHA_HORA  = "$NIVEL/fecha_hora"
    }

    object Control {
        // TRUE = automático, FALSE = manual
        const val MODE_AUTO = "control/mode_auto"
        // TRUE = mostrar alerta roja
        const val ALERTA_SOBRENIVEL = "control/alerta_sobrenivel"
    }
}

