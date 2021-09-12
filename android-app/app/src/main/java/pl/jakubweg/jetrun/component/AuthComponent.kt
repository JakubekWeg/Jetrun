package pl.jakubweg.jetrun.component

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import pl.jakubweg.jetrun.component.AuthState.*
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
    val authState: StateFlow<AuthState>
    fun signOut()
    fun createSignInProvider(): OAuthProvider
    suspend fun signIn(activity: Activity, provider: OAuthProvider): Boolean
    suspend fun signInAnonymously(): Boolean
}

class FirebaseAuthComponent @Inject constructor(
    private val auth: FirebaseAuth
) : AuthComponent, FirebaseAuth.AuthStateListener {

    private val _authState = MutableStateFlow<AuthState>(Unknown)

    override val authState = _authState.asStateFlow()

    init {
        auth.addAuthStateListener(this)
    }

    override fun signOut() {
        if (_authState.value !is Signed)
            return
        auth.signOut()
        setState(NotSigned())
    }

    override suspend fun signInAnonymously(): Boolean {
        check(_authState.value is NotSigned)
        check(auth.currentUser == null)

        setState(Authorizing)

        return try {
            val result = auth.signInAnonymously().await()
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

    override suspend fun signIn(activity: Activity, provider: OAuthProvider): Boolean {
        check(_authState.value is NotSigned)
        check(auth.currentUser == null)

        setState(Authorizing)

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

    override fun createSignInProvider(): OAuthProvider {
        val provider = OAuthProvider.newBuilder("github.com").apply {
            scopes = listOf("user:email")
        }.build()
        return provider
    }

    override fun onAuthStateChanged(_0: FirebaseAuth) {
        val currentUser = auth.currentUser
        setState(if (currentUser == null) NotSigned() else Signed(currentUser))
    }

    private fun setState(new: AuthState) {
        _authState.value = new
    }
}