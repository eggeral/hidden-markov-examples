package egger.software.hmm

import kotlin.math.log10

data class StateWithProbability<out TState>(val state: TState, val probability: Double)

class StateTransitionTable<TSourceState, TTargetState> {

    private var sourceToTargets = mutableMapOf<TSourceState, MutableMap<TTargetState, Double>>()

    val sources = sourceToTargets.keys
    val targets = mutableSetOf<TTargetState>()

    val asNormalized: StateTransitionTable<TSourceState, TTargetState>
        get() {

            val normalized = stateTransitionTable<TSourceState, TTargetState> {}

            for (transitions in sourceToTargets) {
                val sum = transitions.value.entries.sumByDouble { it.value }
                transitions.value.entries.forEach {
                    normalized.setTransition(transitions.key, (it.key withProbabilityOf it.value / sum))
                }
            }
            return normalized

        }

    fun setTransition(sourceState: TSourceState, targetWithProbability: StateWithProbability<TTargetState>) {

        var targetsForSource = sourceToTargets[sourceState]
        if (targetsForSource == null) {
            targetsForSource = mutableMapOf()
            sourceToTargets[sourceState] = targetsForSource
        }
        targetsForSource[targetWithProbability.state] = targetWithProbability.probability

        if (!targets.contains(targetWithProbability.state))
            targets.add(targetWithProbability.state)

    }

    infix fun TSourceState.resultsIn(targetWithProbability: StateWithProbability<TTargetState>) = setTransition(this, targetWithProbability)

    fun given(state: TSourceState): Map<TTargetState, Double> = sourceToTargets[state]
            ?: throw IllegalStateException("State: $state not found")

    fun transitionsAwayFrom(state: TSourceState): List<StateWithProbability<TTargetState>> =
            given(state).entries.map { entry -> StateWithProbability(entry.key, entry.value) }


    override fun toString(): String {
        val stringBuilder = StringBuilder()

        for (source in sources) {
            stringBuilder.append("'$source' - [ ")
            stringBuilder.append(sourceToTargets[source]!!.entries.map { target -> "'${target.key}':(${target.value})" }.joinToString())
            stringBuilder.append(" ]\n")
        }

        return stringBuilder.toString()

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StateTransitionTable<*, *>

        if (sourceToTargets != other.sourceToTargets) return false

        return true
    }

    override fun hashCode(): Int {
        return sourceToTargets.hashCode()
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

val <TState> List<StateWithProbability<TState>>.stateWithMaxProbability: TState?
    get() = this.maxBy { stateWithProbability -> stateWithProbability.probability }?.state


fun <TState> StateTransitionTable<TState, TState>.sequenceLogLikelihood(states: List<TState>): Double {
    var result = 1.0
    var previous: TState? = null
    for (state in states) {

        if (previous != null) {
            // Using Markov assumption!!
            result += log10(given(previous) probabilityOf state)
        }

        previous = state

    }
    return result
}

fun <TState> StateTransitionTable<TState, TState>.sequenceLogLikelihood(vararg states: TState): Double = this.sequenceLogLikelihood(states.asList())

fun <TState> StateTransitionTable<TState, TState>.sequenceLikelihood(states: List<TState>): Double {
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

fun <TState> StateTransitionTable<TState, TState>.sequenceLikelihood(vararg states: TState): Double = this.sequenceLikelihood(states.asList())

fun <TState> createStateTransitionTableFrom(stateList: List<TState>): StateTransitionTable<TState, TState> {

    val states = stateList.groupBy { it }.map { entry -> entry.key }
    var previous = stateList.first()

    val stateCounts = mutableMapOf<TState, MutableMap<TState, Int>>()
    for (state in stateList.drop(1)) {

        val source = stateCounts.getOrPut(previous) { mutableMapOf() }
        source.compute(state) { _, count -> if (count == null) 1 else count + 1 }
        previous = state

    }

    return stateTransitionTable {

        for (state in states) {
            val counts = stateCounts[state] ?: continue

            val total = counts.map { entry -> entry.value }.sum().toDouble()
            for (destination in counts) {
                state resultsIn (destination.key withProbabilityOf destination.value.toDouble() / total)
            }
        }

    }

}

fun <TState> combinedState(vararg subStates: TState): CombinedState<TState> = CombinedState(subStates.toList())

data class CombinedState<TState>(val subStates: List<TState>)

fun <TState> List<TState>.asCombinedStates(size: Int): List<CombinedState<TState>> {

    val queue = FixedCapacityQueue<TState>(size)
    val result = mutableListOf<CombinedState<TState>>()

    for (state in this) {
        queue.add(state)
        if (queue.size >= size) {
            result.add(CombinedState(queue.iterator().asSequence().toList()))
        }
    }

    return result

}
