package pl.jakubweg.jetrun.vm

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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

@RunWith(JUnit4::class)
@ExperimentalCoroutinesApi
class UserViewModelTest : TestCase() {
    @Test
    fun `Gets status`() {
        val auth = mock(AuthComponent::class.java)
        val state = MutableLiveData<AuthState>(NotSigned())
        `when`(auth.authState).thenReturn(state)

        val vm = UserViewModel(auth)

        assertTrue(vm.state === state)
    }

    @Test
    fun `Sign in redirects single call to auth component`() = runBlockingTest {
        Dispatchers.setMain(Dispatchers.Default)
        val auth = mock(AuthComponent::class.java)

        val vm = UserViewModel(auth)

        val activity = mock(Activity::class.java)

//        `when`(auth.signIn(activity, any())).thenReturn(true)
        `when`(auth.signInAnonymously()).thenReturn(true)

        pauseDispatcher()
        vm.signIn(activity)
        vm.signIn(activity)
        resumeDispatcher()

//        verify(auth, times(1)).signIn(activity, any())
        verify(auth, times(1)).signInAnonymously()

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