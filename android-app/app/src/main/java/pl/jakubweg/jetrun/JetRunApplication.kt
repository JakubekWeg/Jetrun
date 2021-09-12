@file:Suppress("unused")

package pl.jakubweg.jetrun

import android.app.Application
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@HiltAndroidApp
@DelicateCoroutinesApi
class JetRunApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG)
            setUpFirebaseEmulators()
    }

    private fun setUpFirebaseEmulators() {
        GlobalScope.launch(Dispatchers.Default) {
            val emulatorHost = "10.0.0.2"
            Firebase.auth.useEmulator(emulatorHost, 9099)
            Firebase.firestore.useEmulator(emulatorHost, 8080)
        }
    }
}