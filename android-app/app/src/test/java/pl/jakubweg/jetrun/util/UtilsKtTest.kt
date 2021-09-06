package pl.jakubweg.jetrun.util

import androidx.lifecycle.MutableLiveData
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito
import kotlin.reflect.KClass

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

inline fun <reified T> assertIs(expectedClass: Class<T>, value: Any?) {
    if (value !is T)
        Assert.fail("Expected value of class ${expectedClass.simpleName}, but got ${value?.javaClass?.simpleName}")
}


fun assertIs(expectedTypeOf: Any, value: Any?) = assertIs(expectedTypeOf.javaClass, value)

inline fun <reified T : Any> assertIs(clazz: KClass<T>, value: Any?) = assertIs(clazz.java, value)