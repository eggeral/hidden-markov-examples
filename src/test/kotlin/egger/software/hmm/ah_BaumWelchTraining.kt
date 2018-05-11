package egger.software.hmm

import io.kotlintest.specs.BehaviorSpec
import java.util.*


class BaumWelchExamples : BehaviorSpec() {

    init {
        // The following examples are described in:
        // https://en.wikipedia.org/wiki/Baum–Welch_algorithm
        Given("a HMM initialized using the parameters of the example") {
            val transitionProbability = stateTransitionTable<String, String> {

                "s" resultsIn ("s" withProbabilityOf 0.3)
                "s" resultsIn ("t" withProbabilityOf 0.7)

                "t" resultsIn ("s" withProbabilityOf 0.1)
                "t" resultsIn ("t" withProbabilityOf 0.9)

            }

            val emissionProbability = stateTransitionTable<String, String> {

                "s" resultsIn ("A" withProbabilityOf 0.4)
                "s" resultsIn ("B" withProbabilityOf 0.6)

                "t" resultsIn ("A" withProbabilityOf 0.5)
                "t" resultsIn ("B" withProbabilityOf 0.5)

            }

            val startingProbability = listOf("s" withProbabilityOf 0.85, "t" withProbabilityOf 0.15)

            var hmm = HiddenMarkovModel(initialStateProbabilities = startingProbability,
                    stateTransitions = transitionProbability,
                    observationProbabilities = emissionProbability)


            When("we observe 10 times ABBA and 20 times BAB") {

                Then("then the HMM parameters are estimated like in the example") {
                    val trainingObservations = mutableListOf<List<String>>()
                    for (count in 1..10) {
                        trainingObservations.add(listOf("A", "B", "B", "A"))
                    }

                    for (count in 1..20) {
                        trainingObservations.add(listOf("B", "A", "B"))
                    }
                    println("0 ===")
                    println(hmm)
                    var currentLikelihood = hmm.totalLogLikelyHood(trainingObservations)
                    println("likelihood: $currentLikelihood")
                    println()
                    for (count in 1..1000) {
                        hmm = hmm.trainOneStepUsingSimpleBaumWelch(trainingObservations)
                        println("$count ===")
                        //println(hmm)
                        val newLikelihood = hmm.totalLogLikelyHood(trainingObservations)
                        println("likelihood: $newLikelihood")
                        println("diff: ${currentLikelihood - newLikelihood}")
                        currentLikelihood = newLikelihood
                    }
                    println(hmm)
                    val random = Random()
                    for (count in 1..10) {
                        println(hmm.generateObservationSequenceAccordingToModel(random, 4))
                    }
                }

            }
        }


        // The following examples are described in:
        // https://en.wikipedia.org/wiki/Baum–Welch_algorithm
        Given("a HMM initialized using the parameters of the example") {
            val transitionProbability = stateTransitionTable<String, String> {

                "State 1" resultsIn ("State 1" withProbabilityOf 0.5)
                "State 1" resultsIn ("State 2" withProbabilityOf 0.5)

                "State 2" resultsIn ("State 1" withProbabilityOf 0.3)
                "State 2" resultsIn ("State 2" withProbabilityOf 0.7)

            }

            val emissionProbability = stateTransitionTable<String, String> {

                "State 1" resultsIn ("No Eggs" withProbabilityOf 0.3)
                "State 1" resultsIn ("Eggs" withProbabilityOf 0.7)

                "State 2" resultsIn ("No Eggs" withProbabilityOf 0.8)
                "State 2" resultsIn ("Eggs" withProbabilityOf 0.2)

            }

            val startingProbability = listOf("State 1" withProbabilityOf 0.2, "State 2" withProbabilityOf 0.8)

            var hmm = HiddenMarkovModel(initialStateProbabilities = startingProbability,
                    stateTransitions = transitionProbability,
                    observationProbabilities = emissionProbability)


            When("we observe NN, NN, NN, NN, NE, EE, EN, NN and NN") {

                Then("the new emission matrix should match the result in the example") {
                    val trainingObservations = listOf(
                            listOf("No Eggs", "No Eggs"),
                            listOf("No Eggs", "No Eggs"),
                            listOf("No Eggs", "No Eggs"),
                            listOf("No Eggs", "No Eggs"),
                            listOf("No Eggs", "Eggs"),
                            listOf("Eggs", "Eggs"),
                            listOf("Eggs", "No Eggs"),
                            listOf("No Eggs", "No Eggs"),
                            listOf("No Eggs", "No Eggs")
                    )
                    println("0 ===")
                    println(hmm)
                    var currentLikelihood = hmm.totalLogLikelyHood(trainingObservations)
                    println("likelihood: $currentLikelihood")
                    println()
                    for (count in 1..1000) {
                        hmm = hmm.trainOneStepUsingSimpleBaumWelch(trainingObservations)
                        println("$count ===")
                        //println(hmm)
                        val newLikelihood = hmm.totalLogLikelyHood(trainingObservations)
                        println("likelihood: $newLikelihood")
                        println("diff: ${currentLikelihood - newLikelihood}")
                        currentLikelihood = newLikelihood
                    }
                    println(hmm)

                    val random = Random()
                    for (count in 1..10) {
                        println(hmm.generateObservationSequenceAccordingToModel(random, 2))
                    }
                }

            }
        }


    }
}





