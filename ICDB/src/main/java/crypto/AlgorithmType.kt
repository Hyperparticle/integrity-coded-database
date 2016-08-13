package crypto

import crypto.signer.MacSigner
import crypto.signer.RsaSigner


/**
 * Enumerates all supported algorithm types, with extension methods to generate and verify signatures using its
 * corresponding algorithm implementation.
 *
 * Created on 5/21/2016
 * @author Dan Kondratyuk
 */
enum class AlgorithmType {
    RSA {
        override fun generateSignature(data: ByteArray, key: Key) =
            RsaSigner.generate(data, key.publicRsaKey)

        override fun verify(data: ByteArray, key: Key, signature: ByteArray) =
            RsaSigner.verify(data, key.privateRsaKey, signature)
    },
    AES {
        override fun generateSignature(data: ByteArray, key: Key) =
            MacSigner.generate(data, key, MacSigner.cmacAes)

        override fun verify(data: ByteArray, key: Key, signature: ByteArray) =
            MacSigner.verify(data, key, signature, MacSigner.cmacAes)
    },
    SHA {
        override fun generateSignature(data: ByteArray, key: Key) =
            MacSigner.generate(data, key, MacSigner.hmacSha)

        override fun verify(data: ByteArray, key: Key, signature: ByteArray) =
            MacSigner.verify(data, key, signature, MacSigner.hmacSha)
    };

    abstract fun generateSignature(data: ByteArray, key: Key): ByteArray
    abstract fun verify(data: ByteArray, key: Key, signature: ByteArray): Boolean
}
