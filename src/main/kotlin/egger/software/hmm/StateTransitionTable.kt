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

fun <TState> StateTransitionTable<TState, TState>.sequenceProbability(vararg states: TState): Double {
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