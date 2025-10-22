package com.example.pumpcontrol.model

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.pumpcontrol.R
import java.time.LocalDate

// Roles con nombres de despliegue
enum class UserRole {
    INVITADO, ADMIN, SUPERADMIN, DESCONOCIDO;

    fun getDisplayName(context: Context): String = when (this) {
        INVITADO -> context.getString(R.string.role_invitado)
        ADMIN -> context.getString(R.string.role_admin)
        SUPERADMIN -> context.getString(R.string.role_superadmin)
        DESCONOCIDO -> context.getString(R.string.role_desconocido)
    }

    companion object {
        fun fromString(value: String?): UserRole = when (value?.trim()?.uppercase()) {
            "INVITADO" -> INVITADO
            "ADMIN" -> ADMIN
            "SUPERADMIN" -> SUPERADMIN
            else -> DESCONOCIDO
        }
    }
}

data class UsuarioData(
    val uid: String = "",
    val correo: String = "",
    val rol: String = "INVITADO"
) {
    fun rolEnum(): UserRole = UserRole.fromString(rol)
}

data class EventoHistorial(
    val fecha: String = "",
    val hora: String = "",
    val tipo: String = "",     // "Sesion", "Bomba", "Setpoint", "Cambio_rol", "Eliminacion"
    val nombre: String = "",   // descripción legible
    val estado: Boolean = false,
    val correo: String = "",
    val rol: String = "",
    val timestamp: Long = 0L
) {
    val color: Color
        get() = when (tipo) {
            "Cambio_rol" -> Color(0xFF1976D2)
            "Eliminacion" -> Color(0xFFD32F2F)
            "Setpoint" -> Color(0xFF388E3C)
            "Bomba" -> Color(0xFF1D087C)
            "Sesion" -> Color(0xFF546E7A)
            else -> Color.Gray
        }

    val icono: String
        get() = when (tipo) {
            "Cambio_rol" -> "🔄"
            "Eliminacion" -> "🗑️"
            "Setpoint" -> "⚙️"
            "Bomba" -> "💧"
            "Sesion" -> "👤"
            else -> "📌"
        }
}
data class FiltrosHistorial(
    val evento: String = "",
    val usuario: String = "",
    val rol: String = "",
    val fechaInicio: LocalDate? = null,
    val fechaFin: LocalDate? = null
)
