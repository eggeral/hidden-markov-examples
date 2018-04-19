package egger.software.hmm

import io.kotlintest.matchers.plusOrMinus
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.BehaviorSpec

// The following examples are described in:
// https://hidden-markov.readthedocs.io/en/latest/example.html
// https://github.com/Red-devilz/hidden_markov/blob/master/hidden_markov/hmm_class.py

class BaumWelchExamples : BehaviorSpec() {

    init {
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
    }
}





