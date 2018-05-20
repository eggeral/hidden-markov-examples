package egger.software.hmm

import kotlin.math.log10


class HiddenMarkovModel<TState, TObservation>(val initialStateProbabilities: List<StateWithProbability<TState>>,
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

    fun logLikelihoodOf(statesAndObservations: List<StateAndObservation<TState, TObservation>>): Double {

        var result = 1.0
        for (statesAndObservation in statesAndObservations) {
            result += log10(observationProbabilities.given(statesAndObservation.state) probabilityOf statesAndObservation.observation)
        }
        return result

    }

    val states get() = stateTransitions.sources

    val observations get() = observationProbabilities.targets

    override fun toString(): String {
        val result = StringBuilder()

        result.appendln("Transition table:")
        result.append(stateTransitions.toString())
        result.appendln("Observation probabilities")
        result.append(observationProbabilities.toString())
        result.appendln("Initial probabilities")
        result.appendln(initialStateProbabilitiesMap.toString())

        return result.toString()
    }

}

data class StateAndObservation<out TState, out TObservation>(val state: TState, val observation: TObservation)
data class HiddenMarkovModelWithObservations<TState, TObservation>(val hiddenMarkovModel: HiddenMarkovModel<TState, TObservation>, val observations: List<TObservation>)
data class HiddenMarkovModelWithObservationsAndStartingProbability<TState, TObservation>(
        val hiddenMarkovModel: HiddenMarkovModel<TState, TObservation>,
        val observations: List<TObservation>, val
        startingProbability: Double)

fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.startingWith(state: TState): HiddenMarkovModelWithObservationsAndStartingProbability<TState, TObservation> =
        HiddenMarkovModelWithObservationsAndStartingProbability(hiddenMarkovModel, observations, hiddenMarkovModel.startingProbabilityOf(state))


fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.observing(vararg observations: TObservation): HiddenMarkovModelWithObservations<TState, TObservation> =
        HiddenMarkovModelWithObservations(this, observations.asList())

fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.observing(observations: List<TObservation>): HiddenMarkovModelWithObservations<TState, TObservation> =
        HiddenMarkovModelWithObservations(this, observations)

