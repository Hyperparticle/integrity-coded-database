package cipher;

import cipher.signature.CMAC;
import cipher.signature.HMAC;
import cipher.signature.RSASignature;
import org.bouncycastle.crypto.params.RSAKeyParameters;


/**
 * <p>
 * </p>
 * Created on 5/21/2016
 *
 * @author Dan Kondratyuk
 */
public enum AlgorithmType {
    RSA {
        @Override
        public byte[] generateSignature(final byte[] data, final Key key) {
            return RSASignature.generate(data, key.getPrivate());
        }

        @Override
        public boolean verify(final byte[] data, final Key key, final byte[] signature) {
            return RSASignature.verify(data, key.getPublic(), signature);
        }
    },
    AES {
        @Override
        public byte[] generateSignature(final byte[] data, final Key key) {
            return CMAC.generate(data, key);
        }

        @Override
        public boolean verify(final byte[] data, final Key key, final byte[] signature) {
            return CMAC.verify(data, key, signature);
        }
    },
    SHA {
        @Override
        public byte[] generateSignature(final byte[] data, final Key key) {
            return HMAC.generate(data, key);
        }

        @Override
        public boolean verify(final byte[] data, final Key key, final byte[] signature) {
            return HMAC.verify(data, key, signature);
        }
    };

    public abstract byte[] generateSignature(final byte[] data, final Key key);
    public abstract boolean verify(final byte[] data, final Key key, final byte[] signature);
}
