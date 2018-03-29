package egger.software.hmm

data class StateWithProbability<out TState>(val state: TState, val probability: Double)

class ProbabilitiesTable<TState, TObservation> {

    private var stateToObservations = mutableMapOf<TState, MutableMap<TObservation, Double>>()

    val states get() = stateToObservations.keys
    val observations = mutableListOf<TObservation>()


    infix fun TState.resultsIn(observationWithProbability: StateWithProbability<TObservation>) {

        var observationsForState = stateToObservations[this]
        if (observationsForState == null) {
            observationsForState = mutableMapOf()
            stateToObservations[this] = observationsForState
        }
        observationsForState[observationWithProbability.state] = observationWithProbability.probability

        observations.add(observationWithProbability.state)

    }

    fun given(state: TState): Map<TObservation, Double> = stateToObservations[state]
            ?: throw IllegalStateException("State: $state not found")


}

fun <TState, TObservation> probabilitiesTable(init: ProbabilitiesTable<TState, TObservation>.() -> Unit): ProbabilitiesTable<TState, TObservation> {
    val result = ProbabilitiesTable<TState, TObservation>()
    init(result)
    return result
}

infix fun <TState> TState.withProbabilityOf(probability: Double) = StateWithProbability(this, probability)
infix fun <TState> Map<TState, Double>.probabilityOf(state: TState) = this[state]
        ?: throw IllegalStateException("State: $state not found")

fun <TState> ProbabilitiesTable<TState, TState>.sequenceProbability(vararg states: TState): Double {
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