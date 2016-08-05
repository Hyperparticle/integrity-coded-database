package verify.serial

import java.security.SecureRandom

/**
 * A random number generator (RNG)
 *
 * Created on 8/5/2016
 * @author Dan Kondratyuk
 */
object Rng {

    private val random = SecureRandom()

    fun next(): Long = random.nextInt(Integer.MAX_VALUE).toLong()
}