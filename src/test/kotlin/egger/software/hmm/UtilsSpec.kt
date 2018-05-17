package egger.software.hmm

import egger.software.hmm.state.Tile.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.hamcrest.CoreMatchers.`is` as isEqualTo


class UtilsSpec {

    @Test
    fun `from a list of StateWithProbability an element with a given offset can be picked`() {

        val statesWithProbability = listOf(Red withProbabilityOf 0.2, Green withProbabilityOf 0.6, Blue withProbabilityOf 0.2)

        assertThat(statesWithProbability.selectStateAtOffset(0.5), isEqualTo(Green))
        assertThat(statesWithProbability.selectStateAtOffset(0.1), isEqualTo(Red))
        assertThat(statesWithProbability.selectStateAtOffset(0.0), isEqualTo(Red))
        assertThat(statesWithProbability.selectStateAtOffset(0.3), isEqualTo(Blue))
        assertThat(statesWithProbability.selectStateAtOffset(0.4), isEqualTo(Green))

        assertThrows<IllegalArgumentException> { statesWithProbability.selectStateAtOffset(-0.1) }
        assertThrows<IllegalArgumentException> { statesWithProbability.selectStateAtOffset(1.0) }

    }

    @Test
    fun `mutable maps can be initialized with default values for a given set of keys`() {

        assertThat(mutableMapOf<String, Int>().initUsing(setOf("A", "B"), 1), isEqualTo(
                mutableMapOf(
                        "A" to 1,
                        "B" to 1
                )))

    }

}
