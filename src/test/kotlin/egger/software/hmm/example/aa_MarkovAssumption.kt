package egger.software.hmm.example

import egger.software.hmm.*
import egger.software.hmm.state.Weather
import egger.software.hmm.state.Weather.*
import egger.software.test.plusOrMinus
import egger.software.test.shouldBe
import kotlin.test.Test

// The following examples are described in:
// https://www.spsc.tugraz.at/system/files/hmm.pdf
//

//                 | Tomorrow’s weather
// Today’s weather | sunny | rainy | foggy
// ----------------|----------------------
//           sunny | 0.8   | 0.05  | 0.15
//           rainy | 0.1   | 0.6   | 0.2
//           foggy | 0.2   | 0.3   | 0.6

class MarkovAssumptionExample {

    @Test
    fun `the weather can be predicted using the Markov assumption`() {

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

        // when / then
        (weatherTable.given(Sunny) probabilityOf Sunny) shouldBe 0.8
        (weatherTable.given(Sunny) probabilityOf Rainy) shouldBe 0.05


        // when / then
        // the probability of tomorrow it is sunny and the day after it is rainy is 0.04"
        // P(sunny, rainy | sunny) = P(rainy | sunny, sunny) * P(sunny | sunny)
        // =  P(rainy | sunny) * P(sunny | sunny) // Markov assumption  -> P (qn|qn−1, qn−2, ..., q1) = P (qn|qn−1)
        ((weatherTable.given(Sunny) probabilityOf Rainy) * (weatherTable.given(Sunny) probabilityOf Sunny)) shouldBe 0.04.plusOrMinus(1E-9)

        // when
        // the weather yesterday was rainy and today it is foggy

        // then
        // the probability of tomorrow it is sunny is 0.2
        // P(sunny | foggy, rainy) = P(sunny | foggy) (using the Markov assumption)
        // => we do not have to take into account P(foggy | rainy)
        ((weatherTable.given(Foggy) probabilityOf Sunny)) shouldBe 0.2

        // when
        // the weather today is foggy

        // then
        // the probability of it is rainy two days from now is 0.34

        // Find all possible paths and sum up probabilities
        //
        // foggy -(0.2)-> sunny -(0.05)-> rainy = 0.2 * 0.05 = 0.01
        // foggy -(0.3)-> rainy -(0.6)-> rainy = 0.3 * 0.6 = 0.18
        // foggy -(0.5)-> foggy -(0.2)> rainy = 0.5 * 0.3 = 0.15
        // foggy --> ??? --> rainy = 0.01 + 0.18 + 0.15 = 0.34
        //
        // In math notation
        // P(sunny, rainy | foggy) +
        // P(foggy, rainy | foggy) +
        // P(rainy, rainy | foggy)

        val probabilityOfFoggySunnyRainy =
                (weatherTable.given(Foggy) probabilityOf Sunny) * (weatherTable.given(Sunny) probabilityOf Rainy)

        val probabilityOfFoggyRainyRainy =
                (weatherTable.given(Foggy) probabilityOf Rainy) * (weatherTable.given(Rainy) probabilityOf Rainy)

        val probabilityOfFoggyFoggyRainy =
                (weatherTable.given(Foggy) probabilityOf Foggy) * (weatherTable.given(Foggy) probabilityOf Rainy)

        probabilityOfFoggySunnyRainy shouldBe 0.01.plusOrMinus(1E-9)
        probabilityOfFoggyRainyRainy shouldBe 0.18.plusOrMinus(1E-9)
        probabilityOfFoggyFoggyRainy shouldBe 0.15.plusOrMinus(1E-9)

        (probabilityOfFoggySunnyRainy + probabilityOfFoggyRainyRainy + probabilityOfFoggyFoggyRainy) shouldBe 0.34.plusOrMinus(1E-9)

        // Alternative calculation using state transition table function
        (weatherTable.sequenceLikelihood(Foggy, Sunny, Rainy) +
                weatherTable.sequenceLikelihood(Foggy, Rainy, Rainy) +
                weatherTable.sequenceLikelihood(Foggy, Foggy, Rainy)) shouldBe 0.34.plusOrMinus(1E-9)
    }

    @Test
    fun `calculating the likelihood of long sequences leeds to float precision problems which can be solved by using the log likelihood instead`() {

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

        // when / then
        // 10.000 is indistinguishable of 100.000 if using the simple likelihood function
        (weatherTable.sequenceLikelihood(List(10000, { Sunny }))) shouldBe 1.0E-323
        (weatherTable.sequenceLikelihood(List(100000, { Sunny }))) shouldBe 1.0E-323

        // using log likelihood works better
        (weatherTable.sequenceLogLikelihood(List(10000, { Sunny }))) shouldBe -968.0032200674957
        (weatherTable.sequenceLogLikelihood(List(100000, { Sunny }))) shouldBe -9689.90439078168

    }

}
