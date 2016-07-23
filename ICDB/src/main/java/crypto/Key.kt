package crypto

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.crypto.util.PublicKeyFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter

import java.io.FileReader
import java.security.Security

/**
 * Wraps RSA and MAC keys for easy access
 *
 * Created on 7/13/2016
 * @author Dan Kondratyuk
 */
class Key(macKey: String, rsaKeyFile: String) {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    val rawMacKey: ByteArray
    val macKey: KeyParameter
    val publicRsaKey: AsymmetricKeyParameter
    val privateRsaKey: AsymmetricKeyParameter

    init {
        this.rawMacKey = Convert.fromBase64(macKey)
        this.macKey = KeyParameter(rawMacKey)

        val rsaKeyPair = readRSAKeys(rsaKeyFile)
        publicRsaKey = rsaKeyPair.public
        privateRsaKey = rsaKeyPair.private
    }

    private fun readRSAKeys(rsaKeyFile: String): AsymmetricCipherKeyPair {
        FileReader(rsaKeyFile).use { reader ->
            val parser = PEMParser(reader)

            var o: Any? = null
            while ({ o = parser.readObject(); o }() != null) {
                if (o is PEMKeyPair) {
                    val pair = JcaPEMKeyConverter().setProvider("BC").getKeyPair(o as PEMKeyPair)

                    val privateKey = PrivateKeyFactory.createKey(pair.private.encoded)
                    val publicKey = PublicKeyFactory.createKey(pair.public.encoded)

                    return AsymmetricCipherKeyPair(privateKey, publicKey)
                }
            }

            return parser.readObject() as AsymmetricCipherKeyPair
        }
    }

}
