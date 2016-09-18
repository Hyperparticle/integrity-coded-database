package verify.serial

import java.util.concurrent.atomic.AtomicLong

/**
 * An ICRL that always contains an input serial. Useful for debugging multiple databses at the same time.
 *
 * Created on 9/18/2016
 * @author Dan Kondratyuk
 */
class NullIcrl : AbstractIcrl {
    private val next: AtomicLong = AtomicLong(Rng.next())

    override fun addNext(): Long = next.andIncrement

    override fun commit() {}

    override fun revoke(serial: Long) {}

    override fun contains(serial: Long): Boolean = true
}