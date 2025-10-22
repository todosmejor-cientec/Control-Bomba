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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AppStartScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            // No bloquees el arranque: haz la verificación en background y navega ya
            launch(Dispatchers.IO) {
                ensureUserDocInFirestoreSafe(auth, FirebaseFirestore.getInstance())
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

/* ===================== Helpers top-level (fuera del @Composable) ===================== */

suspend fun DocumentReference.safeGet() =
    runCatching { get(Source.SERVER).await() }
        .getOrElse { runCatching { get(Source.CACHE).await() }.getOrNull() }

suspend fun ensureUserDocInFirestoreSafe(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore
) {
    try {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: return
        val nombre = email.substringBefore("@")

        val users = firestore.collection("usuarios")
        val userRef = users.document(uid)

        val doc = userRef.safeGet() ?: return // offline sin cache → salir silenciosamente

        if (!doc.exists()) {
            // ¿existe algún usuario?
            val anyUser = runCatching {
                users.limit(1).get(Source.SERVER).await().size() > 0
            }.getOrElse {
                runCatching { users.limit(1).get(Source.CACHE).await().size() > 0 }
                    .getOrDefault(false)
            }

            val rolDefault = if (anyUser) "invitado" else "superadmin"

            runCatching {
                userRef.set(
                    mapOf("correo" to email, "nombre" to nombre, "rol" to rolDefault),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            }
            // si falla (offline), lo dejará pendiente y se sincroniza al reconectar
        } else {
            val updates = mutableMapOf<String, Any>()
            if (!doc.contains("rol")) updates["rol"] = "invitado"
            if (!doc.contains("correo")) updates["correo"] = email
            if (!doc.contains("nombre")) updates["nombre"] = nombre
            if (updates.isNotEmpty()) {
                runCatching {
                    userRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
                }
            }
        }
    } catch (_: Exception) {
        // No bloquees el arranque por errores de red/offline
    }
}
