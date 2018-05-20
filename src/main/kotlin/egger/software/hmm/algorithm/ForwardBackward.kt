package egger.software.hmm.algorithm

import egger.software.hmm.HiddenMarkovModel
import egger.software.hmm.HiddenMarkovModelWithObservations
import egger.software.hmm.observing
import egger.software.hmm.probabilityOf
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

fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.probabilityOfObservationSequence(): Double = this.probabilityOfObservedSequenceForEachHiddenState().values.sum()

fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.totalLogLikelyHoodOfAllObservationSequences(observationsList: List<List<TObservation>>): Double {

    var result = 0.0
    for (observation in observationsList) {
        result += ln(this.observing(observation).probabilityOfObservationSequence())
    }
    return result

}