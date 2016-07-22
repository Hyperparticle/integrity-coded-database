package crypto.signer


import crypto.Key
import org.bouncycastle.crypto.Mac
import org.bouncycastle.crypto.engines.AESFastEngine
import org.bouncycastle.crypto.macs.CMac

import java.util.Arrays

/**
 * A Cipher Message Authentication Code (CMAC) is a fast way to generate a
 * code designed to detect code tampering. Uses AES as its underlying implementation.
 *
 * Created on 6/2/2016
 * @author Dan Kondratyuk
 */
object CmacSigner {

    private val cmac: Mac
        get() {
            val aes = AESFastEngine()
            return CMac(aes)
        }

    @JvmStatic fun generate(data: ByteArray, key: Key): ByteArray = MacSigner.generate(data, key, cmac)
    @JvmStatic fun verify(data: ByteArray, key: Key, signature: ByteArray): Boolean = MacSigner.verify(data, key, signature, cmac)

}
