package cipher;

import cipher.signature.Convert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.security.KeyPair;
import java.security.Security;

/**
 * <p>
 * Wraps RSA and MAC keys for easy access
 * </p>
 * Created on 7/13/2016
 *
 * @author Dan Kondratyuk
 */
public class Key {

    private final AsymmetricCipherKeyPair rsaKeyPair;
    private final byte[] rawMacKey;
    private final KeyParameter macKey;

    private static final Logger logger = LogManager.getLogger();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public Key(String macKey, String rsaKeyFile) {
        this.rawMacKey = Convert.INSTANCE.fromBase64(macKey);
        this.macKey = new KeyParameter(rawMacKey);
        this.rsaKeyPair = readRSAKeys(rsaKeyFile);
    }

    public byte[] getRawMacKey() {
        return rawMacKey;
    }

    public KeyParameter getMacKey() {
        return macKey;
    }

    public AsymmetricKeyParameter getPublic() {
        return rsaKeyPair.getPublic();
    }

    public AsymmetricKeyParameter getPrivate() {
        return rsaKeyPair.getPrivate();
    }

    private static AsymmetricCipherKeyPair readRSAKeys(final String rsaKeyFile)
    {
        try (Reader reader = new FileReader(rsaKeyFile)) {
            PEMParser parser = new PEMParser(reader);

            Object object;
            while ((object = parser.readObject()) != null)
            {
                if (object instanceof PEMKeyPair)
                {
                    PEMKeyPair keyPair = (PEMKeyPair) object;

                    KeyPair pair = new JcaPEMKeyConverter()
                            .setProvider("BC")
                            .getKeyPair(keyPair);

                    AsymmetricKeyParameter privateKey = PrivateKeyFactory.createKey(pair.getPrivate().getEncoded());
                    AsymmetricKeyParameter publicKey = PublicKeyFactory.createKey(pair.getPublic().getEncoded());

                    return new AsymmetricCipherKeyPair(privateKey, publicKey);
                }
            }

            return (AsymmetricCipherKeyPair) parser.readObject();
        } catch (IOException e) {
            logger.error("Failed to read RSA key file: {}", e.getMessage());
            System.exit(1);
        }

        return null;
    }
}
