package com.example.pumpcontrol.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoginMode: Boolean = true,
    val showPassword: Boolean = false,
    val loading: Boolean = false,
    val message: String? = null,
    val success: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui

    fun updateEmail(v: String) = _ui.update { it.copy(email = v) }
    fun updatePassword(v: String) = _ui.update { it.copy(password = v) }
    fun toggleMode() = _ui.update { it.copy(isLoginMode = !it.isLoginMode) }
    fun togglePasswordVisibility() = _ui.update { it.copy(showPassword = !it.showPassword) }
    fun resetMessage() = _ui.update { it.copy(message = null) }
    fun resetSuccess() = _ui.update { it.copy(success = false) }

    fun submit(onSuccess: () -> Unit) {
        val state = _ui.value
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _ui.update { it.copy(message = "Correo inválido") }; return
        }
        if (state.password.length < 6) {
            _ui.update { it.copy(message = "La contraseña debe tener al menos 6 caracteres") }; return
        }

        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            try {
                if (state.isLoginMode) {
                    auth.signInWithEmailAndPassword(state.email, state.password).await()
                } else {
                    auth.createUserWithEmailAndPassword(state.email, state.password).await()
                }
                _ui.update { it.copy(success = true) }
                onSuccess()
            } catch (e: Exception) {
                val msg = when (e) {
                    is FirebaseAuthUserCollisionException -> "La cuenta ya existe"
                    is FirebaseAuthInvalidUserException -> "Usuario no encontrado"
                    is FirebaseAuthInvalidCredentialsException -> "Credenciales inválidas"
                    else -> e.localizedMessage ?: "Error"
                }
                _ui.update { it.copy(message = msg) }
            } finally {
                _ui.update { it.copy(loading = false) }
            }
        }
    }

    fun sendPasswordReset(context: Context) {
        val email = _ui.value.email
        viewModelScope.launch {
            runCatching { auth.sendPasswordResetEmail(email).await() }
                .onSuccess { _ui.update { it.copy(message = "Te enviamos un correo para restablecer tu contraseña") } }
                .onFailure { _ui.update { it.copy(message = "No se pudo enviar el correo") } }
        }
    }

    // TODO: completa este método si ya configuraste Credential Manager/Web OAuth:
    fun signInWithGoogleIdToken(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            try {
                val cred = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(cred).await()
                _ui.update { it.copy(success = true) }
                onSuccess()
            } catch (e: Exception) {
                _ui.update { it.copy(message = e.localizedMessage ?: "Error con Google") }
            } finally {
                _ui.update { it.copy(loading = false) }
            }
        }
    }

    fun signOut() { auth.signOut() }
}
