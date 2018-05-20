package egger.software.hmm.experiment

import egger.software.hmm.HiddenMarkovModel
import egger.software.hmm.algorithm.mostLikelyStateSequence
import egger.software.hmm.experiment.Coin.Fair
import egger.software.hmm.experiment.Coin.UnFair
import egger.software.hmm.experiment.Toss.Heads
import egger.software.hmm.experiment.Toss.Tails
import egger.software.hmm.observing
import egger.software.hmm.stateTransitionTable
import egger.software.hmm.withProbabilityOf
import java.util.*

data class MasterData(val probabilityOfChangingCoins: Double,
                      val probabilityOfFairHeads: Double,
                      val probabilityOfUnFairHeads: Double)

data class TossRow(val coin: Coin, val toss: Toss)

enum class Coin { Fair, UnFair }
enum class Toss {Heads, Tails}

fun generateRandomTosses(numberOfTosses: Int, masterData: MasterData): List<TossRow> {
    val random = Random()
    val tossRows = mutableListOf<TossRow>()

    fun headsProbability(coin: Coin): Double = when (coin) {
        Fair -> masterData.probabilityOfFairHeads
        else -> masterData.probabilityOfUnFairHeads
    }

    var coin = if (random.nextDouble() > 0.5) Fair else UnFair
    var headsProbability = headsProbability(coin)

    fun changeCoin() {
        coin = if (coin == Fair) UnFair else Fair
        headsProbability = headsProbability(coin)
    }

    for (tossNr in 0 until numberOfTosses) {

        if (random.nextDouble() < masterData.probabilityOfChangingCoins) changeCoin()
        val toss = if (random.nextDouble() < headsProbability) Heads else Tails

        tossRows.add(TossRow(coin, toss))

    }

    return tossRows

}

private val List<TossRow>.tosses: List<Toss>
    get() = this.map { row -> row.toss }

private val List<TossRow>.coins: List<Coin>
    get() = this.map { row -> row.coin }

fun estimateMostLikelyCoinSequenceBasedOnTosses(masterData: MasterData, tosses: List<Toss>): List<Coin> {
    val coinTable = stateTransitionTable<Coin, Coin> {

        Fair resultsIn (UnFair withProbabilityOf masterData.probabilityOfChangingCoins)
        Fair resultsIn (Fair withProbabilityOf 1.0 - masterData.probabilityOfChangingCoins)

        UnFair resultsIn (Fair withProbabilityOf masterData.probabilityOfChangingCoins)
        UnFair resultsIn (UnFair withProbabilityOf 1.0 - masterData.probabilityOfChangingCoins)

    }

    val observationTable = stateTransitionTable<Coin, Toss> {

        Fair resultsIn (Heads withProbabilityOf masterData.probabilityOfFairHeads)
        Fair resultsIn (Tails withProbabilityOf 1.0 - masterData.probabilityOfFairHeads)

        UnFair resultsIn (Heads withProbabilityOf masterData.probabilityOfUnFairHeads)
        UnFair resultsIn (Tails withProbabilityOf 1.0 - masterData.probabilityOfUnFairHeads)

    }

    val hmmObservingTosses = HiddenMarkovModel(
            initialStateProbabilities = listOf(Fair withProbabilityOf 0.5, UnFair withProbabilityOf 0.5),
            stateTransitions = coinTable,
            observationProbabilities = observationTable).observing(tosses)

    return hmmObservingTosses.mostLikelyStateSequence
}

fun main(args: Array<String>) {

    val masterData = MasterData(
            probabilityOfChangingCoins = 0.01,
            probabilityOfFairHeads = 0.2,
            probabilityOfUnFairHeads = 0.8)

    val numberOfTosses = 1000

    val generatedData = generateRandomTosses(numberOfTosses, masterData)
    val estimation = estimateMostLikelyCoinSequenceBasedOnTosses(masterData, generatedData.tosses)

    val hits = estimation.zip(generatedData.coins).fold(0, { acc, pair -> if (pair.first == pair.second) acc + 1 else acc })
    val misses = numberOfTosses - hits
    val missesPercentage = misses.toDouble() / numberOfTosses.toDouble() * 100.0

    println("Hits: $hits, misses: $misses, %:$missesPercentage")

}

