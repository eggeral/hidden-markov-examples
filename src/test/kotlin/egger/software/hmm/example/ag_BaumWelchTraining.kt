package egger.software.hmm.example

import egger.software.hmm.HiddenMarkovModel
import egger.software.hmm.algorithm.trainOneStepUsingSimpleBaumWelch
import egger.software.hmm.algorithm.trainOneStepUsingWikipediaBaumWelch
import egger.software.hmm.stateTransitionTable
import egger.software.hmm.totalLogLikelyHood
import egger.software.hmm.withProbabilityOf
import egger.software.test.plusOrMinus
import egger.software.test.shouldBe
import kotlin.test.Test

class BaumWelchExamples {

    @Test
    fun `the parameters of an HMM can be estimated by the Baum-Welch algorithm for the example used by Larry Moss`() {

        // The following example is taken from
        // http://www.indiana.edu/~iulg/moss/hmmcalculations.pdf

        // given
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

        val hmm = HiddenMarkovModel(initialStateProbabilities = startingProbability,
                stateTransitions = transitionProbability,
                observationProbabilities = emissionProbability)

        val trainingObservations = mutableListOf<List<String>>()
        for (count in 1..10) {
            trainingObservations.add(listOf("A", "B", "B", "A"))
        }

        for (count in 1..20) {
            trainingObservations.add(listOf("B", "A", "B"))
        }

        // when
        var trainedHmm = hmm
        for (count in 1..1000) {
            trainedHmm = trainedHmm.trainOneStepUsingSimpleBaumWelch(trainingObservations)
        }

        // then
        hmm.totalLogLikelyHood(trainingObservations) shouldBe (-68.03804999063703).plusOrMinus(1E-9)
        trainedHmm.stateTransitions shouldBe
                stateTransitionTable {

                    "s" resultsIn ("s" withProbabilityOf 0.08834742594843437)
                    "s" resultsIn ("t" withProbabilityOf 0.9116525740515656)

                    "t" resultsIn ("s" withProbabilityOf 1.2947263732068315E-64)
                    "t" resultsIn ("t" withProbabilityOf 1.0)

                }

        trainedHmm.observationProbabilities shouldBe
                stateTransitionTable {

                    "s" resultsIn ("A" withProbabilityOf 0.5881264753173849)
                    "s" resultsIn ("B" withProbabilityOf 0.41187352468261507)
                    "t" resultsIn ("A" withProbabilityOf 0.3767927690757944)
                    "t" resultsIn ("B" withProbabilityOf 0.6232072309242056)

                }

        trainedHmm.initialStateProbabilities shouldBe
                listOf(
                        "s" withProbabilityOf 0.9999976404557083,
                        "t" withProbabilityOf 2.35954429172607E-6
                )


        trainedHmm.totalLogLikelyHood(trainingObservations) shouldBe (-70.67783539223969).plusOrMinus(1E-9)

        // when
        trainedHmm = hmm
        for (count in 1..1000) {
            trainedHmm = trainedHmm.trainOneStepUsingWikipediaBaumWelch(trainingObservations)
        }

        // then
        hmm.totalLogLikelyHood(trainingObservations) shouldBe (-68.03804999063703).plusOrMinus(1E-9)

        trainedHmm.stateTransitions shouldBe
                stateTransitionTable {

                    "s" resultsIn ("s" withProbabilityOf 0.019426319665748135)
                    "s" resultsIn ("t" withProbabilityOf 0.9805736803342516)

                    "t" resultsIn ("s" withProbabilityOf 0.8921465657537299)
                    "t" resultsIn ("t" withProbabilityOf 0.10785343424627042)

                }

        trainedHmm.observationProbabilities shouldBe
                stateTransitionTable {

                    "s" resultsIn ("A" withProbabilityOf 0.3999999999999997)
                    "s" resultsIn ("B" withProbabilityOf 0.6000000000000004)
                    "t" resultsIn ("A" withProbabilityOf 0.4000000000000001)
                    "t" resultsIn ("B" withProbabilityOf 0.5999999999999995)

                }

        trainedHmm.initialStateProbabilities shouldBe
                listOf(
                        "s" withProbabilityOf 0.47639073033859697,
                        "t" withProbabilityOf 0.5236092696614031
                )

        trainedHmm.totalLogLikelyHood(trainingObservations) shouldBe (-67.3011667009256).plusOrMinus(1E-9)

    }

