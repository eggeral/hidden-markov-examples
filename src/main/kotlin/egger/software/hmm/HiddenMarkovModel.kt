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
        val probabilityOfObservedSequence = this.observing(observation).probabilityOfObservedSequence()
        result += log10(this.observing(observation).probabilityOfObservedSequence())
    }
    return result
}

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

fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.alpha(): Map<TState, Double> {
    // "forward" algorithm

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

    val alpha = this.observing(prefix).alpha()
    val beta = this.observing(suffix).beta()
    val probabilityOfObservation = this.observing(prefix + suffix).probabilityOfObservedSequence()
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

        val alpha = this.observing(prefix).alpha()
        val probabilityOfObservedSequence = this.observing(prefix).probabilityOfObservedSequence()

        for (sourceState in this.states) {
            delta[sourceState] = alpha[sourceState]!! / probabilityOfObservedSequence
        }

        return delta

    }
}


fun <TState, TObservation> HiddenMarkovModelWithObservations<TState, TObservation>.probabilityOfObservedSequence(): Double = this.alpha().values.sum()

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
