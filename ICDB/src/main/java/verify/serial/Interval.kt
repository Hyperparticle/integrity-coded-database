package verify.serial

/**
 * Represents a contiguous interval of serial numbers
 *
 * Created on 8/3/2016
 * @author Dan Kondratyuk
 *
 * @property min The interval miminum value
 * @property max The interval maximum value (inclusive)
 */
class Interval(val min: Long, max: Long) {

    var max = max
        private set

    /**
     * Increments the maximum interval value by one
     * @return the new (incremented) max value
     */
    fun inc(): Long = ++max
}