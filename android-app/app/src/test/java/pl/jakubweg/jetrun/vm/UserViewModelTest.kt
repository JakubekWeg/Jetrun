package pl.jakubweg.jetrun.vm

import android.app.Activity
import com.google.firebase.auth.OAuthProvider
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.*
import pl.jakubweg.jetrun.component.AuthComponent
import pl.jakubweg.jetrun.component.AuthState
import pl.jakubweg.jetrun.component.AuthState.NotSigned
import pl.jakubweg.jetrun.util.anyNonNull

@RunWith(JUnit4::class)
@ExperimentalCoroutinesApi
class UserViewModelTest : TestCase() {
    private val testDispatcher = TestCoroutineDispatcher()

    @Test
    fun `Gets status`() {
        val auth = mock(AuthComponent::class.java)
        val state = MutableStateFlow<AuthState>(NotSigned())
        `when`(auth.authState).thenReturn(state)

        val vm = UserViewModel(auth)

        assertTrue(vm.state === state)
    }

    @Test
    fun `Sign in as anon redirects single call to auth component`() =
        testDispatcher.runBlockingTest {
            Dispatchers.setMain(testDispatcher)
            val auth = mock(AuthComponent::class.java)

            val vm = UserViewModel(auth)

            `when`(auth.signInAnonymously()).thenReturn(true)

            pauseDispatcher()
            vm.signInAnonymously()
            vm.signInAnonymously()
            resumeDispatcher()

            verify(auth, times(1)).signInAnonymously()
            Dispatchers.resetMain()
        }


    @Test
    fun `Sign in with github redirects single call to auth component`() =
        testDispatcher.runBlockingTest {
            Dispatchers.setMain(testDispatcher)
            val activity = mock(Activity::class.java)
            val provider = mock(OAuthProvider::class.java)

            val auth = mock(AuthComponent::class.java)
            `when`(auth.createSignInProvider()).thenReturn(provider)

            val vm = UserViewModel(auth)


            `when`(auth.signIn(anyNonNull(), anyNonNull())).thenAnswer {
                require(it.getArgument<Any?>(0) === activity)
                require(it.getArgument<Any?>(1) === provider)
                return@thenAnswer true
            }

            pauseDispatcher()
            vm.signInWithGithub(activity)
            vm.signInWithGithub(activity)
            resumeDispatcher()

            verify(auth, times(1)).signIn(anyNonNull(), anyNonNull())
            Dispatchers.resetMain()
        }

    @Test
    fun `Sign out redirects call to auth component`() {
        val auth = mock(AuthComponent::class.java)
        val vm = UserViewModel(auth)

        vm.signOut()

        verify(auth, times(1)).signOut()
    }
}