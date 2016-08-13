package crypto.signer

import org.bouncycastle.crypto.Signer
import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.engines.RSAEngine
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.signers.ISO9796d2Signer

/**
 *
 * Created on 7/12/2016
 *
 * @author Dan Kondratyuk
 */
object RsaSigner {

    private val rsaSigner: Signer
        get() {
            val rsa = RSAEngine()
            val digest = SHA1Digest()
            return ISO9796d2Signer(rsa, digest)
        }

    fun generate(data: ByteArray, privateKey: AsymmetricKeyParameter): ByteArray {
        val signer = this.rsaSigner

        signer.init(true, privateKey)
        signer.update(data, 0, data.size)

        return signer.generateSignature()
    }

    fun verify(data: ByteArray, publicKey: AsymmetricKeyParameter, signature: ByteArray): Boolean {
        val signer = rsaSigner

        signer.init(false, publicKey)
        signer.update(data, 0, data.size)

        return signer.verifySignature(signature)
    }

}
