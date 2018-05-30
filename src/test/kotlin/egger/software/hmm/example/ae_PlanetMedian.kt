package egger.software.hmm.example

import egger.software.hmm.*
import egger.software.hmm.state.Tile
import egger.software.hmm.state.Tile.*
import egger.software.test.greaterThan
import egger.software.test.plusOrMinus
import egger.software.test.shouldBe
import java.util.*
import kotlin.test.Test

// See:
// https://www.eecis.udel.edu/~lliao/cis841s06/hmmtutorialpart1.pdf
// https://www.eecis.udel.edu/~lliao/cis841s06/hmmtutorialpart2.pdf

class PlanetMedianExample {

    @Test
    fun `a Markov Model can be estimated from a sequence of states created by that model`() {
        // given
        // the Markov model of a Province
        val pavingModel = stateTransitionTable<Tile, Tile> {

            Red resultsIn (Red withProbabilityOf 0.25)
            Red resultsIn (Green withProbabilityOf 0.5)
            Red resultsIn (Blue withProbabilityOf 0.25)

            Green resultsIn (Red withProbabilityOf 0.3)
            Green resultsIn (Green withProbabilityOf 0.4)
            Green resultsIn (Blue withProbabilityOf 0.3)

            Blue resultsIn (Red withProbabilityOf 0.3)
            Blue resultsIn (Green withProbabilityOf 0.5)
            Blue resultsIn (Blue withProbabilityOf 0.2)

        }

        val initialStateProbabilities = listOf(Red withProbabilityOf 0.2, Green withProbabilityOf 0.6, Blue withProbabilityOf 0.2)
        val random = Random(1234L) // fixed seed in order to create reproducible results

        // when
        // we generate a sequence of Tiles using this model
        val stateList = generateStateSequenceAccordingToModel(initialStateProbabilities, pavingModel, random, 35200)

        // then
        // the original model can be estimated from the generated sequence

        // estimate the initial probabilities

        val numberOfStates = stateList.count().toDouble()
        val estimatedInitialStateProbabilities = stateList
                .groupBy { it }
                .map { entry -> StateWithProbability(entry.key, entry.value.size.toDouble() / numberOfStates) }

        // Not that the results are different from the manual set initial properties
        // This is because all states are taken into account not only the first one
        // Having only one state list this might be to only choice.
        // Nevertheless I think this is wrong in the original example.
        estimatedInitialStateProbabilities[0].state shouldBe Green
        estimatedInitialStateProbabilities[0].probability shouldBe 0.45.plusOrMinus(0.01)
        estimatedInitialStateProbabilities[1].state shouldBe Blue
        estimatedInitialStateProbabilities[1].probability shouldBe 0.26.plusOrMinus(0.01)
        estimatedInitialStateProbabilities[2].state shouldBe Red
        estimatedInitialStateProbabilities[2].probability shouldBe 0.28.plusOrMinus(0.01)

        val estimatedModel = createStateTransitionTableFrom(stateList)

        estimatedModel.given(Red) probabilityOf (Red) shouldBe (0.25.plusOrMinus(0.01))
        estimatedModel.given(Red) probabilityOf (Green) shouldBe (0.5.plusOrMinus(0.01))
        estimatedModel.given(Red) probabilityOf (Blue) shouldBe (0.25.plusOrMinus(0.01))

        estimatedModel.given(Green) probabilityOf (Red) shouldBe (0.3.plusOrMinus(0.01))
        estimatedModel.given(Green) probabilityOf (Green) shouldBe (0.4.plusOrMinus(0.01))
        estimatedModel.given(Green) probabilityOf (Blue) shouldBe (0.3.plusOrMinus(0.01))

        estimatedModel.given(Blue) probabilityOf (Red) shouldBe (0.3.plusOrMinus(0.01))
        estimatedModel.given(Blue) probabilityOf (Green) shouldBe (0.5.plusOrMinus(0.01))
        estimatedModel.given(Blue) probabilityOf (Blue) shouldBe (0.2.plusOrMinus(0.01))

    }

