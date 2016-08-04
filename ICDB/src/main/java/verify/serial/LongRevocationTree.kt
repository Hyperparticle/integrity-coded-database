package verify.serial

import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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

    override fun add(range: Long): Long {
        if (range < 1) { throw IllegalArgumentException("Added range must be >= 1") }

        val last = intervalMap.lastEntry()
        return last.setValue(next + range)
    }

    override fun remove(value: Long) {
        // Extract the interval
        val interval = intervalMap.floorEntry(value) ?: throw IllegalArgumentException("value must be within an interval")
        val (min, max) = interval

        val lock = ReentrantLock()
        lock.withLock {
            // There are 4 cases to consider:
            if (value == min && value == max-1) {       // 1. single element
                intervalMap.remove(value)
            } else if (value == min) {                  // 2. min value
                intervalMap.remove(min)
                intervalMap.put(min+1, max)
            } else if (value == max-1) {                // 3. max value
                interval.setValue(max-1)
            } else if (value > min && value < max-1) {  // 4. value inside interval
                interval.setValue(value)
                intervalMap.put(value+1, max)
            } else {
                throw IllegalArgumentException("value must be within an interval")
            }
        }


    }

    override fun validate(minRange: Long, maxRange: Long): Boolean {
        if (minRange > maxRange) { throw IllegalArgumentException("maxRange must be >= minRange") }

        // Return true if within bounds of the nearest interval, false otherwise
        val interval = intervalMap.floorEntry(minRange) ?: return false
        return interval.value < maxRange
    }
}
