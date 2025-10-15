package com.example.pumpcontrol.data.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PumpRepository @Inject constructor(
    private val db: FirebaseDatabase
) {
    private val root: DatabaseReference
        get() = db.getReference(FirebasePaths.ROOT)

    // --- Lecturas (Flows) ---
    fun observeBool(path: String): Flow<Boolean?> = callbackFlow {
        val ref = root.child(path)
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Boolean::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(l)
        awaitClose { ref.removeEventListener(l) }
    }

    fun observeDouble(path: String): Flow<Double?> = callbackFlow {
        val ref = root.child(path)
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val v = when (val any = snapshot.value) {
                    is Double -> any
                    is Long -> any.toDouble()
                    is Int -> any.toDouble()
                    is String -> any.toDoubleOrNull()
                    else -> null
                }
                trySend(v)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(l)
        awaitClose { ref.removeEventListener(l) }
    }
    // Dentro de PumpRepositoryImpl

    // PumpRepository.kt  ✅ CORREGIDO
    fun observeString(path: String): Flow<String?> = callbackFlow {
        // usa el mismo ROOT que en los demás observeX
        val ref = root.child(path)   // <— antes: db.getReference(path)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(String::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }



    // --- Escrituras (suspend) ---
    suspend fun getBomba(): Boolean {
        return root.child(FirebasePaths.Actuadores.BOMBA)
            .get().await().getValue(Boolean::class.java) == true
    }

    suspend fun setBomba(value: Boolean) {
        root.child(FirebasePaths.Actuadores.BOMBA).setValue(value).await()
    }

    suspend fun setLuz(value: Boolean) {
        root.child(FirebasePaths.Actuadores.LUZ).setValue(value).await()
    }

    suspend fun setModeAuto(automatico: Boolean) {
        root.child(FirebasePaths.Control.MODE_AUTO).setValue(automatico).await()
    }

    suspend fun updateSetpoints(min: Double, max: Double) {
        root.child(FirebasePaths.Sensor.NIVEL).updateChildren(
            mapOf(
                "setpoint_ultrasonico_min" to min,
                "setpoint_ultrasonico_max" to max
            )
        ).await()
    }
}
