package com.example.pumpcontrol

import android.app.Application
import android.os.StrictMode
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

import com.google.firebase.crashlytics.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MyApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // No llames a FirebaseApp.initializeApp(this): el Provider lo hace.
        if (BuildConfig.DEBUG) enableStrictMode()

        // Deferir inicializaciones no críticas hasta que la app esté visible.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                appScope.launch {
                    // Habilitar Crashlytics de forma diferida (auto-collection desactivada en Manifest).
                    runCatching {
                        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                    }

                    // "Tocar" RTDB para inicializar en background, sin sincronización global.
                    runCatching {
                        FirebaseDatabase.getInstance().reference.keepSynced(false)
                    }
                }
            }
        })
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }
}
