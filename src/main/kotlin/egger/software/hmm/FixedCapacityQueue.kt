package egger.software.hmm


class FixedCapacityQueue<TValue>(capacity: Int) : AbstractCollection<TValue>() {

    private var firstIndex: Int = 0
    private var nextIndex: Int = 0
    private val buffer: Array<Any?> = Array(capacity, { null })

    override var size: Int = 0
        private set

    override fun iterator(): Iterator<TValue> {

        return object : Iterator<TValue> {

            private var current = firstIndex
            private var available = size

            override fun hasNext() = available > 0

            override fun next(): TValue {
                if (available <= 0) throw NoSuchElementException()
                val result = buffer[current]

                current++
                if (current >= buffer.size )
                    current = 0

                available--

                @Suppress("UNCHECKED_CAST")
                return result as TValue
            }

        }

    }


    fun add(value: TValue) {

        buffer[nextIndex] = value

        if (size < buffer.size)
            size++
        else
            firstIndex++

        if (firstIndex >= buffer.size)
            firstIndex = 0

        nextIndex++
        if (nextIndex >= buffer.size)
            nextIndex = 0

    }
}