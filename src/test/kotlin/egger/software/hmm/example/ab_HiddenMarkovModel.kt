package egger.software.hmm.example

import egger.software.hmm.*
import egger.software.hmm.state.Caretaker
import egger.software.hmm.state.Caretaker.NoUmbrella
import egger.software.hmm.state.Caretaker.Umbrella
import egger.software.hmm.state.Weather
import egger.software.hmm.state.Weather.*
import egger.software.test.plusOrMinus
import egger.software.test.shouldBe
import kotlin.test.Test

// The following examples are described in:
// https://www.spsc.tugraz.at/system/files/hmm.pdf

class HiddenMarkovModelExample {

    @Test
    fun `the weather can be predicted by observing umbrellas`() {
        // given
        val weatherTable = stateTransitionTable<Weather, Weather> {

            Sunny resultsIn (Sunny withProbabilityOf 0.8)
            Sunny resultsIn (Rainy withProbabilityOf 0.05)
            Sunny resultsIn (Foggy withProbabilityOf 0.15)

            Rainy resultsIn (Sunny withProbabilityOf 0.1)
            Rainy resultsIn (Rainy withProbabilityOf 0.6)
            Rainy resultsIn (Foggy withProbabilityOf 0.2)

            Foggy resultsIn (Sunny withProbabilityOf 0.2)
            Foggy resultsIn (Rainy withProbabilityOf 0.3)
            Foggy resultsIn (Foggy withProbabilityOf 0.5)

        }

        val caretakerTable = stateTransitionTable<Weather, Caretaker> {
            Sunny resultsIn (Umbrella withProbabilityOf 0.1)
            Sunny resultsIn (NoUmbrella withProbabilityOf 0.9)

            Rainy resultsIn (Umbrella withProbabilityOf 0.8)
            Rainy resultsIn (NoUmbrella withProbabilityOf 0.2)

            Foggy resultsIn (Umbrella withProbabilityOf 0.3)
            Foggy resultsIn (NoUmbrella withProbabilityOf 0.7)
        }

        // when
        // the weather was sunny and today the caretaker has an umbrella

        // then
        // the likelihood of today's weather is rainy is 0.04

        // P(rainy|umbrella) = ( P(umbrella|rainy) * P(rainy) ) / P(umbrella) [Bayes rule]
        // L(rainy|umbrella) = P(umbrella|rainy) * P(rainy) [Likelihood removes P(umbrella) because it is L is proportional to P]
        // P(rainy) = P(rainy|sunny) -> from the weather table
        // L(rainy|umbrella) = P(umbrella|rainy) * P(rainy|sunny)
        (caretakerTable.given(Rainy) probabilityOf Umbrella) * (weatherTable.given(Sunny) probabilityOf Rainy) shouldBe 0.04.plusOrMinus(10E-9)


        // and the likelihood of today's weather is sunny is 0.08

        (caretakerTable.given(Sunny) probabilityOf Umbrella) * (weatherTable.given(Sunny) probabilityOf Sunny) shouldBe 0.08.plusOrMinus(10E-9)

        // and the likelihood of today's weather is foggy is 0.045

        (caretakerTable.given(Foggy) probabilityOf Umbrella) * (weatherTable.given(Sunny) probabilityOf Foggy) shouldBe 0.045.plusOrMinus(10E-9)

        // when
        // we do not know the weather at the beginning and we did not see an umbrella the following three days

        // then
        // The likelihood of sunny -> foggy -> sunny is 0.00567

        // as the weather at the beginning is unknown we assume P(rainy) = P(sunny) P(foggy) = 1/3
        // L(sunny, foggy, sunny | no_umbrella, no_umbrella, no_umbrella) =
        // ( P(no_umbrella|sunny) * P(no_umbrella|foggy) * P(no_umbrella|sunny) ) * ( P(sunny) * P(foggy|sunny) * P(sunny|foggy) )

        (caretakerTable.given(Sunny) probabilityOf NoUmbrella) *
                (caretakerTable.given(Foggy) probabilityOf NoUmbrella) *
                (caretakerTable.given(Sunny) probabilityOf NoUmbrella) *
                (1.0 / 3.0) * // sunny on the first day
                (weatherTable.given(Sunny) probabilityOf (Foggy)) *
                (weatherTable.given(Foggy) probabilityOf (Sunny)) shouldBe 0.00567.plusOrMinus(10E-9)


        // Alternative
        (caretakerTable.given(Sunny) probabilityOf NoUmbrella) *
                (caretakerTable.given(Foggy) probabilityOf NoUmbrella) *
                (caretakerTable.given(Sunny) probabilityOf NoUmbrella) *
                (1.0 / 3.0) * // sunny on the first day
                weatherTable.sequenceLikelihood(Sunny, Foggy, Sunny) shouldBe 0.00567.plusOrMinus(10E-9)

        // Another alternative
        val hmm = HiddenMarkovModel(
                initialStateProbabilities = listOf(Sunny withProbabilityOf 1.0 / 3.0, Rainy withProbabilityOf 1.0 / 3.0, Foggy withProbabilityOf 1.0 / 3.0),
                stateTransitions = weatherTable,
                observationProbabilities = caretakerTable)

        hmm.observing(NoUmbrella, NoUmbrella, NoUmbrella).startingWith(Sunny).likelihoodOf(Sunny, Foggy, Sunny) shouldBe 0.00567.plusOrMinus(10E-9)

        // Using log likelihood (needed for long sequences)
        hmm.observing(NoUmbrella, NoUmbrella, NoUmbrella).startingWith(Sunny).logLikelihoodOf(Sunny, Foggy, Sunny) shouldBe (-0.2464169411070934).plusOrMinus(10E-9)
    }
}



