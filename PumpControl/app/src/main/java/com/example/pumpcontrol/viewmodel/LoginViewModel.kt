package com.example.pumpcontrol.viewmodel

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.pumpcontrol.R
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore
    private val realtimeDB = FirebaseDatabase.getInstance().reference

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun resetMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(success = false) }
    }

    fun toggleMode() {
        _uiState.update { it.copy(isLoginMode = !it.isLoginMode) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(showPassword = !it.showPassword) }
    }

    fun sendPasswordReset(context: Context) {
        val email = _uiState.value.email
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _uiState.update { it.copy(message = context.getString(R.string.reset_email_sent)) }
            } catch (_: Exception) {
                _uiState.update { it.copy(message = context.getString(R.string.reset_email_error)) }
            }
        }
    }

    fun submit(context: Context) {
        val state = _uiState.value

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(message = context.getString(R.string.email_invalid)) }
            return
        }

        if (state.password.length < 6) {
            _uiState.update { it.copy(message = context.getString(R.string.password_too_short)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                if (state.pendingCredential != null) {
                    val result = auth.signInWithEmailAndPassword(state.email, state.password).await()
                    result.user?.linkWithCredential(state.pendingCredential)?.await()
                    // Asegura doc/rol
                    registrarUsuarioEnFirebase(result.user?.email.orEmpty())
                    _uiState.update { it.copy(success = true, pendingCredential = null) }
                    return@launch
                }

                if (state.isLoginMode) {
                    val result = auth.signInWithEmailAndPassword(state.email, state.password).await()
                    registrarUsuarioEnFirebase(result.user?.email.orEmpty())
                } else {
                    auth.createUserWithEmailAndPassword(state.email, state.password).await()
                    registrarUsuarioEnFirebase(state.email)
                }

                _uiState.update { it.copy(success = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = getErrorMessage(e, context)) }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun signInWithGoogleCredentialManager(context: Context) {
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val firebaseCred = GoogleAuthProvider.getCredential(token, null)

                    try {
                        val authResult = auth.signInWithCredential(firebaseCred).await()
                        registrarUsuarioEnFirebase(authResult.user?.email.orEmpty())
                        _uiState.update { it.copy(success = true) }

                    } catch (e: FirebaseAuthUserCollisionException) {
                        _uiState.update {
                            it.copy(
                                pendingCredential = firebaseCred,
                                message = context.getString(R.string.account_exists)
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(message = context.getString(R.string.credential_type_invalid)) }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(message = context.getString(R.string.credential_error, e.localizedMessage ?: "Error")) }
            }
        }
    }

    fun checkAutoLogin(onSuccess: () -> Unit = {}) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            _uiState.update { it.copy(success = true) }
            onSuccess()
        }
    }

    fun signInWithGoogleWebOAuth(context: Context) {
        val activity = context as? android.app.Activity
        if (activity == null) {
            _uiState.update { it.copy(message = context.getString(R.string.invalid_context)) }
            return
        }

        viewModelScope.launch {
            val provider = OAuthProvider.newBuilder("google.com").build()
            try {
                val result = auth.startActivityForSignInWithProvider(activity, provider).await()
                if (result.user != null) {
                    registrarUsuarioEnFirebase(result.user?.email.orEmpty())
                    _uiState.update { it.copy(success = true) }
                }
            } catch (e: FirebaseAuthUserCollisionException) {
                val credential = GoogleAuthProvider.getCredential(null, null)
                _uiState.update {
                    it.copy(
                        pendingCredential = credential,
                        message = context.getString(R.string.account_exists)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = context.getString(R.string.login_error, e.localizedMessage ?: "Error")) }
            }
        }
    }


    private suspend fun registrarUsuarioEnFirebase(email: String) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val users = firestore.collection("usuarios")
        val metaRef = firestore.collection("meta").document("estado")
        val userRef = users.document(uid)
        val nombre = email.substringBefore("@")

        runCatching {
            firestore.runTransaction { tx ->
                val metaSnap = tx.get(metaRef)
                val hasUsers = metaSnap.exists() && (metaSnap.getBoolean("hasUsers") == true)
                val rolDefault = if (!hasUsers) "superadmin" else "invitado"

                val userSnap = tx.get(userRef)
                val base = mutableMapOf("correo" to email, "nombre" to nombre)
                if (!userSnap.exists() || !userSnap.contains("rol")) base["rol"] = rolDefault

                tx.set(userRef, base, com.google.firebase.firestore.SetOptions.merge())
                if (!hasUsers) tx.set(metaRef, mapOf("hasUsers" to true), com.google.firebase.firestore.SetOptions.merge())
            }.await()
        }.onFailure {
            // Offline: no hagas nada (Firestore lo hará al reconectar si usas colas de escritura),
            // o podrías opcionalmente guardar un flag local para reintentar luego.
        }
    }




    private fun getErrorMessage(e: Exception, context: Context): String = when (e) {
        is FirebaseAuthUserCollisionException -> context.getString(R.string.account_exists)
        is FirebaseAuthInvalidUserException -> context.getString(R.string.user_not_found)
        is FirebaseAuthInvalidCredentialsException -> context.getString(R.string.wrong_password)
        else -> context.getString(R.string.login_error, e.localizedMessage ?: "Error desconocido")
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoginMode: Boolean = true,
    val showPassword: Boolean = false,
    val loading: Boolean = false,
    val message: String? = null,
    val success: Boolean = false,
    val pendingCredential: AuthCredential? = null
)
