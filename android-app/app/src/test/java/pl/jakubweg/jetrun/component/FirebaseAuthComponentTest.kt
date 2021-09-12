package pl.jakubweg.jetrun.component

import android.app.Activity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.*
import pl.jakubweg.jetrun.component.AuthState.*
import pl.jakubweg.jetrun.util.assertIs


@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
@Suppress("UNCHECKED_CAST")
class FirebaseAuthComponentTest : TestCase() {
    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    private var listener: FirebaseAuth.AuthStateListener? = null
    private lateinit var component: FirebaseAuthComponent

    private lateinit var firebaseAuth: FirebaseAuth

    @Before
    fun setup() {
        firebaseAuth = mock(FirebaseAuth::class.java)
        `when`(firebaseAuth.addAuthStateListener(any())).thenAnswer {
            listener = it.getArgument<FirebaseAuth.AuthStateListener>(0)
            listener
        }

        component = FirebaseAuthComponent(firebaseAuth)
    }

    @Test
    fun `Default state is unknown`() {
        assertIs(Unknown::class, component.authState.value)
    }

    @Test
    fun `Can sign out when not signed`() {
        component.signOut()
        component.signOut()
    }

    @Test
    fun `Sign in happens`() = runBlockingTest {
        val activity = mock(Activity::class.java)
        val provider = mock(OAuthProvider::class.java)
        val task = mock(Task::class.java) as Task<AuthResult>
        val result = mock(AuthResult::class.java)
        val user = mock(FirebaseUser::class.java)

        `when`(firebaseAuth.currentUser).thenReturn(null)
        `when`(firebaseAuth.startActivityForSignInWithProvider(activity, provider)).thenReturn(task)
        `when`(task.isComplete).thenReturn(true)
        `when`(task.result).thenReturn(result)
        `when`(result.user).thenReturn(user)

        listener?.onAuthStateChanged(firebaseAuth)
        assertIs(NotSigned::class, component.authState.value)

        component.signIn(activity, provider)
        assertIs(Signed::class, component.authState.value)
    }

    @Test
    fun `Sign in not happens when result is null`() = runBlockingTest {
        val activity = mock(Activity::class.java)
        val provider = mock(OAuthProvider::class.java)
        val task = mock(Task::class.java) as Task<AuthResult>

        `when`(firebaseAuth.currentUser).thenReturn(null)
        `when`(firebaseAuth.startActivityForSignInWithProvider(activity, provider)).thenReturn(task)
        `when`(task.isComplete).thenReturn(true)
        `when`(task.result).thenReturn(null)

        listener?.onAuthStateChanged(firebaseAuth)
        assertIs(NotSigned::class, component.authState.value)

        component.signIn(activity, provider)
        assertIs(NotSigned::class, component.authState.value)
    }

    @Test
    fun `Sign in not happens when result$user is null`() = runBlockingTest {
        val activity = mock(Activity::class.java)
        val provider = mock(OAuthProvider::class.java)
        val task = mock(Task::class.java) as Task<AuthResult>
        val result = mock(AuthResult::class.java)

        `when`(firebaseAuth.currentUser).thenReturn(null)
        `when`(firebaseAuth.startActivityForSignInWithProvider(activity, provider)).thenReturn(task)
        `when`(task.isComplete).thenReturn(true)
        `when`(task.result).thenReturn(result)
        `when`(result.user).thenReturn(null)

        listener?.onAuthStateChanged(firebaseAuth)
        assertIs(NotSigned::class, component.authState.value)

        component.signIn(activity, provider)
        assertIs(NotSigned::class, component.authState.value)
    }


    @Test
    fun `Sign in not happens when firebase throws`() = runBlockingTest {
        val activity = mock(Activity::class.java)
        val provider = mock(OAuthProvider::class.java)
        val task = mock(Task::class.java) as Task<AuthResult>

        `when`(firebaseAuth.currentUser).thenReturn(null)
        `when`(firebaseAuth.startActivityForSignInWithProvider(activity, provider)).thenReturn(task)
        `when`(task.isComplete).thenReturn(true)
        `when`(task.exception).thenReturn(FirebaseException("ERROR"))

        listener?.onAuthStateChanged(firebaseAuth)
        assertIs(NotSigned::class, component.authState.value)

        component.signIn(activity, provider)
        assertIs(NotSigned::class, component.authState.value)
        assertIs(NotSigned.FailedToAuthorize::class, component.authState.value)
        assertEquals("ERROR", (component.authState.value as NotSigned.FailedToAuthorize).reason)
    }

    @Test
    fun `Sign in anonymously works`() = runBlockingTest {
        val task = mock(Task::class.java) as Task<AuthResult>
        val result = mock(AuthResult::class.java)
        val user = mock(FirebaseUser::class.java)

        `when`(firebaseAuth.currentUser).thenReturn(null)
        `when`(firebaseAuth.signInAnonymously()).thenReturn(task)
        `when`(task.isComplete).thenReturn(true)
        `when`(task.result).thenReturn(result)
        `when`(result.user).thenReturn(user)

        listener?.onAuthStateChanged(firebaseAuth)
        assertIs(NotSigned::class, component.authState.value)

        component.signInAnonymously()
        assertIs(Signed::class, component.authState.value)
    }

    @Test(expected = Throwable::class)
    fun `Attempt to sign in when already signed fails`() = runBlockingTest {
        val task = mock(Task::class.java) as Task<AuthResult>
        val result = mock(AuthResult::class.java)
        val user = mock(FirebaseUser::class.java)

        `when`(firebaseAuth.currentUser).thenReturn(null)
        `when`(firebaseAuth.signInAnonymously()).thenReturn(task)
        `when`(task.isComplete).thenReturn(true)
        `when`(task.result).thenReturn(result)
        `when`(result.user).thenReturn(user)


        listener?.onAuthStateChanged(firebaseAuth)
        component.signInAnonymously()
        assertIs(Signed::class, component.authState.value)

        component.signInAnonymously()
    }

}