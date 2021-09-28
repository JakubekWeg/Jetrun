@file:Suppress("unused")

package pl.jakubweg.jetrun

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import pl.jakubweg.jetrun.BuildConfig
import java.lang.ref.WeakReference

@HiltAndroidApp
class JetRunApplication : Application() {
    companion object {
        val globalScope = CoroutineScope(Dispatchers.Default)
        private var instance = WeakReference<JetRunApplication>(null)
        val INSTANCE
            get() = instance.get() ?: throw IllegalStateException("Application reference is null")

        val dataStore get() = INSTANCE.dataStore
    }


    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)

        if (BuildConfig.DEBUG)
            setUpFirebaseEmulators()
    }

    private fun setUpFirebaseEmulators() {
        globalScope.launch {
            val emulatorHost = "10.0.0.2"
            Firebase.auth.useEmulator(emulatorHost, 9099)
            Firebase.firestore.useEmulator(emulatorHost, 8080)
        }
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")