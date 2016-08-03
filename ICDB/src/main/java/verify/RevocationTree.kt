package verify

import java.util.*

/**
 * A Revocation Tree is an ordered binary tree of non-overlapping intervals of integers. The use case is to maintain a
 * list of ranges of valid serial numbers to determine if a given range query is valid.
 *
 * This implementation provides a fast way to:
 * 1. Add valid entries by extending the size of the last interval in the tree
 * 2. Remove an invalid interval/point by cutting an interval, possibly producing a second interval
 *
 * Created on 8/3/2016
 * @author Dan Kondratyuk
 */
class RevocationTree<T> {

    val map: NavigableMap<T, T>

    constructor(map: NavigableMap<T, T>) {
        this.map = map
    }

    constructor() {
        this.map = TreeMap<T, T>()
    }

}