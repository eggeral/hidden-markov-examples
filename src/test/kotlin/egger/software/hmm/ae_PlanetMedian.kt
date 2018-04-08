package egger.software.hmm

import egger.software.hmm.Tile.*
import io.kotlintest.matchers.plusOrMinus
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.BehaviorSpec
import java.util.*

// See:
// https://www.eecis.udel.edu/~lliao/cis841s06/hmmtutorialpart1.pdf
// https://www.eecis.udel.edu/~lliao/cis841s06/hmmtutorialpart2.pdf

class PlanetMedianExample : BehaviorSpec() {

    init {
        Given("the Markov model of a Province") {
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

            When("we generate a sequence of Tiles using this model") {
                val stateList = generateStateSequenceAccordingToModel(initialStateProbabilities, pavingModel, random, 35200)

                Then("the original model can be estimated from the generated sequence") {

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

                    val states = estimatedInitialStateProbabilities.map { it.state }
                    var previous = stateList.first()

                    val stateCounts = mutableMapOf<Tile, MutableMap<Tile, Int>>()
                    for (state in stateList.drop(1)) {

                        val source = stateCounts.getOrPut(previous) { mutableMapOf() }
                        source.compute(state) { _, v -> if (v == null) 1 else v + 1 }
                        previous = state

                    }

                    val estimatedModel = stateTransitionTable<Tile, Tile> {

                        for (state in states) {
                            val counts = requireNotNull(stateCounts[state])
                            val total = counts.map { entry -> entry.value }.sum().toDouble()
                            for (destination in counts) {
                                state resultsIn (destination.key withProbabilityOf destination.value.toDouble() / total)
                            }
                        }

                    }

                    estimatedModel.given(Red) probabilityOf(Red) shouldBe (0.25.plusOrMinus(0.01))
                    estimatedModel.given(Red) probabilityOf(Green) shouldBe (0.5.plusOrMinus(0.01))
                    estimatedModel.given(Red) probabilityOf(Blue) shouldBe (0.25.plusOrMinus(0.01))

                    estimatedModel.given(Green) probabilityOf(Red) shouldBe (0.3.plusOrMinus(0.01))
                    estimatedModel.given(Green) probabilityOf(Green) shouldBe (0.4.plusOrMinus(0.01))
                    estimatedModel.given(Green) probabilityOf(Blue) shouldBe (0.3.plusOrMinus(0.01))

                    estimatedModel.given(Blue) probabilityOf(Red) shouldBe (0.3.plusOrMinus(0.01))
                    estimatedModel.given(Blue) probabilityOf(Green) shouldBe (0.5.plusOrMinus(0.01))
                    estimatedModel.given(Blue) probabilityOf(Blue) shouldBe (0.2.plusOrMinus(0.01))

                }
            }
        }
    }
}

