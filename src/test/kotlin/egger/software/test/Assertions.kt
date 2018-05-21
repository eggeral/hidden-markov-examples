package egger.software.test

import kotlin.test.assertEquals
import kotlin.test.asserter

infix fun <TValue> TValue.shouldBe(expected: TValue?) = assertEquals(expected, this)

fun <TValue> TValue.shouldBe(message: String?, matcher: (String?, TValue) -> Unit) = matcher(message, this)
infix fun <TValue> TValue.shouldBe(matcher: (String?, TValue) -> Unit) = matcher(null, this)

infix fun <TKey, TValue> List<Map<TKey, TValue>>.shouldBe(matcher: List<Map<TKey, (String?, TValue) -> Unit>>) {

    assertEquals(this.size, matcher.size, "list sizes don't match")
    this.zip(matcher).forEachIndexed { index, (actualMap, expectedMap) ->
        assertEquals(expectedMap.size, actualMap.size, "map sizes don't match at list index: $index")
        actualMap.entries.zip(expectedMap.entries).forEach { (actual, expected) ->
            assertEquals(expected.key, actual.key, "map keys don`t match at list index: $index")
            actual.value.shouldBe("map value does not match for key: ${expected.key} at list index $index", expected.value)
        }
    }

}

inline infix fun (() -> Any).shouldThrow(exceptionMatcher: (Throwable) -> Boolean) {
    try {
        this()
    } catch (exception: Throwable) {
        if (!exceptionMatcher(exception))
            failWithMessage(null, "expected an exception of to be thrown, but it was $exception which did not match the criteria.")
        else
            return
    }
    failWithMessage(null, "expected an exception to be thrown, but was completed successfully.")
}

fun Double.plusOrMinus(tolerance: Double): (String?, Double) -> Unit = { message, expected ->

    val diff = Math.abs(this - expected)
    if (diff > tolerance)
        failWithMessage(message, "expected:<$expected> plus or minus $tolerance, but was:<$this>")

}

val Double.exactly: (String?, Double) -> Unit get() = { message, expected -> assertEquals(expected, this, message) }


fun greaterThan(value: Double): (String?, Double) -> Unit = { message, expected ->

    if (expected <= value)
        failWithMessage(message, "expected:<$expected> to be greater than $value")

}

fun failWithMessage(messagePrefix: String?, message: String) {

    val prefix = if (messagePrefix == null) "" else "$messagePrefix. "
    asserter.fail("$prefix$message")

}
