package egger.software.hmm.algorithm

import egger.software.hmm.*

// Baum-Welch algorithm as described in
// http://www.indiana.edu/~iulg/moss/hmmcalculations.pdf

fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.trainOneStepUsingSimpleBaumWelch(observationsList: List<List<TObservation>>): HiddenMarkovModel<TState, TObservation> {

    val newTransitionProbabilities = stateTransitionTable<TState, TState> {}
    for (sourceState in this.states) {
        var overallGammaSum = 0.0
        val gammaSumPerTargetState = mutableMapOf<TState, Double>()
        for (targetState in this.states) {

            var targetStateGammaSum = 0.0
            for (observation in observationsList) {
                for (time in 1 until observation.size) {
                    val prefix = observation.take(time)
                    val suffix = observation.subList(time, observation.size)
                    val gammaValue = gamma(prefix, suffix).given(sourceState) probabilityOf targetState
                    targetStateGammaSum += gammaValue
                }
            }
            gammaSumPerTargetState[targetState] = targetStateGammaSum
            overallGammaSum += targetStateGammaSum
        }

        for (targetState in this.states) {
            newTransitionProbabilities.addTransition(sourceState, targetState withProbabilityOf (gammaSumPerTargetState[targetState]!! / overallGammaSum))
        }
    }

    val newEmissionProbabilities = stateTransitionTable<TState, TObservation> {}

    for (sourceState in this.states) {
        var overallDeltaSum = 0.0
        val deltaSumPerTargetObservation = mutableMapOf<TObservation, Double>()
        for (targetObservation in this.observations) {
            // probability of seeing targetObservation in state sourceState
            var targetObservationDeltaSum = 0.0
            for (observation in observationsList) {
                for (time in 1..observation.size) {
                    val prefix = observation.take(time)
                    if (prefix.last() != targetObservation) {
                        continue
                    }
                    val suffix = observation.subList(time, observation.size)
                    val deltaValue = delta(prefix, suffix)[sourceState]!!
                    targetObservationDeltaSum += deltaValue
                }
            }
            deltaSumPerTargetObservation[targetObservation] = targetObservationDeltaSum
            overallDeltaSum += targetObservationDeltaSum
        }

        for (targetObservation in this.observations) {
            newEmissionProbabilities.addTransition(sourceState, targetObservation withProbabilityOf (deltaSumPerTargetObservation[targetObservation]!! / overallDeltaSum))
        }
    }

    val newInitialProbabilities = mutableListOf<StateWithProbability<TState>>()
    var overallDeltaSum = 0.0
    val deltaSumPerTargetState = mutableMapOf<TState, Double>()
    for (targetState in this.states) {
        var targetStateDeltaSum = 0.0
        for (observation in observationsList) {
            val delta = this.delta(observation.take(1), observation.subList(1, observation.size))
            targetStateDeltaSum += delta[targetState]!!
        }
        deltaSumPerTargetState[targetState] = targetStateDeltaSum
        overallDeltaSum += targetStateDeltaSum
    }

    for (state in this.states) {
        newInitialProbabilities.add(StateWithProbability(state, deltaSumPerTargetState[state]!! / overallDeltaSum))
    }

    return HiddenMarkovModel(newInitialProbabilities.asNormalized, newTransitionProbabilities.asNormalized, newEmissionProbabilities.asNormalized)
}


fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.beta(): Map<TState, Double> {
    // "backward" algorithm

    var beta = mutableMapOf<TState, Double>()

    // Initialization
    for (state in hiddenMarkovModel.states) {
        beta[state] = 1.0
    }

    for (observation in observations.reversed().drop(1)) {
        val previousBeta = beta
        beta = mutableMapOf()

        for (state in hiddenMarkovModel.states) {
            var targetSum = 0.0
            for (targetState in hiddenMarkovModel.states) {
                targetSum += (hiddenMarkovModel.stateTransitions.given(state) probabilityOf targetState) *
                        (hiddenMarkovModel.observationProbabilities.given(targetState) probabilityOf observation) *
                        previousBeta[targetState]!!
            }
            beta[state] = targetSum
        }

    }
    return beta

}

fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.gamma(prefix: List<TObservation>, suffix: List<TObservation>): StateTransitionTable<TState, TState> {
    // probability that the observed sequence has "state" when seeing prefix
    // followed by suffix

    val alpha = this.observing(prefix).probabilityOfObservedSequenceForEachHiddenState()
    val beta = this.observing(suffix).beta()
    val probabilityOfObservation = this.observing(prefix + suffix).probabilityOfObservationSequence()
    val gamma = StateTransitionTable<TState, TState>()

    for (sourceState in this.states) {
        for (targetState in this.states) {
            val gammaValue = (alpha[sourceState]!! *
                    (this.stateTransitions.given(sourceState) probabilityOf targetState) *
                    (this.observationProbabilities.given(targetState) probabilityOf suffix.first()) *
                    beta[targetState]!!) / probabilityOfObservation

            gamma.addTransition(sourceState, targetState withProbabilityOf gammaValue)
        }
    }

    return gamma

}

fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.delta(prefix: List<TObservation>, suffix: List<TObservation>): MutableMap<TState, Double> {

    if (suffix.isNotEmpty()) {

        val gamma = this.gamma(prefix, suffix)
        val delta = mutableMapOf<TState, Double>()

        for (sourceState in this.states) {
            var deltaValue = 0.0
            for (targetState in this.states) {
                deltaValue += gamma.given(sourceState) probabilityOf targetState
            }
            delta[sourceState] = deltaValue
        }

        return delta

    } else {

        val delta = mutableMapOf<TState, Double>()

        val alpha = this.observing(prefix).probabilityOfObservedSequenceForEachHiddenState()
        val probabilityOfObservedSequence = this.observing(prefix).probabilityOfObservationSequence()

        for (sourceState in this.states) {
            delta[sourceState] = alpha[sourceState]!! / probabilityOfObservedSequence
        }

        return delta

    }
}


