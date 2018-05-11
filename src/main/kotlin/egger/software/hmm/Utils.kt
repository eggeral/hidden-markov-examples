package egger.software.hmm

import java.util.*


fun <TState> Iterable<StateWithProbability<TState>>.selectStateAtOffset(offset: Double): TState {
    var sum = 0.0

    require(offset >= 0.0, { "Offset has to be >= 0.0" })
    require(offset < 1.0, { "Offset has to be < 1.0" })

    val statesSortedWithAccumulatedProbability = mutableListOf<StateWithProbability<TState>>()
    this.sortedBy { stateWithProbability -> stateWithProbability.probability }.forEach {
        sum += it.probability
        statesSortedWithAccumulatedProbability.add(StateWithProbability(it.state, sum))
    }

    require(statesSortedWithAccumulatedProbability.last().probability == 1.0, { "Probabilities have to sum up to 1.0 but was ${statesSortedWithAccumulatedProbability.last().probability}" })

    for (stateWithProbability in statesSortedWithAccumulatedProbability) {
        if (offset < stateWithProbability.probability)
            return stateWithProbability.state
    }
    throw IllegalStateException("Could not find $offset in $statesSortedWithAccumulatedProbability [$this]")

}

fun <TState, TObservation> HiddenMarkovModel<TState, TObservation>.generateObservationSequenceAccordingToModel(random: Random, numberOfObservations: Int): List<TObservation> {

    val observationList = mutableListOf<TObservation>()
    var state = this.initialStateProbabilities.selectStateAtOffset(random.nextDouble())

    for (count in 0 until numberOfObservations) {
        observationList.add(this.observationProbabilities.transitionsAwayFrom(state).selectStateAtOffset(random.nextDouble()))
        state = this.stateTransitions.transitionsAwayFrom(state).selectStateAtOffset(random.nextDouble())
    }

    return observationList
}


fun <TState> generateStateSequenceAccordingToModel(
        initialStateProbabilities: List<StateWithProbability<TState>>,
        stateTransitionTable: StateTransitionTable<TState, TState>, random: Random, numberOfStates: Int): List<TState> {

    val stateList = mutableListOf<TState>()
    var state = initialStateProbabilities.selectStateAtOffset(random.nextDouble())
    stateList.add(state)

    for (count in 1 until numberOfStates) {
        state = stateTransitionTable.transitionsAwayFrom(state).selectStateAtOffset(random.nextDouble())
        stateList.add(state)
    }

    return stateList
}

val <TState> List<StateWithProbability<TState>>.asNormalized: List<StateWithProbability<TState>>
    get() {
        val sum = this.sumByDouble { it.probability }
        return this.map { StateWithProbability(it.state, it.probability / sum) }
    }

