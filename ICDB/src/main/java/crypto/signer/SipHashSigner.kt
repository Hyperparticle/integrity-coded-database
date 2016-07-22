package crypto.signer

import crypto.Key
import org.bouncycastle.crypto.Mac
import org.bouncycastle.crypto.macs.SipHash

/**
 * Created on 7/22/2016
 * @author Dan Kondratyuk
 */
object SipHashSigner {

    private val sipHash: Mac
        get() {
            return SipHash()
        }

    @JvmStatic fun generate(data: ByteArray, key: Key): ByteArray = MacSigner.generate(data, key, sipHash)
    @JvmStatic fun verify(data: ByteArray, key: Key, signature: ByteArray): Boolean = MacSigner.verify(data, key, signature, sipHash)

}
