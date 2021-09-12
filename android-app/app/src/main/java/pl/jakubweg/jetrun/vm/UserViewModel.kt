package pl.jakubweg.jetrun.vm

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.jakubweg.jetrun.component.AuthComponent
import javax.inject.Inject


@HiltViewModel
class UserViewModel @Inject constructor(
    private val auth: AuthComponent
) : ViewModel() {
    private var works = false

    fun signInWithGithub(activity: Activity) {
        if (works) return
        works = true
        viewModelScope.launch {
            val provider = auth.createSignInProvider()
            auth.signIn(activity, provider)
            works = false
        }
    }


    fun signInAnonymously() {
        if (works) return
        works = true
        viewModelScope.launch {
            auth.signInAnonymously()
            works = false
        }
    }

    fun signOut() {
        auth.signOut()
    }

    val state get() = auth.authState
}