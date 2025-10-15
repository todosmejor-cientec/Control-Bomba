package com.example.pumpcontrol.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pumpcontrol.data.firebase.FirebasePaths
import com.example.pumpcontrol.data.firebase.PumpRepository
import com.example.pumpcontrol.util.NetworkMonitor
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class PumpUiState(
    val loading: Boolean = true,
    val hasInternet: Boolean = true,
    val connected: Boolean = true,
    val error: String? = null,

    val bomba: Boolean? = null,
    val luz: Boolean? = null,

    val nivelActual: Double? = null,
    val setMin: Double? = null,
    val setMax: Double? = null,

    val modoAutomatico: Boolean = false,     // TRUE = automático, FALSE = manual
    val alertaSobreNivel: Boolean = false ,   // TRUE = mostrar alerta roja
    // NUEVO:
    val fechaHora: String? = null
)

@HiltViewModel
class PumpViewModel @Inject constructor(
    private val app: Application,
    private val repo: PumpRepository,
    private val db: FirebaseDatabase
) : ViewModel() {

    private val _ui = MutableStateFlow(PumpUiState(loading = true))
    val ui: StateFlow<PumpUiState> = _ui.asStateFlow()

    init {
        observeAll()
        observeFirebaseConnected()
        observeInternet()
    }

    private fun observeAll() {
        val fBomba  = repo.observeBool(FirebasePaths.Actuadores.BOMBA)
        val fLuz    = repo.observeBool(FirebasePaths.Actuadores.LUZ)
        val fNivel  = repo.observeDouble(FirebasePaths.Sensor.ULTRASONICO)
        val fMin    = repo.observeDouble(FirebasePaths.Sensor.SET_MIN)
        val fMax    = repo.observeDouble(FirebasePaths.Sensor.SET_MAX)
        val fModo   = repo.observeBool(FirebasePaths.Control.MODE_AUTO)
        val fAlerta = repo.observeBool(FirebasePaths.Control.ALERTA_SOBRENIVEL)
        // NUEVO:
        val fFecha  = repo.observeString(FirebasePaths.Sensor.FECHA_HORA)


        combine(fBomba, fLuz, fNivel, fMin, fMax, fModo, fAlerta, fFecha) { values: Array<Any?> ->
            val bomba   = values[0] as Boolean?
            val luz     = values[1] as Boolean?
            val nivel   = values[2] as Double?
            val min     = values[3] as Double?
            val max     = values[4] as Double?
            val modo    = (values[5] as Boolean?) == true
            val alerta  = (values[6] as Boolean?) == true
            val fecha   = values[7] as String?

            PumpUiState(
                loading = false,
                hasInternet = _ui.value.hasInternet,
                connected  = _ui.value.connected,
                error = null,
                bomba = bomba,
                luz = luz,
                nivelActual = nivel,
                setMin = min,
                setMax = max,
                modoAutomatico = modo,
                alertaSobreNivel = alerta,
                fechaHora = fecha
            )
        }
            .onStart { emit(_ui.value.copy(loading = true)) }
            .catch { e -> _ui.update { it.copy(loading = false, error = e.message) } }
            .onEach { _ui.value = it }
            .launchIn(viewModelScope)
    }

    private fun observeFirebaseConnected() {
        db.getReference(".info/connected")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    _ui.update { it.copy(connected = snapshot.getValue(Boolean::class.java) == true) }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    _ui.update { it.copy(connected = false, error = error.message) }
                }
            })
    }

    private fun observeInternet() {
        NetworkMonitor.internetFlow(app)
            .onEach { isOnline -> _ui.update { it.copy(hasInternet = isOnline) } }
            .catch { _ui.update { it.copy(hasInternet = false) } }
            .launchIn(viewModelScope)
    }

    // --- Acciones ---
    fun toggleBomba() {
        if (_ui.value.modoAutomatico) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val current = repo.getBomba()
                val newValue = !current
                repo.setBomba(newValue)
                repo.setLuz(newValue)
            }.onFailure { e ->
                _ui.update { it.copy(error = e.message) }
            }
        }
    }

    fun guardarSetpoints(min: Float, max: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val minOk = String.format(Locale.US, "%.2f", min).toDouble()
                val maxOk = String.format(Locale.US, "%.2f", max).toDouble()
                require(minOk <= maxOk) { "min no puede ser mayor que max" }
                repo.updateSetpoints(minOk, maxOk)
            }.onFailure { e ->
                _ui.update { it.copy(error = e.message) }
            }
        }
    }

    fun setModoAutomatico(automatico: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.setModeAuto(automatico) }
                .onFailure { e -> _ui.update { it.copy(error = e.message) } }
        }
    }

    fun pingFirebase() {
        db.getReference(".info/connected")
            .get()
            .addOnSuccessListener { snap ->
                _ui.update { it.copy(connected = snap.getValue(Boolean::class.java) == true) }
            }
            .addOnFailureListener { e ->
                _ui.update { it.copy(connected = false, error = e.message) }
            }
    }
}
