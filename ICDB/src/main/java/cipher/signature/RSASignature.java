package cipher.signature;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.signers.ISO9796d2Signer;

/**
 * <p>
 * </p>
 * Created on 7/12/2016
 *
 * @author Dan Kondratyuk
 */
public class RSASignature {

    private static Logger logger = LogManager.getLogger();

    public static byte[] generate(final byte[] data, final AsymmetricKeyParameter privateKey) {
        RSAEngine rsa = new RSAEngine();
        ISO9796d2Signer signer = new ISO9796d2Signer(rsa, new SHA1Digest());
        signer.init(true, privateKey);

        signer.update(data, 0, data.length);

        try {
            return signer.generateSignature();
        } catch (CryptoException e) {
            logger.error("Failed to generate RSA signature: {}", e.getMessage());
            System.exit(1);
        }

        return null;
    }

    public static boolean verify(final byte[] data, final AsymmetricKeyParameter publicKey, final byte[] signature) {

        RSAEngine rsa = new RSAEngine();
        ISO9796d2Signer signer = new ISO9796d2Signer(rsa, new SHA1Digest());
        signer.init(false, publicKey);

        signer.update(data, 0, data.length);
        return signer.verifySignature(signature);
    }
}
