package cipher.signature

import cipher.Key
import org.bouncycastle.crypto.Digest
import org.bouncycastle.crypto.Mac
import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.digests.ShortenedDigest
import org.bouncycastle.crypto.macs.HMac

import java.util.Arrays

/**
 *
 *
 * A Hashing Message Authentication Code (HMAC) is a fast way to generate a
 * code designed to detect code tampering
 *
 * Created on 6/2/2016

 * @author Dan Kondratyuk
 */
object HMAC {

    private val DATA_SIZE = 16 // Size in bytes

    private val cmac: Mac
        get() {
            val digest = ShortenedDigest(SHA1Digest(), DATA_SIZE)
            return HMac(digest)
        }

    private val digest = ShortenedDigest(SHA1Digest(), DATA_SIZE)
    private val hmac = HMac(digest)

    fun generate(data: ByteArray, key: Key): ByteArray {
        //        Mac hmac = hmac;

        hmac.init(key.macKey)
        hmac.update(data, 0, data.size)
        val result = ByteArray(DATA_SIZE)
        hmac.doFinal(result, 0)
        return result
    }

    fun verify(data: ByteArray, key: Key, signature: ByteArray): Boolean {
        val generated = generate(data, key)
        return Arrays.equals(generated, signature)
    }




}
