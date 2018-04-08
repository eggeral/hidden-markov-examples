package egger.software.hmm

import egger.software.hmm.Tile.*
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.specs.BehaviorSpec


class UtilsSpec : BehaviorSpec() {

    init {
        Given("a list of StateWithProbability") {
            val statesWithProbability = listOf(Red withProbabilityOf 0.2, Green withProbabilityOf 0.6, Blue withProbabilityOf 0.2)

            When("the element with offset 0.5 is picked") {
                val picked = statesWithProbability.selectStateAtOffset(0.5)

                Then("Green is returned") {
                    picked shouldBe Green
                }
            }

            When("the element with offset 0.1 is picked") {
                val picked = statesWithProbability.selectStateAtOffset(0.1)

                Then("Red is returned") {
                    picked shouldBe Red
                }
            }

            When("the element with offset 0.0 is picked") {
                val picked = statesWithProbability.selectStateAtOffset(0.0)

                Then("Red is returned") {
                    picked shouldBe Red
                }
            }

            When("the element with offset 0.3 is picked") {
                val picked = statesWithProbability.selectStateAtOffset(0.3)

                Then("Blue is returned") {
                    picked shouldBe Blue
                }
            }

            When("the element with offset 0.4 is picked") {
                val picked = statesWithProbability.selectStateAtOffset(0.4)

                Then("Green is returned") {
                    picked shouldBe Green
                }
            }

            When("the element with offset below 0.0 is picked") {

                Then("an exception is thrown") {
                    shouldThrow<IllegalArgumentException> { statesWithProbability.selectStateAtOffset(-0.1) }
                }
            }

            When("the element with offset larger or equal to 0.0 is picked") {

                Then("an exception is thrown") {
                    shouldThrow<IllegalArgumentException> { statesWithProbability.selectStateAtOffset(1.0) }
                }
            }

        }

        Given("a list of StateWithProbability") {
            val statesWithProbability = listOf(Red withProbabilityOf 0.2, Green withProbabilityOf 0.8, Blue withProbabilityOf 0.2)
            When("the probabilities do not sum up to 1.0") {
                Then("an exception is thrown") {
                    shouldThrow<IllegalArgumentException> { statesWithProbability.selectStateAtOffset(0.5) }
                }
            }

        }
    }
}
