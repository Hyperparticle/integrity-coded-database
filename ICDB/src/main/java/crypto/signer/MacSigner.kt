package crypto.signer

import crypto.Key
import org.bouncycastle.crypto.Mac
import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.digests.ShortenedDigest
import org.bouncycastle.crypto.engines.AESFastEngine
import org.bouncycastle.crypto.macs.CMac
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.macs.SipHash
import org.bouncycastle.crypto.tls.SSL3Mac
import java.util.*

/**
 * A Message Authentication Code (MAC) is a fast way to generate a signature designed to detect code tampering.
 * Created on 7/22/2016
 * @author Dan Kondratyuk
 */
object MacSigner {

    // Size of output byte array
    const val DATA_SIZE = 16

    // The following properties provide different types of MAC signers to use

//    class Signer(mac: Mac, size: Int)

    /**
     * Hash-based MAC
     */
    val hmacSha: Mac
        get() {
            val sha = SHA1Digest()
            val digest = ShortenedDigest(sha, DATA_SIZE)
            return HMac(digest)
        }

    /**
     * BlockCipher-based MAC
     */
    val cmacAes: Mac
        get() {
            val aes = AESFastEngine()
            return CMac(aes)
        }

    /**
     * Very fast, 64 bit MAC
     */
    val sipHash: Mac
        get() {
            return SipHash()
        }

    val ssl3Mac: Mac
        get() {
            val sha = SHA1Digest()
            val digest = ShortenedDigest(sha, MacSigner.DATA_SIZE)
            return SSL3Mac(digest)
        }

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