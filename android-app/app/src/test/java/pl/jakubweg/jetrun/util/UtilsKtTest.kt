package pl.jakubweg.jetrun.util

import androidx.lifecycle.MutableLiveData
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito

@RunWith(JUnit4::class)
class UtilsKtTest : TestCase() {

    @Test
    fun `MutableLiveData$nonMutable returns the same instance`() {
        val data = MutableLiveData("ANYTHING")
        val data2 = data.nonMutable

        assertSame(data, data2)
    }
}

inline fun <reified T> anyNonNull(): T = Mockito.any(T::class.java)