package com.example.pumpcontrol.viewmodel

import android.content.Context
import android.util.Log
import com.example.pumpcontrol.R
import com.example.pumpcontrol.model.EventoHistorial
import com.example.pumpcontrol.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Guarda y lee historial desde Firestore. La colección final es "historial_<coleccion>".
 */
class HistorialHelperFirestore(
    private val coleccion: String,
    private val context: Context,
    private val scope: CoroutineScope,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRole: MutableStateFlow<UserRole>,
    private val historial: MutableStateFlow<List<EventoHistorial>>
) {
    val nuevoEvento = MutableSharedFlow<EventoHistorial>(extraBufferCapacity = 64)

    fun cargarHistorial(limit: Long = 1000) {
        scope.launch(Dispatchers.IO) {
            try {
                val querySnapshot = firestore.collection("historial_$coleccion")
                    .orderBy("timestamp")
                    .limit(limit)
                    .get()
                    .await()

                val lista = querySnapshot.documents.mapNotNull { it.toObject(EventoHistorial::class.java) }
                historial.value = lista.reversed()
            } catch (e: Exception) {
                Log.e("HistorialHelper", "Error cargando historial: ${e.message}")
            }
        }
    }

    /** Registra acción del usuario autenticado (o correo/rol provistos). */
    fun registrarDesdeApp(
        tipo: String,
        nombre: String,
        estado: Boolean,
        correoManual: String? = null,
        rolManual: String? = null
    ) {
        val evento = crearEvento(
            tipo = tipo,
            nombre = nombre,
            estado = estado,
            correo = correoManual ?: (auth.currentUser?.email ?: context.getString(R.string.default_user_sistema)),
            rol = rolManual ?: userRole.value.name.lowercase()
        )
        registrarEvento(evento)
    }

    /** Para acciones del sistema (si las necesitas) */
    fun registrarDesdeSistema(tipo: String, nombre: String, estado: Boolean) {
        val evento = crearEvento(
            tipo, nombre, estado,
            correo = context.getString(R.string.default_user_sistema),
            rol = context.getString(R.string.system_user)
        )
        registrarEvento(evento)
    }

    private fun crearEvento(
        tipo: String,
        nombre: String,
        estado: Boolean,
        correo: String,
        rol: String
    ): EventoHistorial {
        val now = LocalDateTime.now()
        val fFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val fHora = DateTimeFormatter.ofPattern("HH:mm:ss")
        return EventoHistorial(
            fecha = fFecha.format(now),
            hora = fHora.format(now),
            tipo = tipo,
            nombre = nombre,
            estado = estado,
            correo = correo,
            rol = rol,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun registrarEvento(evento: EventoHistorial) {
        scope.launch(Dispatchers.IO) {
            try {
                firestore.collection("historial_$coleccion").add(evento).await()
                nuevoEvento.tryEmit(evento) // push inmediato para UI
            } catch (e: Exception) {
                Log.e("HistorialHelper", "Error registrando evento: ${e.message}")
            }
        }
    }
}
