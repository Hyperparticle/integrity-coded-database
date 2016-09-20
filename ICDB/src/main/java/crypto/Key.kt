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
import java.math.BigInteger
import java.security.KeyFactory
import java.security.Security
import java.security.spec.RSAPublicKeySpec

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

    val rawMacKey = Convert.fromBase64(macKey)
    val macKey = KeyParameter(rawMacKey)
    var modulus: BigInteger =  BigInteger("1")
    var exponent: BigInteger =  BigInteger("1")
    private val rsaKeyPair = readRSAKeys(rsaKeyFile)

    val publicRsaKey: AsymmetricKeyParameter = rsaKeyPair.public
    val privateRsaKey: AsymmetricKeyParameter = rsaKeyPair.private

    private fun readRSAKeys(rsaKeyFile: String): AsymmetricCipherKeyPair {
        FileReader(rsaKeyFile).use { reader ->
            val parser = PEMParser(reader)

            var o: Any? = null
            while ({ o = parser.readObject(); o }() != null) {
                if (o is PEMKeyPair) {
                    val pair = JcaPEMKeyConverter()
                        .setProvider("BC")
                        .getKeyPair(o as PEMKeyPair)

                    val privateKey = PrivateKeyFactory.createKey(pair.private.encoded)
                    val publicKey = PublicKeyFactory.createKey(pair.public.encoded)

                    val keyFac = KeyFactory.getInstance("RSA")
                    val pkSpec = keyFac.getKeySpec(pair.public,
                            RSAPublicKeySpec::class.java)

                    modulus = pkSpec.modulus
                    exponent=pkSpec.publicExponent

                    return AsymmetricCipherKeyPair(privateKey, publicKey)
                }
            }

            return parser.readObject() as AsymmetricCipherKeyPair
        }
    }

}
