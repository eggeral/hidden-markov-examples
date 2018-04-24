package egger.software.hmm

import io.kotlintest.matchers.plusOrMinus
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.BehaviorSpec


class BaumWelchExamples : BehaviorSpec() {

    init {
        // The following examples are described in:
        // https://hidden-markov.readthedocs.io/en/latest/example.html
        // https://github.com/Red-devilz/hidden_markov/blob/master/hidden_markov/hmm_class.py
        Given("a HMM initialized using the parameters of the example") {
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


            When("we observe A B B A") {

                Then("the probability of the observed sequence should be 0.051533999999999996") {
                    hmm.observing("A", "B", "B", "A").probabilityOfObservedSequence() shouldBe 0.051533999999999996.plusOrMinus(10E-9)
                }

            }
        }

        // The following examples are described in:
        // https://en.wikipedia.org/wiki/Forward–backward_algorithm
        Given("a HMM initialized using the parameters of the Wikipedia forward backward example") {

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


            When("we calculate the result of the forward backward algorith") {

                Then("the results are the same like in the Wikipedia article") {
                    val result = hmm.observing("umbrella", "umbrella", "no umbrella", "umbrella", "umbrella").calculateForwardBackward()

                    result.forward shouldBe listOf(
                            mapOf("Rain" to 0.8181818181818181, "No Rain" to 0.18181818181818182),
                            mapOf("Rain" to 0.883357041251778, "No Rain" to 0.11664295874822192),
                            mapOf("Rain" to 0.19066793972352525, "No Rain" to 0.8093320602764749),
                            mapOf("Rain" to 0.730794004584982, "No Rain" to 0.26920599541501794),
                            mapOf("Rain" to 0.8673388895754847, "No Rain" to 0.13266111042451528)
                    )

                    result.backward shouldBe listOf(
                            mapOf("Rain" to 0.6469355558301939, "No Rain" to 0.35306444416980615),
                            mapOf("Rain" to 0.5923176018339928, "No Rain" to 0.4076823981660072),
                            mapOf("Rain" to 0.37626717588941017, "No Rain" to 0.62373282411059),
                            mapOf("Rain" to 0.6533428165007112, "No Rain" to 0.34665718349928876),
                            mapOf("Rain" to 0.6272727272727272, "No Rain" to 0.37272727272727274),
                            mapOf("Rain" to 1.0, "No Rain" to 1.0)
                    )

                }

            }
        }

        // The following examples are described in:
        // https://en.wikipedia.org/wiki/Forward–backward_algorithm
        Given("a HMM initialized using the parameters of the Wikipedia Python forward backward example") {

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


            When("we calculate the result of the forward backward algorith") {

                Then("the results are the same like in the Wikipedia artikle") {
                    val result = hmm.observing("normal", "cold", "dizzy").calculateForwardBackward()

                    // not the same results as in the article!
                    // This is because my implementation does not use the python
                    // implementation but implementation in the article which also normalizes
                    // the results to 1.0
                    result.forward shouldBe listOf(

                            mapOf("Healthy" to 0.8823529411764707, "Fever" to 0.11764705882352944, "E" to 0.0),
                            mapOf("Healthy" to 0.7235561323815705, "Fever" to 0.2764438676184296, "E" to 0.0),
                            mapOf("Healthy" to 0.21095270484130565, "Fever" to 0.7890472951586943, "E" to 0.0)

                    )

                    result.backward shouldBe listOf(
                            mapOf("Healthy" to 0.58964700106513, "Fever" to 0.41035299893487, "E" to 0.0),
                            mapOf("Healthy" to 0.4873831644539253, "Fever" to 0.5126168355460746, "E" to 0.0),
                            mapOf("Healthy" to 0.38724727838258166, "Fever" to 0.6127527216174183, "E" to 0.0),
                            mapOf("Healthy" to 1.0, "Fever" to 1.0, "E" to 1.0)
                    )
                }

            }
        }

    }
}





