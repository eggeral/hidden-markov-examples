package egger.software.test


import org.junit.Test
import kotlin.test.fail

class AssertionsSpec {

    @Test
    fun `shouldThrow tests if an exception is thrown within a code block`() {

        { throw IllegalStateException("throw") } shouldThrow { exception -> exception is IllegalStateException }
        { throw IllegalStateException("throw") } shouldThrow { exception -> exception.message == "throw" }

        try {
            { 1 + 1 } shouldThrow { exception -> exception is IllegalStateException }
            fail("AssertionError not thrown for failing shouldThrow")
        } catch (exception: AssertionError) {
            exception.message shouldBe "expected an exception to be thrown, but was completed successfully."
        }

        try {
            { throw IllegalStateException("throw") } shouldThrow { exception -> exception.message == "bla" }
            fail("AssertionError not thrown for failing shouldThrow")
        } catch (exception: AssertionError) {
            exception.message shouldBe "expected an exception of to be thrown, but it was java.lang.IllegalStateException: throw which did not match the criteria."
        }

    }

    @Test
    fun `shouldBe tests if two objects are equal`() {

        2 shouldBe 2
        "A" shouldBe "A"

        { 1 shouldBe 2 } shouldThrow { exception -> exception is AssertionError && exception.message == "expected:<2> but was:<1>" }

    }

    @Test
    fun `greaterThan tests if a double is greater than another`() {

        2.0 shouldBe greaterThan(1.0)

        ({ 1.0 shouldBe greaterThan(2.0) }) shouldThrow { exception -> exception is AssertionError && exception.message == "expected:<1.0> to be greater than 2.0" }

    }

    @Test
    fun `doubles can be compared by a given precision`() {

        2.01 shouldBe 2.0.plusOrMinus(1E-2)

        ({ 2.01 shouldBe 2.0.plusOrMinus(1E-3) }) shouldThrow { exception -> exception is AssertionError && exception.message == "expected:<2.0> plus or minus 0.001, but was:<2.01>" }

    }

    @Test
    fun `doubles can be compared exactly`() {

        2.01 shouldBe 2.01.exactly

        ({ 2.01 shouldBe 2.0.exactly }) shouldThrow { exception -> exception is AssertionError && exception.message == "expected:<2.01> but was:<2.0>" }

    }


    @Test
    fun `list of maps with doubles can be compared by a given precision`() {


        listOf(

                mapOf("a" to 1.01, "b" to 2.01),
                mapOf("c" to 3.01)

        ) shouldBe listOf(

                mapOf("a" to 1.0.plusOrMinus(1E-1), "b" to 2.0.plusOrMinus(1E-1)),
                mapOf("c" to 3.0.plusOrMinus(1E-1))

        );

        {
            (listOf(

                    mapOf("a" to 1.01, "b" to 2.01),
                    mapOf("c" to 3.01)

            ) shouldBe listOf(

                    mapOf("a" to 1.0.plusOrMinus(1E-1), "b" to 2.0.plusOrMinus(1E-1))

            ))
        } shouldThrow { exception -> exception is AssertionError && exception.message == "list sizes don't match expected:<2> but was:<1>" }

        {
            (listOf(

                    mapOf("a" to 1.01, "b" to 2.01),
                    mapOf("c" to 3.01)

            ) shouldBe listOf(

                    mapOf("a" to 1.0.plusOrMinus(1E-1)),
                    mapOf("c" to 3.0.plusOrMinus(1E-1))

            ))
        } shouldThrow { exception -> exception is AssertionError && exception.message == "map sizes don't match at list index: 0 expected:<1> but was:<2>" }

        {
            listOf(

                    mapOf("a" to 1.01, "b" to 2.01),
                    mapOf("c" to 3.01)

            ) shouldBe listOf(

                    mapOf("a" to 1.0.plusOrMinus(1E-1), "d" to 2.0.plusOrMinus(1E-1)),
                    mapOf("c" to 3.0.plusOrMinus(1E-1))

            )
        } shouldThrow { exception -> exception is AssertionError && exception.message == "map keys don`t match at list index: 0 expected:<[d]> but was:<[b]>" }


        {
            listOf(

                    mapOf("a" to 1.01, "b" to 2.01),
                    mapOf("c" to 3.01)

            ) shouldBe listOf(

                    mapOf("a" to 1.0.plusOrMinus(1E-2), "b" to 2.0.plusOrMinus(1E-1)),
                    mapOf("c" to 3.0.plusOrMinus(1E-1))

            )
        } shouldThrow { exception -> exception is AssertionError && exception.message == "map value does not match for key: a at list index 0. expected:<1.0> plus or minus 0.01, but was:<1.01>" }

    }


}
