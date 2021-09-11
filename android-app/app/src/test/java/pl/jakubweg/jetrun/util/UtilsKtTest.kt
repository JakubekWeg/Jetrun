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

    @Test
    fun `assertThrows works if thrown`() {
        assertThrows { throw Exception() }
    }

    @Test(expected = Throwable::class)
    fun `assertThrows works if not thrown`() {
        assertThrows { }
    }

    @Test
    fun `assertIs works when using class java`() {
        open class A
        class B : A()

        val a = A()
        val b = B()

        assertIs(A::class.java, a)
        assertIs(A::class.java, b)
        assertIs(B::class.java, b)
        assertThrows {
            assertIs(B::class.java, a)
        }
    }

    @Test
    fun `assertIs works when using class`() {
        open class A
        class B : A()

        val a = A()
        val b = B()

        assertIs(A::class, a)
        assertIs(A::class, b)
        assertIs(B::class, b)
        assertThrows {
            assertIs(B::class, a)
        }
    }

    @Test
    fun `assertIs works when using instances`() {
        open class A
        class B : A()

        val a = A()
        val b = B()

        assertIs(a, a)
        assertIs(a, b)
        assertIs(b, b)
        assertThrows {
            assertIs(b, a)
        }
    }
}

fun assertThrows(callback: () -> Unit) {
    try {
        callback.invoke()
    } catch (e: Throwable) {
        // ignore
        return
    }
    Assert.fail("This method should have thrown an exception")
}

inline fun <reified T> anyNonNull(): T = Mockito.any(T::class.java)

inline fun <reified T> assertIs(expectedClass: Class<T>, value: Any?) {
    if (value == null || !expectedClass.isInstance(value))
        Assert.fail("Expected value of class ${expectedClass.simpleName}, but got ${value?.javaClass?.simpleName}")
}


fun assertIs(expectedTypeOf: Any, value: Any?) = assertIs(expectedTypeOf.javaClass, value)

inline fun <reified T : Any> assertIs(clazz: KClass<T>, value: Any?) = assertIs(clazz.java, value)