package crypto.signer

import crypto.Key
import org.bouncycastle.crypto.Mac
import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.digests.ShortenedDigest
import org.bouncycastle.crypto.tls.SSL3Mac

/**
 * Created on 7/22/2016
 * @author Dan Kondratyuk
 */
object Ssl3MacSigner {

    private val ssl3Mac: Mac
        get() {
            val sha = SHA1Digest()
            val digest = ShortenedDigest(sha, MacSigner.DATA_SIZE)
            return SSL3Mac(digest)
        }

    @JvmStatic fun generate(data: ByteArray, key: Key): ByteArray = MacSigner.generate(data, key, ssl3Mac)
    @JvmStatic fun verify(data: ByteArray, key: Key, signature: ByteArray): Boolean = MacSigner.verify(data, key, signature, ssl3Mac)

}
