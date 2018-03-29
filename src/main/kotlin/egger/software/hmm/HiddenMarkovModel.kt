package egger.software.hmm


class HiddenMarkovModel<TState, TObservation>(initialStateProbabilities: List<StateWithProbability<TState>>,
                                              val stateTransitions: ProbabilitiesTable<TState, TState>,
                                              val observationProbabilities: ProbabilitiesTable<TState, TObservation>) {

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

    val states get() = stateTransitions.states
    val observations get() = observationProbabilities.observations

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


