package egger.software.hmm

data class StateWithProbability<out TState>(val state: TState, val probability: Double)

class StateTransitionTable<TSourceState, TTargetState> {

    private var sourceToTargets = mutableMapOf<TSourceState, MutableMap<TTargetState, Double>>()

    val sources get() = sourceToTargets.keys
    private val targets = mutableListOf<TTargetState>()

    infix fun TSourceState.resultsIn(targetWithProbability: StateWithProbability<TTargetState>) {

        var targetForSource = sourceToTargets[this]
        if (targetForSource == null) {
            targetForSource = mutableMapOf()
            sourceToTargets[this] = targetForSource
        }
        targetForSource[targetWithProbability.state] = targetWithProbability.probability

        targets.add(targetWithProbability.state)

    }

    fun given(state: TSourceState): Map<TTargetState, Double> = sourceToTargets[state]
            ?: throw IllegalStateException("State: $state not found")

    fun observationsOf(state: TSourceState): List<StateWithProbability<TTargetState>> =
            given(state).entries.map { entry -> StateWithProbability(entry.key, entry.value) }

    override fun toString(): String {
        return "$sourceToTargets"
    }

}

fun <TSourceState, TTargetState> stateTransitionTable(init: StateTransitionTable<TSourceState, TTargetState>.() -> Unit): StateTransitionTable<TSourceState, TTargetState> {
    val result = StateTransitionTable<TSourceState, TTargetState>()
    init(result)
    return result
}

infix fun <TState> TState.withProbabilityOf(probability: Double) = StateWithProbability(this, probability)
infix fun <TState> Map<TState, Double>.probabilityOf(state: TState) = this[state]
        ?: throw IllegalStateException("State: $state not found")


fun <TState> StateTransitionTable<TState, TState>.sequenceProbability(states: List<TState>): Double {
    var result = 1.0
    var previous: TState? = null
    for (state in states) {

        if (previous != null) {
            // Using Markov assumption!!
            result *= given(previous) probabilityOf state
        }

        previous = state

    }
    return result
}

fun <TState> StateTransitionTable<TState, TState>.sequenceProbability(vararg states: TState): Double = this.sequenceProbability(*states)

fun <TState> estimateStateTransitionTable(stateList: List<TState>): StateTransitionTable<TState, TState> {

    val states = stateList.groupBy { it }.map { entry -> entry.key }
    var previous = stateList.first()

    val stateCounts = mutableMapOf<TState, MutableMap<TState, Int>>()
    for (state in stateList.drop(1)) {

        val source = stateCounts.getOrPut(previous) { mutableMapOf() }
        source.compute(state) { _, v -> if (v == null) 1 else v + 1 }
        previous = state

    }

    return stateTransitionTable<TState, TState> {

        for (state in states) {
            val counts = requireNotNull(stateCounts[state])
            val total = counts.map { entry -> entry.value }.sum().toDouble()
            for (destination in counts) {
                state resultsIn (destination.key withProbabilityOf destination.value.toDouble() / total)
            }
        }

    }


}