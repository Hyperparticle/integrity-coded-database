package crypto

/**
 * A wrapper class containing a key and an algorithm to generate a signature from a message
 *
 * Created on 6/29/2016
 * @author Dan Kondratyuk
 */
class CodeGen(var algorithm: AlgorithmType, private val key: Key) {

    fun generateSignature(data: ByteArray): ByteArray {
        return algorithm.generateSignature(data, key)
    }

    fun verify(data: ByteArray, signature: ByteArray): Boolean {
        return algorithm.verify(data, key, signature)
    }

}
