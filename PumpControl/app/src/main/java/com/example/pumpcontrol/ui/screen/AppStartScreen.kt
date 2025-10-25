// AppStartScreen.kt
package com.example.pumpcontrol.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.pumpcontrol.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

@Composable
fun AppStartScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            // Asegura usuario/meta en background y navega
            withContext(Dispatchers.IO) {
                bootstrapUserIfNeeded(auth, FirebaseFirestore.getInstance())
            }
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.AppStart.route) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.AppStart.route) { inclusive = true }
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Crea / completa / actualiza:
 *   - /usuarios/{uid}   (primer usuario => rol "superadmin")
 *   - /meta/estado      (hasUsers=true)
 *
 * No hace "list" de la colección; solo lee meta y su propio doc.
 */
suspend fun bootstrapUserIfNeeded(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore
) {
    val user = auth.currentUser ?: return
    val uid = user.uid
    val email = user.email ?: return
    val nombre = email.substringBefore("@")

    val users = firestore.collection("usuarios")
    val userRef = users.document(uid)
    val metaRef = firestore.collection("meta").document("estado")

    runCatching {
        firestore.runTransaction { tx ->
            val metaSnap = tx.get(metaRef)
            val hasUsers = metaSnap.exists() && (metaSnap.getBoolean("hasUsers") == true)

            val rolDefault = if (hasUsers) "invitado" else "superadmin"

            val userSnap = tx.get(userRef)
            val base = hashMapOf(
                "correo" to email,
                "nombre" to nombre
            )
            // Si no existe o le falta rol, ponemos el adecuado
            if (!userSnap.exists() || !userSnap.contains("rol")) {
                base["rol"] = rolDefault
            }

            tx.set(userRef, base, SetOptions.merge())
            if (!hasUsers) {
                tx.set(metaRef, mapOf("hasUsers" to true), SetOptions.merge())
            }
        }.await()
    }.onFailure {
        // Si estás offline, no bloquees el arranque
        // (cuando reconectes, puedes volver a llamar a esta función)
    }
}
