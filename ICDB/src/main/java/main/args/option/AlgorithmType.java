package main.args.option;

import cipher.mac.CMAC;
import cipher.mac.HMAC;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;


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
        public byte[] generateSignature(byte[] data, byte[] key) {
            throw new NotImplementedException();
        }

        @Override
        public boolean verify(byte[] data, byte[] key, byte[] signature) {
            throw new NotImplementedException();
        }
    },
    AES {
        @Override
        public byte[] generateSignature(byte[] data, byte[] key) {
            return CMAC.generateSignature(data, key);
        }

        @Override
        public boolean verify(byte[] data, byte[] key, byte[] signature) {
            return CMAC.verify(data, key, signature);
        }
    },
    SHA {
        @Override
        public byte[] generateSignature(byte[] data, byte[] key) {
            return HMAC.generateSignature(data, key);
        }

        @Override
        public boolean verify(byte[] data, byte[] key, byte[] signature) {
            return HMAC.verify(data, key, signature);
        }
    };

    public abstract byte[] generateSignature(final byte[] data, final byte[] key);
    public abstract boolean verify(byte[] data, byte[] key, byte[] signature);
}
