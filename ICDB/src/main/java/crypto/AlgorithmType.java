package crypto;

import crypto.signer.CmacSigner;
import crypto.signer.HmacSigner;
import crypto.signer.RsaSigner;
import crypto.signer.SipHashSigner;


/**
 * Created on 5/21/2016
 * @author Dan Kondratyuk
 */
public enum AlgorithmType {
    RSA {
        @Override
        public byte[] generateSignature(final byte[] data, final Key key) {
            return RsaSigner.generate(data, key.getPrivate());
        }

        @Override
        public boolean verify(final byte[] data, final Key key, final byte[] signature) {
            return RsaSigner.verify(data, key.getPublic(), signature);
        }
    },
    AES {
        @Override
        public byte[] generateSignature(final byte[] data, final Key key) {
            return CmacSigner.generate(data, key);
        }

        @Override
        public boolean verify(final byte[] data, final Key key, final byte[] signature) {
            return CmacSigner.verify(data, key, signature);
        }
    },
    SHA {
        @Override
        public byte[] generateSignature(final byte[] data, final Key key) {
            return HmacSigner.generate(data, key);
        }

        @Override
        public boolean verify(final byte[] data, final Key key, final byte[] signature) {
            return HmacSigner.verify(data, key, signature);
        }
    };

    public abstract byte[] generateSignature(final byte[] data, final Key key);
    public abstract boolean verify(final byte[] data, final Key key, final byte[] signature);
}
