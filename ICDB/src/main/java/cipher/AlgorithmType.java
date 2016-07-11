package cipher;

import cipher.mac.CMAC;
import cipher.mac.HMAC;


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
        public byte[] generateSignature(final byte[] data, final byte[] key) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean verify(final byte[] data, final byte[] key, final byte[] signature) {
            throw new RuntimeException("Not implemented");
        }
    },
    AES {
        @Override
        public byte[] generateSignature(final byte[] data, final byte[] key) {
            return CMAC.generateSignature(data, key);
        }

        @Override
        public boolean verify(final byte[] data, final byte[] key, final byte[] signature) {
            return CMAC.verify(data, key, signature);
        }
    },
    SHA {
        @Override
        public byte[] generateSignature(final byte[] data, final byte[] key) {
            return HMAC.generateSignature(data, key);
        }

        @Override
        public boolean verify(final byte[] data, final byte[] key, final byte[] signature) {
            return HMAC.verify(data, key, signature);
        }
    };

    public abstract byte[] generateSignature(final byte[] data, final byte[] key);
    public abstract boolean verify(final byte[] data, final byte[] key, final byte[] signature);
}
