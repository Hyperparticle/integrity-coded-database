package cipher;

import cipher.signature.CMAC;
import cipher.signature.HMAC;
import cipher.signature.RSASignature;


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
            return RSASignature.INSTANCE.generate(data, key.getPrivate());
        }

        @Override
        public boolean verify(final byte[] data, final Key key, final byte[] signature) {
            return RSASignature.INSTANCE.verify(data, key.getPublic(), signature);
        }
    },
    AES {
        @Override
        public byte[] generateSignature(final byte[] data, final Key key) {
            return CMAC.INSTANCE.generate(data, key);
        }

        @Override
        public boolean verify(final byte[] data, final Key key, final byte[] signature) {
            return CMAC.INSTANCE.verify(data, key, signature);
        }
    },
    SHA {
        @Override
        public byte[] generateSignature(final byte[] data, final Key key) {
            return HMAC.INSTANCE.generate(data, key);
        }

        @Override
        public boolean verify(final byte[] data, final Key key, final byte[] signature) {
            return HMAC.INSTANCE.verify(data, key, signature);
        }
    };

    public abstract byte[] generateSignature(final byte[] data, final Key key);
    public abstract boolean verify(final byte[] data, final Key key, final byte[] signature);
}
