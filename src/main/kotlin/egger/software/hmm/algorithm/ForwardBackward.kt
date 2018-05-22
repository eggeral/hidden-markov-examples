package egger.software.hmm.algorithm

import egger.software.hmm.*
import kotlin.math.ln


// TODO change posterior type to List<StateWithProbability>
data class ForwardBackwardCalculationResult<TState>(
        val forward: List<Map<TState, Double>>,
        val backward: List<Map<TState, Double>>,
        val posterior: List<Map<TState, Double>>) {
    override fun toString(): String {
        return "ForwardBackwardCalculationResult(\n  forward=$forward,\n  backward=$backward, \n  posterior=$posterior\n)"
    }
}

fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.calculateForwardBackward(normalize: Boolean = true): ForwardBackwardCalculationResult<TState> {

    // forward
    val forward = mutableListOf<Map<TState, Double>>()

    fun Map<TState, Double>.normalizeIfRequested() = if (normalize) this.asNormalized else this

    // initialization
    // time = 0 -> this can not happen but helps when calculating beta and posterior
    var currentForwardColumn = mutableMapOf<TState, Double>()
    for (state in hiddenMarkovModel.states) {
        currentForwardColumn[state] = hiddenMarkovModel.startingProbabilityOf(state)
    }
    forward.add(currentForwardColumn) // already normalized

    // time = 1
    currentForwardColumn = mutableMapOf()
    val firstObservation = observations.first()
    for (state in hiddenMarkovModel.states) {
        val probabilityOfStateAndObservation = hiddenMarkovModel.observationProbabilities.given(state) probabilityOf firstObservation
        currentForwardColumn[state] = hiddenMarkovModel.startingProbabilityOf(state) * probabilityOfStateAndObservation
    }
    forward.add(currentForwardColumn.normalizeIfRequested())


    // iteration
    for (observation in observations.drop(1)) {

        val previousForwardColumn = forward.last()
        currentForwardColumn = mutableMapOf()

        for (state in hiddenMarkovModel.states) {
            val probabilityOfStateAndObservation = hiddenMarkovModel.observationProbabilities.given(state) probabilityOf observation

            var incomingSum = 0.0
            for (incomingState in hiddenMarkovModel.states) {
                incomingSum += (hiddenMarkovModel.stateTransitions.given(incomingState) probabilityOf state) * previousForwardColumn[incomingState]!!
            }
            currentForwardColumn[state] = incomingSum * probabilityOfStateAndObservation
        }

        forward.add(currentForwardColumn.normalizeIfRequested())

    }

    // backward
    val backward = mutableListOf<Map<TState, Double>>()

    // initialization
    var currentBackwardColumn = mutableMapOf<TState, Double>()
    for (state in hiddenMarkovModel.states) {
        currentBackwardColumn[state] = 1.0
    }
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

        backward.add(0, currentBackwardColumn.normalizeIfRequested())
    }

    val posterior = mutableListOf<Map<TState, Double>>()

    for (idx in 0..observations.size) {
        val currentPosteriorColumn = mutableMapOf<TState, Double>()

        for (state in hiddenMarkovModel.states) {
            currentPosteriorColumn[state] = forward[idx][state]!! * backward[idx][state]!!
        }

        posterior.add(currentPosteriorColumn.normalizeIfRequested())
    }

    return ForwardBackwardCalculationResult(forward, backward, posterior)

}

fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.probabilityOfObservedSequenceForEachHiddenState(): Map<TState, Double> {

    // "forward" algorithm (alpha)
    var alpha = mutableMapOf<TState, Double>()

    // Initialization
    for (state in hiddenMarkovModel.states) {
        val pi = hiddenMarkovModel.startingProbabilityOf(state)
        val b = hiddenMarkovModel.observationProbabilities.given(state) probabilityOf observations[0]
        alpha[state] = pi * b
    }


    for (observation in observations.drop(1)) {
        val previousAlpha = alpha
        alpha = mutableMapOf()

        for (state in hiddenMarkovModel.states) {
            val b = hiddenMarkovModel.observationProbabilities.given(state) probabilityOf observation

            var incomingSum = 0.0
            for (incomingState in hiddenMarkovModel.states) {
                incomingSum += (hiddenMarkovModel.stateTransitions.given(incomingState) probabilityOf state) * previousAlpha[incomingState]!!
            }
            alpha[state] = incomingSum * b
        }

    }
    return alpha

}

fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.probabilityOfObservedSequence(): Double = this.probabilityOfObservedSequenceForEachHiddenState().values.sum()

fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.totalLogLikelyHoodOfAllObservationSequences(observationsList: List<List<TObservation>>): Double {

    var result = 0.0
    for (observation in observationsList) {
        result += ln(this.observing(observation).probabilityOfObservedSequence())
    }
    return result

}

fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.totalLikelyHoodOfAllObservationSequences(observationsList: List<List<TObservation>>): Double {

    var result = 1.0
    for (observation in observationsList) {
        result *= this.observing(observation).probabilityOfObservedSequence()
    }
    return result

}