    @Test
    fun `the parameters of an HMM can be estimated by the Baum-Welch algorithm for the example used on Wikipedia`() {

        // The following example is described in:
        // https://en.wikipedia.org/wiki/Baum–Welch_algorithm

        // given
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

        val hmm = HiddenMarkovModel(initialStateProbabilities = startingProbability,
                stateTransitions = transitionProbability,
                observationProbabilities = emissionProbability)

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

        // when
        var trainedHmm = hmm
        for (count in 1..1000) {
            trainedHmm = trainedHmm.trainOneStepUsingSimpleBaumWelch(trainingObservations)
        }

        // then
        hmm.totalLogLikelyHood(trainingObservations) shouldBe (-10.024586720876568).plusOrMinus(1E-9)

        trainedHmm.stateTransitions shouldBe
                stateTransitionTable {

                    "State 1" resultsIn ("State 1" withProbabilityOf 0.7549972129807374)
                    "State 1" resultsIn ("State 2" withProbabilityOf 0.24500278701926256)

                    "State 2" resultsIn ("State 1" withProbabilityOf 0.07628438054321163)
                    "State 2" resultsIn ("State 2" withProbabilityOf 0.9237156194567884)

                }

        trainedHmm.observationProbabilities shouldBe
                stateTransitionTable {

                    "State 1" resultsIn ("No Eggs" withProbabilityOf 0.23731329030594606)
                    "State 1" resultsIn ("Eggs" withProbabilityOf 0.7626867096940539)

                    "State 2" resultsIn ("No Eggs" withProbabilityOf 0.9460574905679491)
                    "State 2" resultsIn ("Eggs" withProbabilityOf 0.053942509432050874)

                }

        trainedHmm.initialStateProbabilities shouldBe
                listOf(
                        "State 1" withProbabilityOf 0.23743363646286353,
                        "State 2" withProbabilityOf 0.7625663635371365
                )


        trainedHmm.totalLogLikelyHood(trainingObservations) shouldBe (-9.024464380657642).plusOrMinus(1E-9)

        // when
        trainedHmm = hmm
        for (count in 1..1000) {
            trainedHmm = trainedHmm.trainOneStepUsingWikipediaBaumWelch(trainingObservations)
        }

        // then
        hmm.totalLogLikelyHood(trainingObservations) shouldBe (-10.024586720876568).plusOrMinus(1E-9)

        trainedHmm.stateTransitions shouldBe
                stateTransitionTable {

                    "State 1" resultsIn ("State 1" withProbabilityOf 0.5)
                    "State 1" resultsIn ("State 2" withProbabilityOf 0.5)

                    "State 2" resultsIn ("State 1" withProbabilityOf 0.14285714285714285)
                    "State 2" resultsIn ("State 2" withProbabilityOf 0.8571428571428571)

                }

        trainedHmm.observationProbabilities shouldBe
                stateTransitionTable {

                    "State 1" resultsIn ("No Eggs" withProbabilityOf 2.3183814909631144E-102)
                    "State 1" resultsIn ("Eggs" withProbabilityOf 1.0)
                    "State 2" resultsIn ("No Eggs" withProbabilityOf 1.0)
                    "State 2" resultsIn ("Eggs" withProbabilityOf 2.9535782158455884E-193)

                }

        trainedHmm.initialStateProbabilities shouldBe
                listOf(
                        "State 1" withProbabilityOf 0.2222222222222222,
                        "State 2" withProbabilityOf 0.7777777777777778
                )

        trainedHmm.totalLogLikelyHood(trainingObservations) shouldBe (-9.024464380657644).plusOrMinus(1E-9)

    }


}







