package crypto

import crypto.signer.CmacSigner
import crypto.signer.HmacSigner
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
        override fun generateSignature(data: ByteArray, key: Key): ByteArray {
            return RsaSigner.generate(data, key.publicRsaKey)
        }

        override fun verify(data: ByteArray, key: Key, signature: ByteArray): Boolean {
            return RsaSigner.verify(data, key.privateRsaKey, signature)
        }
    },
    AES {
        override fun generateSignature(data: ByteArray, key: Key): ByteArray {
            return CmacSigner.generate(data, key)
        }

        override fun verify(data: ByteArray, key: Key, signature: ByteArray): Boolean {
            return CmacSigner.verify(data, key, signature)
        }
    },
    SHA {
        override fun generateSignature(data: ByteArray, key: Key): ByteArray {
            return HmacSigner.generate(data, key)
        }

        override fun verify(data: ByteArray, key: Key, signature: ByteArray): Boolean {
            return HmacSigner.verify(data, key, signature)
        }
    };

    abstract fun generateSignature(data: ByteArray, key: Key): ByteArray
    abstract fun verify(data: ByteArray, key: Key, signature: ByteArray): Boolean
}
