package egger.software.hmm.algorithm

import egger.software.hmm.HiddenMarkovModelWithObservations
import egger.software.hmm.StateWithProbability
import egger.software.hmm.probabilityOf

val <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.mostLikelyStateSequence: List<TState>
    get() {

        // Simple implementation of the Viterbi Algorithm

        // delta(n,i) = the highest likelihood of any single path ending in state s(i) after n steps
        // psi(n,i) = the best path ending in state s(i) after n steps
        // pi(i) = probability of s(i) being the first state

        // Initialize delta(1,i) and psi(1,i)
        val delta = mutableMapOf<TState, MutableList<Double>>()
        val psi = mutableMapOf<TState, MutableList<TState?>>()

        for (state in hiddenMarkovModel.states) {

            val pi = hiddenMarkovModel.startingProbabilityOf(state)
            val b = hiddenMarkovModel.observationProbabilities.given(state) probabilityOf observations[0]

            delta[state] = mutableListOf(pi * b)
            psi[state] = mutableListOf<TState?>(null)

        }

        fun predecessorWithMaximumLikelihood(iteration: Int, targetState: TState): StateWithProbability<TState> {
            var result: StateWithProbability<TState>? = null
            for (predecessor in hiddenMarkovModel.states) {
                val likelihood = delta[predecessor]!![iteration - 1] * (hiddenMarkovModel.stateTransitions.given(predecessor) probabilityOf targetState)
                if (result == null || likelihood > result.probability)
                    result = StateWithProbability(predecessor, likelihood)
            }
            return result!!
        }

        for (iteration in 1 until observations.size) {
            for (state in hiddenMarkovModel.states) {

                val predecessor = predecessorWithMaximumLikelihood(iteration, state)
                delta[state]!!.add(predecessor.probability * (hiddenMarkovModel.observationProbabilities.given(state) probabilityOf observations[iteration]))
                psi[state]!!.add(predecessor.state)

            }
        }

        val pathEnding = requireNotNull(delta.entries.map { d -> StateWithProbability(d.key, d.value[2]) }.maxBy { s -> s.probability })

        var mostLikelyStateSequence = listOf(pathEnding.state)
        var currentState = pathEnding.state
        for (idx in observations.size - 1 downTo 1) {
            val previousState = psi[currentState]!![idx]!!
            mostLikelyStateSequence = listOf(previousState) + mostLikelyStateSequence
            currentState = previousState
        }

        return mostLikelyStateSequence
    }
