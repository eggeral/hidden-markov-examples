package egger.software.test

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

infix fun <T> T.shouldBe(expected: T?): T = this.apply { assertEquals(expected, this) }
infix fun <T> T.shouldBe(matcher: (T) -> Unit): T = this.apply { matcher(this) }
inline fun <reified T : kotlin.Throwable> shouldThrow(message: kotlin.String? = null, noinline block: () -> kotlin.Unit): T = assertFailsWith(message, block)

fun Double.plusOrMinus(tolerance: Double): (Double) -> Unit = { expected ->
    val diff = Math.abs(this - expected)
    if (diff > tolerance)
        throw  AssertionError("$this should be equal to $expected plus or minus $tolerance")
}

fun greaterThan(value: Double): (Double) -> Unit = { expected ->
    if (expected <= value)
        throw  AssertionError("$expected should be greater than $value")
}
