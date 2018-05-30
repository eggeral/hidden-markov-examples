package egger.software.hmm.algorithm

import egger.software.hmm.*


// Baum-Welch algorithm as described in
// https://www.ece.ucsb.edu/Faculty/Rabiner/ece259/Reprints/tutorial%20on%20hmm%20and%20applications.pdf
// and
// https://people.eecs.berkeley.edu/~stephentu/writeups/hmm-baum-welch-derivation.pdf
fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.trainOneStepUsingBaumWelch(observationsList: List<List<TObservation>>): HiddenMarkovModel<TState, TObservation> {

    val newTransitionProbabilities = stateTransitionTable<TState, TState> {}
    val newEmissionProbabilities = stateTransitionTable<TState, TObservation> {}
    val newInitialProbabilities = mutableListOf<StateWithProbability<TState>>()

    val expectedNumberOfTimesInStateAtTheBeginning = mutableMapOf<TState, Double>().initUsing(states, 0.0)
    val expectedNumberOfTransitionsFromStateToState = mutableMapOf<TState, MutableMap<TState, Double>>()
            .apply { states.forEach { state -> set(state, mutableMapOf<TState, Double>().initUsing(states, 0.0)) } }
    val expectedTotalNumberOfTransitionsAwayFromState = mutableMapOf<TState, Double>().initUsing(states, 0.0)
    val expectedNumberOfTimesInState = mutableMapOf<TState, Double>().initUsing(states, 0.0)
    val expectedNumberOfTimesInStateAndObserving = mutableMapOf<TState, MutableMap<TObservation, Double>>()
            .apply { states.forEach { state -> set(state, mutableMapOf<TObservation, Double>().initUsing(observations, 0.0)) } }

    for (observation in observationsList) {

        val forwardBackwardCalculationResult = this.observing(observation).calculateForwardBackward()

        val probabilityOfBeingInStateAtTimeOne = this.observing(observation).probabilityOfBeingInState(forwardBackwardCalculationResult, 1)
        for (state in this.states) {
            expectedNumberOfTimesInStateAtTheBeginning[state] = expectedNumberOfTimesInStateAtTheBeginning[state]!! + probabilityOfBeingInStateAtTimeOne[state]!!
        }

        for (time in 1 until observation.size) { // note that we go only to time - 1 as the algorithm demands

            val probabilityOfBeingInStateAndTheNextStateIs = this.observing(observation).probabilityOfBeingInStateAndTheNextStateIs(forwardBackwardCalculationResult, time)

            for (sourceState in this.states) {
                for (targetState in this.states) {
                    expectedNumberOfTransitionsFromStateToState[sourceState]!![targetState] = expectedNumberOfTransitionsFromStateToState[sourceState]!![targetState]!! +
                            probabilityOfBeingInStateAndTheNextStateIs.given(sourceState).probabilityOf(targetState)
                }
            }

            val probabilityOfBeingInState = this.observing(observation).probabilityOfBeingInState(forwardBackwardCalculationResult, time)

            for (sourceState in this.states) {
                expectedTotalNumberOfTransitionsAwayFromState[sourceState] = expectedTotalNumberOfTransitionsAwayFromState[sourceState]!! +
                        probabilityOfBeingInState[sourceState]!!
            }

        }

        for (time in 1..observation.size) {
            for (state in this.states) {
                expectedNumberOfTimesInState[state] = expectedNumberOfTimesInState[state]!! + this.observing(observation).probabilityOfBeingInState(forwardBackwardCalculationResult, time)[state]!!
            }
        }

        for (time in 1..observation.size) {
            for (sourceState in this.states) {
                for (targetObservation in this.observations) {
                    if (targetObservation == observation[time - 1]) {
                        expectedNumberOfTimesInStateAndObserving[sourceState]!![targetObservation] = expectedNumberOfTimesInStateAndObserving[sourceState]!![targetObservation]!! +
                                this.observing(observation).probabilityOfBeingInState(forwardBackwardCalculationResult, time)[sourceState]!!
                    }
                }
            }
        }

    }

    for (state in states) {
        newInitialProbabilities.add(state withProbabilityOf (expectedNumberOfTimesInStateAtTheBeginning[state]!! / observationsList.size))
    }

    for (sourceState in this.states) {
        for (targetState in this.states) {
            newTransitionProbabilities.setTransition(sourceState, targetState withProbabilityOf (
                    expectedNumberOfTransitionsFromStateToState[sourceState]!![targetState]!! /
                            expectedTotalNumberOfTransitionsAwayFromState[sourceState]!!))
        }
    }

    for (sourceState in this.states) {
        for (targetObservation in this.observations) {
            newEmissionProbabilities.setTransition(sourceState, targetObservation withProbabilityOf (
                    expectedNumberOfTimesInStateAndObserving[sourceState]!![targetObservation]!! /
                            expectedNumberOfTimesInState[sourceState]!!))
        }
    }


    return HiddenMarkovModel(newInitialProbabilities, newTransitionProbabilities, newEmissionProbabilities)
}

fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.probabilityOfBeingInState(forwardBackwardCalculationResult: ForwardBackwardCalculationResult<TState>, time: Int): Map<TState, Double> {

    val result = mutableMapOf<TState, Double>()
    var totalSum = 0.0
    val alpha = forwardBackwardCalculationResult.forward[time]
    val beta = forwardBackwardCalculationResult.backward[time]

    for (sourceState in this.hiddenMarkovModel.states) {
        val value = alpha[sourceState]!! * beta[sourceState]!!
        result[sourceState] = value
        totalSum += value
    }

    for (sourceState in this.hiddenMarkovModel.states) {
        result[sourceState] = result[sourceState]!! / totalSum
    }

    return result

}

fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.probabilityOfBeingInStateAndTheNextStateIs(forwardBackwardCalculationResult: ForwardBackwardCalculationResult<TState>, time: Int): StateTransitionTable<TState, TState> {

    val tmp = StateTransitionTable<TState, TState>()
    var totalSum = 0.0
    val alpha = forwardBackwardCalculationResult.forward[time]
    val beta = forwardBackwardCalculationResult.backward[time + 1]

    for (sourceState in this.hiddenMarkovModel.states) {
        for (targetState in this.hiddenMarkovModel.states) {


            val value = alpha[sourceState]!! * beta[targetState]!! *
                    this.hiddenMarkovModel.stateTransitions.given(sourceState).probabilityOf(targetState) *
                    this.hiddenMarkovModel.observationProbabilities.given(targetState).probabilityOf(observations[time])
            tmp.setTransition(sourceState, targetState withProbabilityOf (value))
            totalSum += value

        }
    }

    val result = StateTransitionTable<TState, TState>()

    for (sourceState in this.hiddenMarkovModel.states) {
        for (targetState in this.hiddenMarkovModel.states) {

            val tmpValue = tmp.given(sourceState).probabilityOf(targetState)
            result.setTransition(sourceState, targetState.withProbabilityOf(tmpValue / totalSum))

        }
    }

    return result

}

