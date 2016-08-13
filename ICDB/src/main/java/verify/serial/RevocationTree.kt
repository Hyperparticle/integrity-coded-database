package verify.serial

import java.util.*

/**
 * A Revocation Tree is an ordered tree of non-overlapping intervals of integers. The use case is to maintain a
 * list of ranges of valid serial numbers to determine if a given range query is valid.
 *
 * This implementation provides a fast way to:
 * 1. Add valid entries by extending the size of the last interval in the tree
 * 2. Remove an invalid interval/point by cutting an interval, possibly producing a second interval
 * 3. Query whether an interval is completely overlapped by an interval in the tree
 *
 * Created on 8/3/2016
 * @author Dan Kondratyuk
 *
 * @property intervalMap a NavigableMap implementation that may have existing intervals
 */
abstract class RevocationTree<T : Comparable<T>>(val intervalMap: NavigableMap<T, T>) {

    val next: T
        get() = intervalMap.lastEntry().value

    init {
        // Validate that no intervals contain min values larger than max values
        intervalMap.map { it.key.compareTo(it.value) > 1 }
            .filter { it }
            .forEach { throw IllegalArgumentException("Interval map must not contain min values larger than max values") }
    }

    /**
     * Adds the specified number of valid serial numbers to the tree
     */
    abstract fun add(range: T)

    /**
     * Removes a value from the tree
     */
    abstract fun remove(value: T)

    /**
     * Validates whether the value is contained in the tree
     */
    abstract fun contains(value: T): Boolean

}