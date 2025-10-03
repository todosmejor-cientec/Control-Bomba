package com.example.pumpcontrol

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val root: DatabaseReference = FirebaseDatabase.getInstance().getReference("pumpControl")
    private val refBomba = root.child("actuadores/bomba")
    private val refLuz = root.child("actuadores/luz")
    private val refNivel = root.child("sensor/nivel")

    suspend fun setBomba(value: Boolean) {
        refBomba.setValue(value).await()
        refLuz.setValue(value).await()
    }

    suspend fun setSetpoints(min: Double, max: Double) {
        refNivel.updateChildren(
            mapOf(
                "setpoint_ultrasonico_min" to min,
                "setpoint_ultrasonico_max" to max
            )
        ).await()
    }
}
