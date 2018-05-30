package egger.software.hmm

import egger.software.test.shouldBe
import kotlin.test.Test


class StateTransitionTableSpec {

    @Test
    fun `a state transition table can be created from a list of states`() {

        createStateTransitionTableFrom(listOf("a", "b", "c")) shouldBe stateTransitionTable {
            "a" resultsIn ("b" withProbabilityOf 1.0)
            "b" resultsIn ("c" withProbabilityOf 1.0)
        }

        createStateTransitionTableFrom(listOf("a", "b", "a", "c")) shouldBe stateTransitionTable {
            "a" resultsIn ("b" withProbabilityOf 0.5)
            "a" resultsIn ("c" withProbabilityOf 0.5)
            "b" resultsIn ("a" withProbabilityOf 1.0)
        }

        createStateTransitionTableFrom(listOf("a", "a", "b", "a")) shouldBe stateTransitionTable {
            "a" resultsIn ("a" withProbabilityOf 0.5)
            "a" resultsIn ("b" withProbabilityOf 0.5)
            "b" resultsIn ("a" withProbabilityOf 1.0)
        }

    }

    @Test
    fun `a higher order state transition table can be represented as a first order state transition table`() {
        // See: http://www.ussigbase.org/downloads/jadp_phd.pdf
        // Basic idea: Instead of creating new transitions combine the states to higher order states

        createStateTransitionTableFrom(listOf("a", "b", "c", "c", "d", "a", "b", "d").asCombinedStates(2)) shouldBe stateTransitionTable {
            combinedState("a", "b") resultsIn (combinedState("b", "c") withProbabilityOf 0.5)
            combinedState("b", "c") resultsIn (combinedState("c", "c") withProbabilityOf 1.0)
            combinedState("c", "c") resultsIn (combinedState("c", "d") withProbabilityOf 1.0)
            combinedState("c", "d") resultsIn (combinedState("d", "a") withProbabilityOf 1.0)
            combinedState("d", "a") resultsIn (combinedState("a", "b") withProbabilityOf 1.0)
            combinedState("a", "b") resultsIn (combinedState("b", "d") withProbabilityOf 0.5)
        }

    }

    @Test
    fun `a list of states can be converted to a list of combined states of a given size`() {

        listOf("a", "b", "c", "c", "d", "a", "b", "d").asCombinedStates(2) shouldBe listOf(
                combinedState("a", "b"),
                combinedState("b", "c"),
                combinedState("c", "c"),
                combinedState("c", "d"),
                combinedState("d", "a"),
                combinedState("a", "b"),
                combinedState("b", "d")
        )

    }

}

