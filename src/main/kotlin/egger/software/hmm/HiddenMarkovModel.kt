package egger.software.hmm


class HiddenMarkovModel<TState, TObservation>(initialStateProbabilities: List<StateWithProbability<TState>>,
                                              val stateTransitions: StateTransitionTable<TState, TState>,
                                              val observationProbabilities: StateTransitionTable<TState, TObservation>) {

    private val initialStateProbabilitiesMap = mutableMapOf<TState, Double>()

    init {
        for (stateWithProbability in initialStateProbabilities) {
            initialStateProbabilitiesMap[stateWithProbability.state] = stateWithProbability.probability
        }
    }

    fun startingProbabilityOf(state: TState) = initialStateProbabilitiesMap[state]
            ?: throw IllegalStateException("State $state not found in starting probabilities")

    fun likelihoodOf(statesAndObservations: List<StateAndObservation<TState, TObservation>>): Double {

        var result = 1.0
        for (statesAndObservation in statesAndObservations) {
            result *= observationProbabilities.given(statesAndObservation.state) probabilityOf statesAndObservation.observation
        }
        return result

    }

    val states get() = stateTransitions.sources

}

data class StateAndObservation<out TState, out TObservation>(val state: TState, val observation: TObservation)
data class HiddenMarkovModelWithObservations<TState, TObservation>(val hiddenMarkovModel: HiddenMarkovModel<TState, TObservation>, val observations: List<TObservation>)
data class HiddenMarkovModelWithObservationsAndStartingProbability<TState, TObservation>(
        val hiddenMarkovModel: HiddenMarkovModel<TState, TObservation>,
        val observations: List<TObservation>, val
        startingProbability: Double)

fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.startingWith(state: TState): HiddenMarkovModelWithObservationsAndStartingProbability<TState, TObservation> =
        HiddenMarkovModelWithObservationsAndStartingProbability(hiddenMarkovModel, observations, hiddenMarkovModel.startingProbabilityOf(state))

fun <TState, TObservation> HiddenMarkovModelWithObservationsAndStartingProbability<TState, TObservation>.likelihoodOf(vararg states: TState): Double {

    if (states.size != observations.size) throw IllegalStateException("Number of observations has to match the number of states")
    val statesAndObservations = states.zip(observations).map { pair -> StateAndObservation(pair.first, pair.second) }
    return hiddenMarkovModel.stateTransitions.sequenceProbability(*states) * hiddenMarkovModel.likelihoodOf(statesAndObservations) * startingProbability

}

fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.observing(vararg observations: TObservation): HiddenMarkovModelWithObservations<TState, TObservation> =
        HiddenMarkovModelWithObservations(this, observations.asList())

fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.observing(observations: List<TObservation>): HiddenMarkovModelWithObservations<TState, TObservation> =
        HiddenMarkovModelWithObservations(this, observations)

val <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.mostLikelyStateSequence: List<TState>
    get() {

        // Simple implementation of the Viterbi Algorithm

        // delta(n,i) = the highest likelihood of any single path ending in state s(i) after n steps
        // psi(n,i) = the best path ending in state s(i) after n steps
        // pi(i) = probability of s(i) being the first state

        // Initialize delta(1,i) and psi(1,i)
        val delta = mutableMapOf<TState, MutableList<Double>>()
        val psi = mutableMapOf<TState, MutableList<TState?>>()

        for (weather in hiddenMarkovModel.states) {

            val pi = hiddenMarkovModel.startingProbabilityOf(weather)
            val b = hiddenMarkovModel.observationProbabilities.given(weather) probabilityOf observations[0]

            delta[weather] = mutableListOf(pi * b)
            psi[weather] = mutableListOf<TState?>(null)

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
        for (idx in observations.size-1 downTo 1) {
            val previousState = psi[currentState]!![idx]!!
            mostLikelyStateSequence = listOf(previousState) + mostLikelyStateSequence
            currentState = previousState
        }

        return mostLikelyStateSequence
    }
