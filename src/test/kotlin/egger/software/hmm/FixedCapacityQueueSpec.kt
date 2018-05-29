package egger.software.hmm

import egger.software.test.shouldBe
import egger.software.test.shouldThrow
import org.junit.Test


class FixedCapacityQueueSpec {
    @Test
    fun `a fixed capacity queue holds up to a given number of elements`() {
        // given
        val queue = FixedCapacityQueue<String>(3)

        // then
        queue.iterator().hasNext() shouldBe false
        { queue.iterator().next() } shouldThrow { it is NoSuchElementException }
        queue.size shouldBe 0

        // when
        queue.add("a")

        // then
        queue.iterator().asSequence().toList() shouldBe listOf("a")
        queue.size shouldBe 1

        // when
        queue.add("b")

        // then
        queue.iterator().asSequence().toList() shouldBe listOf("a", "b")
        queue.size shouldBe 2

        // when
        queue.add("c")

        // then
        queue.iterator().asSequence().toList() shouldBe listOf("a", "b", "c")
        queue.size shouldBe 3

        // when
        queue.add("d")

        // then
        queue.iterator().asSequence().toList() shouldBe listOf("b", "c", "d")
        queue.size shouldBe 3


        // when
        queue.add("e")
        queue.add("f")
        queue.add("g")
        queue.add("h")

        // then
        queue.iterator().asSequence().toList() shouldBe listOf("f", "g", "h")
        queue.size shouldBe 3

        val iterator = queue.iterator()
        iterator.next() shouldBe "f"
        iterator.next() shouldBe "g"
        iterator.next() shouldBe "h"
        iterator.hasNext() shouldBe false
        { iterator.next() } shouldThrow { it is NoSuchElementException }

    }
}