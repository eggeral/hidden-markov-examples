package egger.software.hmm.example

import egger.software.hmm.*
import egger.software.hmm.algorithm.mostLikelyStateSequence
import egger.software.hmm.state.Caretaker
import egger.software.hmm.state.Caretaker.NoUmbrella
import egger.software.hmm.state.Caretaker.Umbrella
import egger.software.hmm.state.Weather
import egger.software.hmm.state.Weather.*
import io.kotlintest.matchers.plusOrMinus
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.BehaviorSpec

// The following examples are described in:
// https://www.spsc.tugraz.at/system/files/hmm.pdf

class ViterbiExample : BehaviorSpec() {

    init {
        Given("the weather probabilities table and umbrella probabilities") {
            val weatherTable = stateTransitionTable<Weather, Weather> {

                Sunny resultsIn (Sunny withProbabilityOf 0.8)
                Sunny resultsIn (Rainy withProbabilityOf 0.05)
                Sunny resultsIn (Foggy withProbabilityOf 0.15)

                Rainy resultsIn (Sunny withProbabilityOf 0.1)
                Rainy resultsIn (Rainy withProbabilityOf 0.6)
                Rainy resultsIn (Foggy withProbabilityOf 0.2)

                Foggy resultsIn (Sunny withProbabilityOf 0.2)
                Foggy resultsIn (Rainy withProbabilityOf 0.3)
                Foggy resultsIn (Foggy withProbabilityOf 0.5)

            }

            val caretakerTable = stateTransitionTable<Weather, Caretaker> {
                Sunny resultsIn (Umbrella withProbabilityOf 0.1)
                Sunny resultsIn (NoUmbrella withProbabilityOf 0.9)

                Rainy resultsIn (Umbrella withProbabilityOf 0.8)
                Rainy resultsIn (NoUmbrella withProbabilityOf 0.2)

                Foggy resultsIn (Umbrella withProbabilityOf 0.3)
                Foggy resultsIn (NoUmbrella withProbabilityOf 0.7)
            }

            When("we don't know the weather on the first day and the sequence of observations is 'no umbrella' 'umbrella 'umbrella'") {

                Then("the most probable sequence is Foggy, Rainy, Rainy") {

                    val hmm = HiddenMarkovModel(
                            initialStateProbabilities = listOf(Sunny withProbabilityOf 1.0 / 3.0, Rainy withProbabilityOf 1.0 / 3.0, Foggy withProbabilityOf 1.0 / 3.0),
                            stateTransitions = weatherTable,
                            observationProbabilities = caretakerTable).observing(NoUmbrella, Umbrella, Umbrella)

                    // delta(n,i) = the highest likelihood of any single path ending in state s(i) after n steps
                    // psi(n,i) = the best path ending in state s(i) after n steps
                    // pi(i) = probability of s(i) being the first state

                    // Initialize delta(1,i) and psi(1,i)
                    val delta = mutableMapOf<Weather, MutableList<Double>>()
                    val psi = mutableMapOf<Weather, MutableList<Weather?>>()

                    for (weather in hmm.hiddenMarkovModel.states) {

                        val pi = hmm.hiddenMarkovModel.startingProbabilityOf(weather)
                        val b = hmm.hiddenMarkovModel.observationProbabilities.given(weather) probabilityOf hmm.observations[0]

                        delta[weather] = mutableListOf(pi * b)
                        psi[weather] = mutableListOf<Weather?>(null)
                    }

                    delta[Sunny] shouldBe listOf(1.0 / 3.0 * 0.9)
                    delta[Rainy] shouldBe listOf(1.0 / 3.0 * 0.2)
                    delta[Foggy] shouldBe listOf(1.0 / 3.0 * 0.7)

                    psi[Sunny] shouldBe listOf(null)
                    psi[Rainy] shouldBe listOf(null)
                    psi[Foggy] shouldBe listOf(null)

                    // Iteration step 1

                    // likelihood of getting to Sunny from all predecessors

                    fun predecessorWithMaximumLikelihood(iteration: Int, targetState: Weather): StateWithProbability<Weather> {
                        var result: StateWithProbability<Weather>? = null
                        for (predecessor in hmm.hiddenMarkovModel.states) {
                            val likelihood = delta[predecessor]!![iteration - 1] * (hmm.hiddenMarkovModel.stateTransitions.given(predecessor) probabilityOf targetState)
                            if (result == null || likelihood > result.probability)
                                result = StateWithProbability(predecessor, likelihood)
                        }
                        return result!!
                    }


                    val sunnyPredecessor = predecessorWithMaximumLikelihood(1, Sunny)
                    delta[Sunny]!!.add(sunnyPredecessor.probability * (hmm.hiddenMarkovModel.observationProbabilities.given(Sunny) probabilityOf hmm.observations[1]))
                    psi[Sunny]!!.add(sunnyPredecessor.state)

                    delta[Sunny]!![1] shouldBe 0.024.plusOrMinus(10E-9)
                    psi[Sunny]!![1] shouldBe Sunny

                    // likelihood of getting to Rainy from all predecessors

                    val rainyPredecessor = predecessorWithMaximumLikelihood(1, Rainy)
                    delta[Rainy]!!.add(rainyPredecessor.probability * (hmm.hiddenMarkovModel.observationProbabilities.given(Rainy) probabilityOf hmm.observations[1]))
                    psi[Rainy]!!.add(rainyPredecessor.state)

                    delta[Rainy]!![1] shouldBe 0.056.plusOrMinus(10E-9)
                    psi[Rainy]!![1] shouldBe Foggy

                    // likelihood of getting to Foggy from all predecessors

                    val foggyPredecessor = predecessorWithMaximumLikelihood(1, Foggy)
                    delta[Foggy]!!.add(foggyPredecessor.probability * (hmm.hiddenMarkovModel.observationProbabilities.given(Foggy) probabilityOf hmm.observations[1]))
                    psi[Foggy]!!.add(foggyPredecessor.state)

                    delta[Foggy]!![1] shouldBe 0.035.plusOrMinus(10E-9)
                    psi[Foggy]!![1] shouldBe Foggy

                    // Iteration step 2

                    val iteration = 2
                    for (state in hmm.hiddenMarkovModel.states) {

                        val predecessor = predecessorWithMaximumLikelihood(iteration, state)
                        delta[state]!!.add(predecessor.probability * (hmm.hiddenMarkovModel.observationProbabilities.given(state) probabilityOf hmm.observations[iteration]))
                        psi[state]!!.add(predecessor.state)

                    }

                    delta[Sunny]!![2] shouldBe 0.00192.plusOrMinus(10E-9)
                    psi[Sunny]!![2] shouldBe Sunny

                    delta[Rainy]!![2] shouldBe 0.02688.plusOrMinus(10E-9)
                    psi[Rainy]!![2] shouldBe Rainy

                    delta[Foggy]!![2] shouldBe 0.00525.plusOrMinus(10E-9)
                    psi[Foggy]!![2] shouldBe Foggy

                    // Termination

                    // find the most likely path ending by looking at the final state

                    val pathEnding = requireNotNull(delta.entries.map { d -> StateWithProbability(d.key, d.value[2]) }.maxBy { s -> s.probability })
                    pathEnding.probability shouldBe 0.02688.plusOrMinus(10E-9)
                    pathEnding.state shouldBe Rainy

                    // Backtracking

                    var mostLikelyStateSequence = listOf<Weather>(pathEnding.state)
                    var currentState = pathEnding.state
                    for (idx in 2 downTo 1) {
                        val previousState = psi[currentState]!![idx]!!
                        mostLikelyStateSequence = listOf(previousState) + mostLikelyStateSequence
                        currentState = previousState
                    }

                    mostLikelyStateSequence shouldBe listOf(Foggy, Rainy, Rainy)

                    // Alternative
                    HiddenMarkovModel(
                            initialStateProbabilities = listOf(Sunny withProbabilityOf 1.0 / 3.0, Rainy withProbabilityOf 1.0 / 3.0, Foggy withProbabilityOf 1.0 / 3.0),
                            stateTransitions = weatherTable,
                            observationProbabilities = caretakerTable)
                            .observing(NoUmbrella, Umbrella, Umbrella)
                            .mostLikelyStateSequence shouldBe listOf(Foggy, Rainy, Rainy)

                }
            }
        }
    }
}




