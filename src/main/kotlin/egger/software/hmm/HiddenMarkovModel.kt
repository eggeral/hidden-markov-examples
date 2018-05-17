package egger.software.hmm

import egger.software.hmm.algorithm.alpha
import kotlin.math.ln
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

fun <TState, TObservation> HiddenMarkovModelWithObservationsAndStartingProbability<TState, TObservation>.likelihoodOf(vararg states: TState): Double {

    if (states.size != observations.size) throw IllegalStateException("Number of observations has to match the number of states")
    val statesAndObservations = states.zip(observations).map { pair -> StateAndObservation(pair.first, pair.second) }
    return hiddenMarkovModel.stateTransitions.sequenceLikelihood(*states) * hiddenMarkovModel.likelihoodOf(statesAndObservations) * startingProbability

}

fun <TState, TObservation> HiddenMarkovModelWithObservationsAndStartingProbability<TState, TObservation>.logLikelihoodOf(vararg states: TState): Double {

    if (states.size != observations.size) throw IllegalStateException("Number of observations has to match the number of states")
    val statesAndObservations = states.zip(observations).map { pair -> StateAndObservation(pair.first, pair.second) }
    return hiddenMarkovModel.stateTransitions.sequenceLogLikelihood(*states) + hiddenMarkovModel.logLikelihoodOf(statesAndObservations) + log10(startingProbability)

}

// TODO change posterior type to List<StateWithProbability>
data class ForwardBackwardCalculationResult<TState>(
        val forward: List<Map<TState, Double>>,
        val backward: List<Map<TState, Double>>,
        val posterior: List<Map<TState, Double>>) {
    override fun toString(): String {
        return "ForwardBackwardCalculationResult(\n  forward=$forward,\n  backward=$backward, \n  posterior=$posterior\n)"
    }
}

fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.calculateForwardBackward(): ForwardBackwardCalculationResult<TState> {

    // forward
    val forward = mutableListOf<Map<TState, Double>>()

    // initialization

    var currentForwardColumn = mutableMapOf<TState, Double>()
    for (state in hiddenMarkovModel.states) {
        currentForwardColumn[state] = hiddenMarkovModel.startingProbabilityOf(state)
    }
    forward.add(currentForwardColumn)

    // iteration
    for (observation in observations) {
        val previousForwardColumn = forward.last()
        currentForwardColumn = mutableMapOf()

        for (state in hiddenMarkovModel.states) {
            val b = hiddenMarkovModel.observationProbabilities.given(state) probabilityOf observation

            var incomingSum = 0.0
            for (incomingState in hiddenMarkovModel.states) {
                incomingSum += (hiddenMarkovModel.stateTransitions.given(incomingState) probabilityOf state) * previousForwardColumn[incomingState]!!
            }
            currentForwardColumn[state] = incomingSum * b
        }

        val scale = 1.0 / currentForwardColumn.values.sum()
        forward.add(currentForwardColumn.mapValues { entry -> entry.value * scale })

    }

    // backward
    val backward = mutableListOf<Map<TState, Double>>()

    // initialization
    var currentBackwardColumn = mutableMapOf<TState, Double>()
    for (state in hiddenMarkovModel.states) {
        currentBackwardColumn[state] = 1.0
    }
    // ??? this is not normalized to 1.0 as the other entries. Is this correct?
    backward.add(0, currentBackwardColumn)

    // iteration
    for (observation in observations.asReversed()) {

        val previousBackwardColumn = backward.first()
        currentBackwardColumn = mutableMapOf()

        for (state in hiddenMarkovModel.states) {

            var targetSum = 0.0
            for (targetState in hiddenMarkovModel.states) {
                targetSum += (hiddenMarkovModel.stateTransitions.given(state) probabilityOf targetState) *
                        (hiddenMarkovModel.observationProbabilities.given(targetState) probabilityOf observation) *
                        previousBackwardColumn[targetState]!!
            }
            currentBackwardColumn[state] = targetSum

        }

        val scale = 1.0 / currentBackwardColumn.values.sum()
        backward.add(0, currentBackwardColumn.mapValues { entry -> entry.value * scale })

    }

    val posterior = mutableListOf<Map<TState, Double>>()

    for (idx in 0..observations.size) {
        val currentPosteriorColumn = mutableMapOf<TState, Double>()

        for (state in hiddenMarkovModel.states) {
            currentPosteriorColumn[state] = forward[idx][state]!! * backward[idx][state]!!
        }

        val scale = 1.0 / currentPosteriorColumn.values.sum()
        posterior.add(currentPosteriorColumn.mapValues { entry -> entry.value * scale })
    }

    return ForwardBackwardCalculationResult(forward, backward, posterior)

}

fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.totalLogLikelyHood(observationsList: List<List<TObservation>>): Double {

    var result = 0.0
    for (observation in observationsList) {
        result += ln(this.observing(observation).probabilityOfObservedSequence())
    }
    return result

}

fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.probabilityOfObservedSequence(): Double = this.alpha().values.sum()


fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.observing(vararg observations: TObservation): HiddenMarkovModelWithObservations<TState, TObservation> =
        HiddenMarkovModelWithObservations(this, observations.asList())

fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.observing(observations: List<TObservation>): HiddenMarkovModelWithObservations<TState, TObservation> =
        HiddenMarkovModelWithObservations(this, observations)

