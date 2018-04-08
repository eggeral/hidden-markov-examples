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

    require(statesSortedWithAccumulatedProbability.last().probability == 1.0, { "Probabilities have to sum up to 1.0" })

    for (stateWithProbability in statesSortedWithAccumulatedProbability) {
        if (offset < stateWithProbability.probability)
            return stateWithProbability.state
    }
    throw IllegalStateException("Could not find $offset in $statesSortedWithAccumulatedProbability [$this]")

}

fun <TState> generateStateSequenceAccordingToModel(
        initialStateProbabilities: List<StateWithProbability<TState>>,
        stateTransitionTable: StateTransitionTable<TState, TState>, random: Random, numberOfStates: Int): List<TState> {

    val stateList = mutableListOf<TState>()
    var state = initialStateProbabilities.selectStateAtOffset(random.nextDouble())
    stateList.add(state)

    for (count in 1 until numberOfStates) {
        state = stateTransitionTable.observationsOf(state).selectStateAtOffset(random.nextDouble())
        stateList.add(state)
    }

    return stateList
}
