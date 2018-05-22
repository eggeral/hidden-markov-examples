package egger.software.hmm.example

import egger.software.hmm.HiddenMarkovModel
import egger.software.hmm.algorithm.calculateForwardBackward
import egger.software.hmm.algorithm.probabilityOfObservedSequence
import egger.software.hmm.observing
import egger.software.hmm.stateTransitionTable
import egger.software.hmm.withProbabilityOf
import egger.software.test.exactly
import egger.software.test.plusOrMinus
import egger.software.test.shouldBe
import kotlin.test.Test

class ForwardBackwardExamples {

    @Test
    fun `for a given the Hidden Markov Model the probability of an observed sequence can be calculated`() {
        // The following examples are described in:
        // https://hidden-markov.readthedocs.io/en/latest/example.html
        // https://github.com/Red-devilz/hidden_markov/blob/master/hidden_markov/hmm_class.py

        // given
        // a HMM initialized using the parameters of the example
        val transitionProbability = stateTransitionTable<String, String> {

            "s" resultsIn ("s" withProbabilityOf 0.6)
            "s" resultsIn ("t" withProbabilityOf 0.4)

            "t" resultsIn ("s" withProbabilityOf 0.3)
            "t" resultsIn ("t" withProbabilityOf 0.7)

        }

        val emissionProbability = stateTransitionTable<String, String> {

            "s" resultsIn ("A" withProbabilityOf 0.3)
            "s" resultsIn ("B" withProbabilityOf 0.7)

            "t" resultsIn ("A" withProbabilityOf 0.4)
            "t" resultsIn ("B" withProbabilityOf 0.6)

        }

        val startingProbability = listOf("s" withProbabilityOf 0.5, "t" withProbabilityOf 0.5)

        val hmm = HiddenMarkovModel(initialStateProbabilities = startingProbability,
                stateTransitions = transitionProbability,
                observationProbabilities = emissionProbability)


        // when
        // we observe A B B A

        // then
        // the probability of the observed sequence should be 0.051533999999999996"
        hmm.observing("A", "B", "B", "A").probabilityOfObservedSequence() shouldBe 0.051533999999999996.plusOrMinus(10E-9)


    }

    @Test
    fun `the probabilities of being in a hidden state given a sequence of observed states can be calculated (Wikipedia example)`() {
        // The following examples are described in:
        // https://en.wikipedia.org/wiki/Forward–backward_algorithm

        // given
        // a HMM initialized using the parameters of the Wikipedia forward backward example

        val transitionProbability = stateTransitionTable<String, String> {

            "Rain" resultsIn ("Rain" withProbabilityOf 0.7)
            "Rain" resultsIn ("No Rain" withProbabilityOf 0.3)

            "No Rain" resultsIn ("Rain" withProbabilityOf 0.3)
            "No Rain" resultsIn ("No Rain" withProbabilityOf 0.7)

        }

        val emissionProbability = stateTransitionTable<String, String> {

            "Rain" resultsIn ("umbrella" withProbabilityOf 0.9)
            "Rain" resultsIn ("no umbrella" withProbabilityOf 0.1)

            "No Rain" resultsIn ("umbrella" withProbabilityOf 0.2)
            "No Rain" resultsIn ("no umbrella" withProbabilityOf 0.8)

        }

        val startingProbability =
                listOf("Rain" withProbabilityOf 0.5, "No Rain" withProbabilityOf 0.5)

        val hmm = HiddenMarkovModel(initialStateProbabilities = startingProbability,
                stateTransitions = transitionProbability,
                observationProbabilities = emissionProbability)


        // when
        // we calculate the result of the forward backward algorithm

        // then
        // the results are the same like in the Wikipedia article
        val result = hmm.observing("umbrella", "umbrella", "no umbrella", "umbrella", "umbrella").calculateForwardBackward()

        result.forward shouldBe listOf(

                mapOf("Rain" to 0.5.exactly, "No Rain" to 0.5.exactly),
                mapOf("Rain" to 0.8182.plusOrMinus(1E-4), "No Rain" to 0.1818.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.8834.plusOrMinus(1E-4), "No Rain" to 0.1166.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.1907.plusOrMinus(1E-4), "No Rain" to 0.8093.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.7308.plusOrMinus(1E-4), "No Rain" to 0.2692.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.8673.plusOrMinus(1E-4), "No Rain" to 0.1327.plusOrMinus(1E-4))

        )

        result.backward shouldBe listOf(

                mapOf("Rain" to 0.6469.plusOrMinus(1E-4), "No Rain" to 0.3531.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.5923.plusOrMinus(1E-4), "No Rain" to 0.4077.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.3762.plusOrMinus(1E-4), "No Rain" to 0.6237.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.6533.plusOrMinus(1E-4), "No Rain" to 0.3467.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.6272.plusOrMinus(1E-4), "No Rain" to 0.3727.plusOrMinus(1E-4)),
                mapOf("Rain" to 1.0.exactly, "No Rain" to 1.0.exactly)

        )

        result.posterior shouldBe listOf(

                mapOf("Rain" to 0.6469.plusOrMinus(1E-4), "No Rain" to 0.3531.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.8673.plusOrMinus(1E-4), "No Rain" to 0.1327.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.8204.plusOrMinus(1E-4), "No Rain" to 0.1796.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.3075.plusOrMinus(1E-4), "No Rain" to 0.6925.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.8204.plusOrMinus(1E-4), "No Rain" to 0.1796.plusOrMinus(1E-4)),
                mapOf("Rain" to 0.8673.plusOrMinus(1E-4), "No Rain" to 0.1327.plusOrMinus(1E-4))

        )
    }

