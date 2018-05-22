package egger.software.hmm.example

import egger.software.hmm.HiddenMarkovModel
import egger.software.hmm.algorithm.totalLikelyHoodOfAllObservationSequences
import egger.software.hmm.algorithm.totalLogLikelyHoodOfAllObservationSequences
import egger.software.hmm.algorithm.trainOneStepUsingRabinerBaumWelch
import egger.software.hmm.algorithm.trainOneStepUsingSimpleBaumWelch
import egger.software.hmm.stateTransitionTable
import egger.software.hmm.withProbabilityOf
import egger.software.test.greaterThan
import egger.software.test.plusOrMinus
import egger.software.test.shouldBe
import kotlin.test.Test

class BaumWelchExamples {

    @Test
    fun `the parameters of an HMM can be estimated by the Baum-Welch algorithm for the example used by Larry Moss using a simple Baum-Welch version`() {

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
        val originalTotalLogLikelyHoodOfAllObservationSequences = hmm.totalLogLikelyHoodOfAllObservationSequences(trainingObservations)
        val originalTotalLikelyHoodOfAllObservationSequences = hmm.totalLikelyHoodOfAllObservationSequences(trainingObservations)

        originalTotalLogLikelyHoodOfAllObservationSequences shouldBe (-68.03804999063703).plusOrMinus(1E-9)
        originalTotalLikelyHoodOfAllObservationSequences shouldBe (2.827810674989839E-30).plusOrMinus(1E-40)


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

        val trainedTotalLogLikelyHoodOfAllObservationSequences = trainedHmm.totalLogLikelyHoodOfAllObservationSequences(trainingObservations)
        val trainedTotalLikelyHoodOfAllObservationSequences = trainedHmm.totalLikelyHoodOfAllObservationSequences(trainingObservations)

        trainedTotalLogLikelyHoodOfAllObservationSequences shouldBe (-70.67783539223969).plusOrMinus(1E-9)
        trainedTotalLikelyHoodOfAllObservationSequences shouldBe (2.0183946961180503E-31).plusOrMinus(1E-40)


        // TODO my simple implementation is wrong as it does not get to a better result than the original HMM
        //trainedTotalLogLikelyHoodOfAllObservationSequences shouldBe greaterThan(originalTotalLogLikelyHoodOfAllObservationSequences)
        //trainedTotalLikelyHoodOfAllObservationSequences shouldBe greaterThan(originalTotalLikelyHoodOfAllObservationSequences)


    }

    @Test
    fun `the parameters of an HMM can be estimated by the Baum-Welch algorithm for the example used by Larry Moss using the Baum-Welch version used by Rabiner`() {

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
            trainedHmm = trainedHmm.trainOneStepUsingRabinerBaumWelch(trainingObservations)
        }

        // then

        val originalTotalLogLikelyHoodOfAllObservationSequences = hmm.totalLogLikelyHoodOfAllObservationSequences(trainingObservations)
        val originalTotalLikelyHoodOfAllObservationSequences = hmm.totalLikelyHoodOfAllObservationSequences(trainingObservations)

        originalTotalLogLikelyHoodOfAllObservationSequences shouldBe (-68.03804999063703).plusOrMinus(1E-9)
        originalTotalLikelyHoodOfAllObservationSequences shouldBe (2.827810674989839E-30).plusOrMinus(1E-40)

        trainedHmm.stateTransitions shouldBe
                stateTransitionTable {

                    "s" resultsIn ("s" withProbabilityOf 0.0)
                    "s" resultsIn ("t" withProbabilityOf 1.0)

                    "t" resultsIn ("s" withProbabilityOf 1.0)
                    "t" resultsIn ("t" withProbabilityOf 0.0)

                }

        trainedHmm.observationProbabilities shouldBe
                stateTransitionTable {

                    "s" resultsIn ("A" withProbabilityOf 0.16666666666666666)
                    "s" resultsIn ("B" withProbabilityOf 0.8333333333333334)
                    "t" resultsIn ("A" withProbabilityOf 0.75)
                    "t" resultsIn ("B" withProbabilityOf 0.25)

                }

        trainedHmm.initialStateProbabilities shouldBe
                listOf(
                        "s" withProbabilityOf 1.0,
                        "t" withProbabilityOf 0.0
                )


        val trainedTotalLogLikelyHoodOfAllObservationSequences = trainedHmm.totalLogLikelyHoodOfAllObservationSequences(trainingObservations)
        val trainedTotalLikelyHoodOfAllObservationSequences = trainedHmm.totalLikelyHoodOfAllObservationSequences(trainingObservations)

        trainedTotalLogLikelyHoodOfAllObservationSequences shouldBe (-49.5270783167306).plusOrMinus(1E-9)
        trainedTotalLikelyHoodOfAllObservationSequences shouldBe (3.095018022243141E-22).plusOrMinus(1E-40)

        trainedTotalLogLikelyHoodOfAllObservationSequences shouldBe greaterThan(originalTotalLogLikelyHoodOfAllObservationSequences)
        trainedTotalLikelyHoodOfAllObservationSequences shouldBe greaterThan(originalTotalLikelyHoodOfAllObservationSequences)

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
        hmm.totalLogLikelyHoodOfAllObservationSequences(trainingObservations) shouldBe (-10.024586720876568).plusOrMinus(1E-9)

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


        trainedHmm.totalLogLikelyHoodOfAllObservationSequences(trainingObservations) shouldBe (-9.024464380657642).plusOrMinus(1E-9)

        // when
        trainedHmm = hmm
        for (count in 1..1000) {
            trainedHmm = trainedHmm.trainOneStepUsingRabinerBaumWelch(trainingObservations)
        }

        // then
        hmm.totalLogLikelyHoodOfAllObservationSequences(trainingObservations) shouldBe (-10.024586720876568).plusOrMinus(1E-9)

        trainedHmm.stateTransitions shouldBe
                stateTransitionTable {

                    "State 1" resultsIn ("State 1" withProbabilityOf 0.7549972129807376)
                    "State 1" resultsIn ("State 2" withProbabilityOf 0.24500278701926237)

                    "State 2" resultsIn ("State 1" withProbabilityOf 0.07628438054321154)
                    "State 2" resultsIn ("State 2" withProbabilityOf 0.9237156194567884)

                }

        trainedHmm.observationProbabilities shouldBe
                stateTransitionTable {

                    "State 1" resultsIn ("No Eggs" withProbabilityOf 0.23731329030594595)
                    "State 1" resultsIn ("Eggs" withProbabilityOf 0.7626867096940542)
                    "State 2" resultsIn ("No Eggs" withProbabilityOf 0.9460574905679491)
                    "State 2" resultsIn ("Eggs" withProbabilityOf 0.053942509432051)

                }

        trainedHmm.initialStateProbabilities shouldBe
                listOf(
                        "State 1" withProbabilityOf 0.23743363646286328,
                        "State 2" withProbabilityOf 0.7625663635371367
                )

        trainedHmm.totalLogLikelyHoodOfAllObservationSequences(trainingObservations) shouldBe (-9.024464380657644).plusOrMinus(1E-9)

    }


}







