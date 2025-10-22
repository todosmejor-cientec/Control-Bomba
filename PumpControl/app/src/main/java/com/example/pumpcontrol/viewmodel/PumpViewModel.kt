package com.example.pumpcontrol.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pumpcontrol.R
import com.example.pumpcontrol.data.firebase.FirebasePaths
import com.example.pumpcontrol.data.firebase.PumpRepository
import com.example.pumpcontrol.model.EventoHistorial
import com.example.pumpcontrol.model.FiltrosHistorial
import com.example.pumpcontrol.model.UserRole
import com.example.pumpcontrol.model.UsuarioData
import com.example.pumpcontrol.util.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

    val modoAutomatico: Boolean = false,
    val alertaSobreNivel: Boolean = false,
    val fechaHora: String? = null
)

@HiltViewModel
class PumpViewModel @Inject constructor(
    private val app: Application,
    private val repo: PumpRepository,
    private val db: FirebaseDatabase
) : ViewModel() {

    // ---------- Auth / Firestore ----------
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCol get() = firestore.collection("usuarios")

    // ---------- UI ----------
    private val _ui = MutableStateFlow(PumpUiState(loading = true))
    val ui: StateFlow<PumpUiState> = _ui.asStateFlow()

    // ---------- Roles / Usuarios ----------
    private val _userRole = MutableStateFlow(UserRole.DESCONOCIDO)
    val userRole: StateFlow<UserRole> = _userRole

    private val _usuarios = MutableStateFlow<List<UsuarioData>>(emptyList())
    val usuarios: StateFlow<List<UsuarioData>> = _usuarios
    private var userDocListener: ListenerRegistration? = null


    // ---------- Historial ----------
    private val _historial = MutableStateFlow<List<EventoHistorial>>(emptyList())
    val historial: StateFlow<List<EventoHistorial>> = _historial

    private val _historialCompleto = MutableStateFlow<List<EventoHistorial>>(emptyList())
    val historialCompleto: StateFlow<List<EventoHistorial>> = _historialCompleto

    private val _filtros = MutableStateFlow(FiltrosHistorial())
    val filtros: StateFlow<FiltrosHistorial> = _filtros
    private val _busquedaTexto = MutableStateFlow("")
    val busquedaTexto: StateFlow<String> = _busquedaTexto
    private val _cargandoHistorial = MutableStateFlow(false)
    val cargandoHistorial: StateFlow<Boolean> = _cargandoHistorial
    private val _errorHistorial = MutableStateFlow<String?>(null)
    val errorHistorial: StateFlow<String?> = _errorHistorial

    private val historialHelper = HistorialHelperFirestore(
        coleccion = "pumpcontrol",
        context = app.applicationContext,
        scope = viewModelScope,
        firestore = firestore,
        auth = auth,
        userRole = _userRole,
        historial = _historial
    )

    // ✅ ESTE init SE QUEDA
    init {
        observeAll()
        observeFirebaseConnected()
        observeInternet()
        observeDbPermissionErrors()
        cargarRolUsuario {
            startMyRoleListenerSafe() // ← arranca listener tolerante a offline
        }
        viewModelScope.launch {
            historialHelper.nuevoEvento.collect { e ->
                _historialCompleto.value = listOf(e) + _historialCompleto.value
                _historial.value = listOf(e) + _historial.value
            }
        }
        historialHelper.cargarHistorial()
    }


    // -------- Sensores/estado --------
    private fun observeAll() {
        val fBomba  = repo.observeBool(FirebasePaths.Actuadores.BOMBA)
        val fLuz    = repo.observeBool(FirebasePaths.Actuadores.LUZ)
        val fNivel  = repo.observeDouble(FirebasePaths.Sensor.ULTRASONICO)
        val fMin    = repo.observeDouble(FirebasePaths.Sensor.SET_MIN)
        val fMax    = repo.observeDouble(FirebasePaths.Sensor.SET_MAX)
        val fModo   = repo.observeBool(FirebasePaths.Control.MODE_AUTO)
        val fAlerta = repo.observeBool(FirebasePaths.Control.ALERTA_SOBRENIVEL)
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
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _ui.update { it.copy(connected = snapshot.getValue(Boolean::class.java) == true) }
                }
                override fun onCancelled(error: DatabaseError) {
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

    private fun observeDbPermissionErrors() {
        db.getReference(FirebasePaths.ROOT)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) { /* noop */ }
                override fun onCancelled(error: DatabaseError) {
                    _ui.update {
                        it.copy(error = "Permiso denegado en Realtime DB (${error.code}). Revisa reglas y rol.")
                    }
                }
            })
    }



    fun cargarUsuarios() {
        if (_userRole.value != UserRole.SUPERADMIN) return
        val uidActual = auth.currentUser?.uid ?: return

        usersCol.addSnapshotListener { qs, e ->
            if (e != null) {
                // sin permisos u otro error: no crashees
                Log.w("Usuarios", "No se pudo listar usuarios: ${e.message}")
                _usuarios.value = emptyList()
                return@addSnapshotListener
            }
            val lista = qs?.documents.orEmpty()
                .mapNotNull { d ->
                    val uid = d.id
                    if (uid == uidActual) return@mapNotNull null
                    val correo = d.getString("correo") ?: ""
                    val rol = d.getString("rol") ?: "invitado"
                    com.example.pumpcontrol.model.UsuarioData(uid, correo, rol)
                }
            _usuarios.value = lista
        }
    }


    // --------- Rol de usuario / Usuarios ---------
    fun cargarRolUsuario(onLoaded: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return onLoaded()
        usersCol.document(uid)
            .get()
            .addOnSuccessListener { snap ->
                val rol = snap.getString("rol") ?: "INVITADO"
                _userRole.value = UserRole.fromString(rol)
                onLoaded()
            }
            .addOnFailureListener {
                _userRole.value = UserRole.DESCONOCIDO
                onLoaded()
            }
    }


    private fun startMyRoleListenerSafe() {
        val uid = auth.currentUser?.uid ?: return
        userDocListener?.remove()
        userDocListener = usersCol.document(uid)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, e ->
                if (e != null) { Log.w("UserRole","listener error: ${e.message}"); return@addSnapshotListener }
                val rol = snap?.getString("rol") ?: "invitado"
                _userRole.value = UserRole.fromString(rol)
            }
    }
    override fun onCleared() {
        userDocListener?.remove()
        super.onCleared()
    }


    fun cambiarRolUsuario(uid: String, nuevoRol: String, onResult: (Boolean) -> Unit) {
        if (_userRole.value != UserRole.SUPERADMIN) { onResult(false); return }
        usersCol.document(uid).update("rol", nuevoRol)
            .addOnSuccessListener {
                val correo = _usuarios.value.find { it.uid == uid }?.correo ?: uid
                historialHelper.registrarDesdeApp(
                    tipo = "Cambio_rol",
                    nombre = "Usuario: $correo → $nuevoRol",
                    estado = true
                )
                onResult(true)
            }
            .addOnFailureListener { onResult(false) }
    }
    fun eliminarUsuario(uid: String, onResult: (Boolean) -> Unit) {
        if (_userRole.value != UserRole.SUPERADMIN) { onResult(false); return }
        val correo = _usuarios.value.find { it.uid == uid }?.correo ?: uid
        usersCol.document(uid).delete()
            .addOnSuccessListener {
                historialHelper.registrarDesdeApp("Eliminacion", "Usuario: $correo", true)
                onResult(true)
            }
            .addOnFailureListener { onResult(false) }
    }

    // --------- Acciones que se registran en historial ---------
    fun registrarLogin(context: Context) {
        historialHelper.registrarDesdeApp("Sesion", context.getString(R.string.auth_successful), true)
    }

    fun registrarLogout(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val correo = auth.currentUser?.email

            // Si tienes una versión que acepta correo/rol explícitos:
            // historialHelper.registrarDesdeAppConUsuario("Sesion", context.getString(R.string.session_closed), true, correo, rolActualComoString())

            // Si solo tienes registrarDesdeApp(tipo,nombre,estado):
            historialHelper.registrarDesdeApp(
                tipo = "Sesion",
                nombre = context.getString(R.string.session_closed),
                estado = true,
                correoManual = correo
            )

            delay(300)
            onComplete()
        }
    }

    fun toggleBomba() {
        if (_ui.value.modoAutomatico) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val current = repo.getBomba()
                val newValue = !current
                repo.setBomba(newValue)
                repo.setLuz(newValue)

                val texto = "Bomba: ${if (current) "encendida" else "apagada"} → ${if (newValue) "encendida" else "apagada"}"
                historialHelper.registrarDesdeApp("Bomba", texto, newValue)
            }.onFailure { e -> _ui.update { it.copy(error = e.message) } }
        }
    }

    fun guardarSetpoints(min: Float, max: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val minOk = String.format(Locale.US, "%.2f", min).toDouble()
                val maxOk = String.format(Locale.US, "%.2f", max).toDouble()
                require(minOk <= maxOk) { "min no puede ser mayor que max" }
                repo.updateSetpoints(minOk, maxOk)

                val texto = "setpoint_ultrasonico_min=$minOk, setpoint_ultrasonico_max=$maxOk"
                historialHelper.registrarDesdeApp("Setpoint", texto, true)
            }.onFailure { e -> _ui.update { it.copy(error = e.message) } }
        }
    }

    // --------- Utilidades ---------
    fun setModoAutomatico(automatico: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.setModeAuto(automatico) }
                .onFailure { e -> _ui.update { it.copy(error = e.message) } }
        }
    }

    fun pingFirebase() {
        db.getReference(".info/connected")
            .get()
            .addOnSuccessListener { snap -> _ui.update { it.copy(connected = snap.getValue(Boolean::class.java) == true) } }
            .addOnFailureListener { e -> _ui.update { it.copy(connected = false, error = e.message) } }
    }

    fun setBusquedaTexto(valor: String) = run { _busquedaTexto.value = valor }


    val historialFiltrado: StateFlow<List<EventoHistorial>> =
        filtros.combine(_busquedaTexto) { filtrosActivos, textoLibre -> filtrosActivos to textoLibre }
            .flatMapLatest { (filtrosActivos, textoLibre) ->
                historial.map { lista ->
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    lista.filter {
                        val eventoReducido = clasificarEvento(it)
                        (filtrosActivos.evento.isBlank() || eventoReducido.equals(filtrosActivos.evento, true)) &&
                                (filtrosActivos.usuario.isBlank() || it.correo.contains(filtrosActivos.usuario, true)) &&
                                (filtrosActivos.rol.isBlank() || it.rol.contains(filtrosActivos.rol, true)) &&
                                ((filtrosActivos.fechaInicio == null && filtrosActivos.fechaFin == null) ||
                                        runCatching {
                                            val fechaEvento = LocalDate.parse(it.fecha, formatter)
                                            (filtrosActivos.fechaInicio == null || !fechaEvento.isBefore(filtrosActivos.fechaInicio)) &&
                                                    (filtrosActivos.fechaFin == null || !fechaEvento.isAfter(filtrosActivos.fechaFin))
                                        }.getOrDefault(false)) &&
                                (textoLibre.isBlank() || it.nombre.contains(textoLibre, true))
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    fun setFiltros(nuevos: FiltrosHistorial) {
        _filtros.value = nuevos
    }

    fun cargarHistorial(tipo: String = "pumpcontrol") {
        _cargandoHistorial.value = true
        _errorHistorial.value = null

        try {
            // ✅ una sola colección
            historialHelper.cargarHistorial()  // llena _historial internamente vía tu helper

            // ✅ si usas varias colecciones por zona, usa esto en su lugar:
            /*
            when (tipo) {
                "planta"    -> historialHelperPlanta.cargarHistorial()
                "torre_uno" -> historialHelperTorreUno.cargarHistorial()
                "torre_dos" -> historialHelperTorreDos.cargarHistorial()
                else        -> historialHelper.cargarHistorial()
            }
            */
        } catch (e: Exception) {
            _errorHistorial.value = e.message ?: app.getString(R.string.screen_state_error, "Desconocido")
        } finally {
            _cargandoHistorial.value = false
        }
    }

    fun clasificarEvento(evento: EventoHistorial): String {
        return when {
            evento.tipo.equals("Setpoint", true) &&
                    evento.nombre.contains("ultrasonico", true) -> "Setpoint - Nivel"



            evento.tipo.equals("Cambio_rol", true) ->
                app.getString(R.string.event_type_cambio_rol)

            evento.tipo.equals("Bomba", true) ->
                app.getString(R.string.event_type_bomba)


            evento.tipo.equals("Sesion", true) ->
                app.getString(R.string.event_type_sesion)

            else -> "${evento.tipo} - ${evento.nombre.substringBefore(':').trim()}"
        }
    }

}