    @Test
    fun `the probabilities of being in a hidden state given a sequence of observed states can be calculated (Wikipedia Python example)`() {
        // The following examples are described in:
        // https://en.wikipedia.org/wiki/Forward–backward_algorithm

        // given
        // a HMM initialized using the parameters of the Wikipedia Python forward backward example") {

        val transitionProbability = stateTransitionTable<String, String> {

            "Healthy" resultsIn ("Healthy" withProbabilityOf 0.69)
            "Healthy" resultsIn ("Fever" withProbabilityOf 0.3)
            "Healthy" resultsIn ("E" withProbabilityOf 0.01)

            "Fever" resultsIn ("Healthy" withProbabilityOf 0.4)
            "Fever" resultsIn ("Fever" withProbabilityOf 0.59)
            "Fever" resultsIn ("E" withProbabilityOf 0.01)

            "E" resultsIn ("Healthy" withProbabilityOf 0.0)
            "E" resultsIn ("Fever" withProbabilityOf 0.0)
            "E" resultsIn ("E" withProbabilityOf 1.0)
        }

        val emissionProbability = stateTransitionTable<String, String> {

            "Healthy" resultsIn ("normal" withProbabilityOf 0.5)
            "Healthy" resultsIn ("cold" withProbabilityOf 0.4)
            "Healthy" resultsIn ("dizzy" withProbabilityOf 0.1)

            "Fever" resultsIn ("normal" withProbabilityOf 0.1)
            "Fever" resultsIn ("cold" withProbabilityOf 0.3)
            "Fever" resultsIn ("dizzy" withProbabilityOf 0.6)

            "E" resultsIn ("normal" withProbabilityOf 0.0)
            "E" resultsIn ("cold" withProbabilityOf 0.0)
            "E" resultsIn ("dizzy" withProbabilityOf 0.0)

        }

        val startingProbability =
                listOf("Healthy" withProbabilityOf 0.6, "Fever" withProbabilityOf 0.4, "E" withProbabilityOf 0.0)

        val hmm = HiddenMarkovModel(initialStateProbabilities = startingProbability,
                stateTransitions = transitionProbability,
                observationProbabilities = emissionProbability)

        // when
        // we calculate the result of the forward backward algorithm
        val result = hmm.observing("normal", "cold", "dizzy").calculateForwardBackward(normalize = false)

        // then
        // the results are the same like in the Wikipedia article

        result.forward shouldBe listOf(

                mapOf("Healthy" to 0.6, "Fever" to 0.4, "E" to 0.0), // pseudo time 0 added by the Wikipedia algorithm
                mapOf("Healthy" to 0.3, "Fever" to 0.04000000000000001, "E" to 0.0),
                mapOf("Healthy" to 0.0892, "Fever" to 0.03408, "E" to 0.0),
                mapOf("Healthy" to 0.007518, "Fever" to 0.028120319999999997, "E" to 0.0)

        )

        result.backward shouldBe listOf(

                mapOf("Healthy" to 0.03923082, "Fever" to 0.027301902000000003, "E" to 0.0),
                mapOf("Healthy" to 0.104184, "Fever" to 0.10957800000000001, "E" to 0.0),
                mapOf("Healthy" to 0.249, "Fever" to 0.394, "E" to 0.0),
                mapOf("Healthy" to 1.0, "Fever" to 1.0, "E" to 1.0)

        )

        result.posterior shouldBe listOf(

                mapOf("Healthy" to 0.023538491999999998, "Fever" to 0.010920760800000002, "E" to 0.0),
                mapOf("Healthy" to 0.0312552, "Fever" to 0.004383120000000002, "E" to 0.0),
                mapOf("Healthy" to 0.0222108, "Fever" to 0.01342752, "E" to 0.0),
                mapOf("Healthy" to 0.007518, "Fever" to 0.028120319999999997, "E" to 0.0)
        )
    }

}





