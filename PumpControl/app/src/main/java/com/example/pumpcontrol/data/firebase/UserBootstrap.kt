// data/firebase/UserBootstrap.kt
package com.example.pumpcontrol.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await

suspend fun ensureUserNode(auth: FirebaseAuth, db: DatabaseReference) {
    val uid = auth.currentUser?.uid ?: return
    val userRef = db.child("usuarios").child(uid)
    val snap = userRef.get().await()
    if (!snap.exists()) {
        userRef.setValue(
            mapOf(
                "rol" to "invitado",
                "correo" to (auth.currentUser?.email ?: "")
            )
        ).await()
    }
}
