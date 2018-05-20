package egger.software.hmm.example

import egger.software.hmm.HiddenMarkovModel
import egger.software.hmm.algorithm.mostLikelyStateSequence
import egger.software.hmm.experiment.Coin
import egger.software.hmm.experiment.Coin.Fair
import egger.software.hmm.experiment.Coin.UnFair
import egger.software.hmm.experiment.Toss
import egger.software.hmm.experiment.Toss.Heads
import egger.software.hmm.experiment.Toss.Tails
import egger.software.hmm.observing
import egger.software.hmm.stateTransitionTable
import egger.software.hmm.withProbabilityOf
import egger.software.test.shouldBe
import kotlin.test.Test

// See:
// L. Rabiner, "A Tutorial on Hidden Markov Models and Selected Applications in Speech Recognition", Proc. IEEE, Feb. 1989.
// http://www.cs.cornell.edu/courses/cs312/2006sp/lectures/lec17.html

class CoinTossExample {

    // In a game of coin tossing there is a fair coin and an unfair coin.
    // There is only one coin tossed at any time.
    // Changing the coin is hard to do and will not happen often
    // Only the result of the coin toss can be observed
    // The coin change is hidden
    // We want to detect which coin is used
    //
    // The probability of changing coins is 0.1
    // For the unfair coin the probability of heads is 0.6
    // For the fair coin the probability of heads is 0.5
    //

    @Test
    fun `the most probable coin sequence (fair or unfair) can be calculated by observing coin tosses`() {
        // given
        val coinTable = stateTransitionTable<Coin, Coin> {

            Fair resultsIn (UnFair withProbabilityOf 0.1)
            Fair resultsIn (Fair withProbabilityOf 0.9)

            UnFair resultsIn (Fair withProbabilityOf 0.1)
            UnFair resultsIn (UnFair withProbabilityOf 0.9)

        }

        val observationTable = stateTransitionTable<Coin, Toss> {

            Fair resultsIn (Heads withProbabilityOf 0.5)
            Fair resultsIn (Tails withProbabilityOf 0.5)

            UnFair resultsIn (Heads withProbabilityOf 0.6)
            UnFair resultsIn (Tails withProbabilityOf 0.4)
        }

        // when
        // we observe the sequence Heads, Tails, Heads

        // then
        // the most probable coin sequence is UnFair, UnFair, UnFair

        val hmm = HiddenMarkovModel(
                initialStateProbabilities = listOf(Fair withProbabilityOf 0.5, UnFair withProbabilityOf 0.5),
                stateTransitions = coinTable,
                observationProbabilities = observationTable).observing(Heads, Tails, Heads)

        hmm.mostLikelyStateSequence shouldBe listOf(UnFair, UnFair, UnFair)

    }
}