    @Test
    fun `the Markov Model which most likely produced a sequence a states can be calculated`() {

        // given
        // tile sequences of all four provinces
        val northTrueModel = stateTransitionTable<Tile, Tile> {

            Red resultsIn (Red withProbabilityOf 0.25)
            Red resultsIn (Green withProbabilityOf 0.5)
            Red resultsIn (Blue withProbabilityOf 0.25)

            Green resultsIn (Red withProbabilityOf 0.3)
            Green resultsIn (Green withProbabilityOf 0.4)
            Green resultsIn (Blue withProbabilityOf 0.3)

            Blue resultsIn (Red withProbabilityOf 0.3)
            Blue resultsIn (Green withProbabilityOf 0.5)
            Blue resultsIn (Blue withProbabilityOf 0.2)

        }
        val northInitialProbabilities = listOf(Red withProbabilityOf 0.2, Green withProbabilityOf 0.6, Blue withProbabilityOf 0.2)

        val eastTrueModel = stateTransitionTable<Tile, Tile> {

            Red resultsIn (Red withProbabilityOf 0.4)
            Red resultsIn (Green withProbabilityOf 0.3)
            Red resultsIn (Blue withProbabilityOf 0.3)

            Green resultsIn (Red withProbabilityOf 0.5)
            Green resultsIn (Green withProbabilityOf 0.25)
            Green resultsIn (Blue withProbabilityOf 0.25)

            Blue resultsIn (Red withProbabilityOf 0.4)
            Blue resultsIn (Green withProbabilityOf 0.3)
            Blue resultsIn (Blue withProbabilityOf 0.3)

        }
        val eastInitialProbabilities = listOf(Red withProbabilityOf 0.5, Green withProbabilityOf 0.25, Blue withProbabilityOf 0.25)

        val southTrueModel = stateTransitionTable<Tile, Tile> {

            Red resultsIn (Red withProbabilityOf 0.2)
            Red resultsIn (Green withProbabilityOf 0.3)
            Red resultsIn (Blue withProbabilityOf 0.5)

            Green resultsIn (Red withProbabilityOf 0.3)
            Green resultsIn (Green withProbabilityOf 0.3)
            Green resultsIn (Blue withProbabilityOf 0.4)

            Blue resultsIn (Red withProbabilityOf 0.2)
            Blue resultsIn (Green withProbabilityOf 0.2)
            Blue resultsIn (Blue withProbabilityOf 0.6)

        }
        val southInitialProbabilities = listOf(Red withProbabilityOf 0.2, Green withProbabilityOf 0.2, Blue withProbabilityOf 0.6)

        val westTrueModel = stateTransitionTable<Tile, Tile> {

            Red resultsIn (Red withProbabilityOf 0.3)
            Red resultsIn (Green withProbabilityOf 0.3)
            Red resultsIn (Blue withProbabilityOf 0.4)

            Green resultsIn (Red withProbabilityOf 0.3)
            Green resultsIn (Green withProbabilityOf 0.4)
            Green resultsIn (Blue withProbabilityOf 0.3)

            Blue resultsIn (Red withProbabilityOf 0.4)
            Blue resultsIn (Green withProbabilityOf 0.3)
            Blue resultsIn (Blue withProbabilityOf 0.3)

        }
        val westInitialProbabilities = listOf(Red withProbabilityOf 0.3, Green withProbabilityOf 0.3, Blue withProbabilityOf 0.4)

        val random = Random(4321L) // fixed seed in order to create reproducible results

        // when
        // we estimate the models of the provinces

        val northEstimatedModel = createStateTransitionTableFrom(generateStateSequenceAccordingToModel(northInitialProbabilities, northTrueModel, random, 35200))
        val eastEstimatedModel = createStateTransitionTableFrom(generateStateSequenceAccordingToModel(eastInitialProbabilities, eastTrueModel, random, 35200))
        val southEstimatedModel = createStateTransitionTableFrom(generateStateSequenceAccordingToModel(southInitialProbabilities, southTrueModel, random, 35200))
        val westEstimatedModel = createStateTransitionTableFrom(generateStateSequenceAccordingToModel(westInitialProbabilities, westTrueModel, random, 35200))

        // then
        // the estimated north model produces the highest probability for a sequence created by the north province
        val north500ElementTestSequence = generateStateSequenceAccordingToModel(northInitialProbabilities, northTrueModel, random, 500)

        val northSequenceProbability = northEstimatedModel.sequenceLogLikelihood(north500ElementTestSequence)
        val eastSequenceProbability = eastEstimatedModel.sequenceLogLikelihood(north500ElementTestSequence)
        val southSequenceProbability = southEstimatedModel.sequenceLogLikelihood(north500ElementTestSequence)
        val westSequenceProbability = westEstimatedModel.sequenceLogLikelihood(north500ElementTestSequence)

        northSequenceProbability shouldBe greaterThan(eastSequenceProbability)
        northSequenceProbability shouldBe greaterThan(southSequenceProbability)
        northSequenceProbability shouldBe greaterThan(westSequenceProbability)

    }
}


