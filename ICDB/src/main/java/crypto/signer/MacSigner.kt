package crypto.signer

import crypto.Key
import org.bouncycastle.crypto.Mac
import java.util.*

/**
 * Base class implementing all Message Authentication Code (MAC) based signers
 *
 * Created on 7/22/2016
 * @author Dan Kondratyuk
 */
object MacSigner {

    // Size of output byte array
    const val DATA_SIZE = 16

    fun generate(data: ByteArray, key: Key, mac: Mac): ByteArray {
        val signature = ByteArray(DATA_SIZE)

        mac.init(key.macKey)
        mac.update(data, 0, data.size)
        mac.doFinal(signature, 0)

        return signature
    }

    fun verify(data: ByteArray, key: Key, signature: ByteArray, mac: Mac): Boolean {
        return Arrays.equals(generate(data, key, mac), signature)
    }

}