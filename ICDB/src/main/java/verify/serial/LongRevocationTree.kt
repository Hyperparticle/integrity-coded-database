package verify.serial

import java.util.*

/**
 * A Long implementation of a Revocation Tree
 *
 * Created on 8/3/2016
 * @see RevocationTree
 * @author Dan Kondratyuk
 */
class LongRevocationTree(intervalMap: NavigableMap<Long, Long>) : RevocationTree<Long>(intervalMap) {

    constructor(intervalMap: NavigableMap<Long, Long>, offset: Long) : this(intervalMap) {
        intervalMap.put(offset, offset)
    }

    @Synchronized override fun add(range: Long) {
        if (range < 1) { throw IllegalArgumentException("Added range must be >= 1") }

        val last = intervalMap.pollLastEntry()
        intervalMap.put(last.key, last.value + range)
    }

    @Synchronized override fun remove(value: Long) {
        // Extract the interval
        val (min, max) = intervalMap.floorEntry(value) ?: throw IllegalArgumentException("value must not be smaller than the minimum")

        if (value >= max) {
            throw IllegalArgumentException("value must be within an interval")
        }

        // The interval, since it is read only
        intervalMap.remove(min)

        // There are 4 cases to consider:
        if (value == min) {                    // 1. min value
            if (value == max - 1) { return; }  // 2. single element (already removed)
            intervalMap.put(min + 1, max)
        } else if (value == max - 1) {         // 3. max value
            intervalMap.put(min, max - 1)
        } else {                               // 4. value inside interval
            intervalMap.put(min, value)
            intervalMap.put(value + 1, max)
        }
    }

    override fun contains(value: Long): Boolean {
        // Return true if within bounds of the nearest interval, false otherwise
        val interval = intervalMap.floorEntry(value) ?: return false
        return value < interval.value
    }
}
