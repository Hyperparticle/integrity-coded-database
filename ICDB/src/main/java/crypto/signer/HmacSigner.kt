package crypto.signer

import crypto.Key
import org.bouncycastle.crypto.Mac
import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.digests.ShortenedDigest
import org.bouncycastle.crypto.macs.HMac

/**
 * A Hash Message Authentication Code (HMAC) is a fast way to generate a
 * code designed to detect code tampering. Uses SHA as its underlying implementation.
 *
 * Created on 6/2/2016
 * @author Dan Kondratyuk
 */
object HmacSigner {

    private val hmac: Mac
        get() {
            val sha = SHA1Digest()
            val digest = ShortenedDigest(sha, MacSigner.DATA_SIZE)
            return HMac(digest)
        }

    @JvmStatic fun generate(data: ByteArray, key: Key): ByteArray = MacSigner.generate(data, key, hmac)
    @JvmStatic fun verify(data: ByteArray, key: Key, signature: ByteArray): Boolean = MacSigner.verify(data, key, signature, hmac)

}
