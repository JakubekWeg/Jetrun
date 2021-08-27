package pl.jakubweg.jetrun.component

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.tasks.await
import pl.jakubweg.jetrun.component.AuthState.*
import pl.jakubweg.jetrun.util.nonMutable
import javax.inject.Inject

sealed class AuthState {
    /**
     * This is when firebase auth hasn't initialized yet, should wait
     */
    object Unknown : AuthState()
    class Signed(val user: FirebaseUser) : AuthState()
    object Authorizing : AuthState()
    open class NotSigned : AuthState() {
        class FailedToAuthorize(val reason: String) : NotSigned()
    }
}

interface AuthComponent {
    val authState: LiveData<AuthState>
    fun signOut()
    suspend fun signIn(activity: Activity): Boolean
}

class FirebaseAuthComponent @Inject constructor(
    private val auth: FirebaseAuth
) : AuthComponent, FirebaseAuth.AuthStateListener {

    private val _authState = MutableLiveData<AuthState>(Unknown)

    override val authState: LiveData<AuthState>
        get() = _authState.nonMutable

    init {
        auth.addAuthStateListener(this)
    }

    override fun signOut() {
        if (_authState.value !is Signed)
            return
        auth.signOut()
        setState(NotSigned())
    }

    override suspend fun signIn(activity: Activity): Boolean {
        check(_authState.value is NotSigned)
        check(auth.currentUser == null)

        setState(Authorizing)

        val provider = OAuthProvider.newBuilder("github.com").apply {
            scopes = listOf("user:email")
        }.build()

        return try {
            val result = auth.startActivityForSignInWithProvider(activity, provider).await()
            val user = result?.user
            setState(
                if (user == null)
                    NotSigned()
                else
                    Signed(user)
            )
            user != null
            true
        } catch (e: FirebaseException) {
            // cancelled
            setState(NotSigned.FailedToAuthorize(e.localizedMessage ?: "unknown error"))
            false
        }
    }

    override fun onAuthStateChanged(_0: FirebaseAuth) {
        val currentUser = auth.currentUser
        setState(if (currentUser == null) NotSigned() else Signed(currentUser))
    }

    private fun setState(new: AuthState) {
        _authState.postValue(new)
    }
}