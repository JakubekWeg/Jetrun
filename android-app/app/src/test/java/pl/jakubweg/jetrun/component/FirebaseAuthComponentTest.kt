package pl.jakubweg.jetrun.component

import com.google.firebase.auth.FirebaseAuth
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import pl.jakubweg.jetrun.component.AuthState.Unknown

@RunWith(JUnit4::class)
class FirebaseAuthComponentTest : TestCase() {
    private lateinit var component: FirebaseAuthComponent

    private lateinit var firebaseAuth: FirebaseAuth

    @Before
    fun setup() {
        firebaseAuth = mock(FirebaseAuth::class.java)

        component = FirebaseAuthComponent(firebaseAuth)
    }

    @Test
    fun `Default state is unknown`() {
        assert(component.authState.value is Unknown)
    }

    @Test
    fun `Can sign out when not signed`() {
        component.signOut()
        component.signOut()
    }

}