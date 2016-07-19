package cipher.signature;

import cipher.Key;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.util.Arrays;

//import javax.crypto.spec.SecretKeySpec;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//import java.security.SignatureException;
//import java.util.Arrays;

/**
 * <p>
 *      A Hashing Message Authentication Code (HMAC) is a fast way to generate a
 *      code designed to detect code tampering
 * </p>
 * Created on 6/2/2016
 *
 * @author Dan Kondratyuk
 */
public class HMAC {

    private static final Digest digest = new SHA1Digest();
    private static final HMac hmac = new HMac(digest);

    public static byte[] generate(final byte[] data, final Key key) {
        hmac.init(new KeyParameter(key.getMacKey()));
        hmac.update(data, 0, data.length);
        final byte[] result = new byte[digest.getDigestSize()];
        hmac.doFinal(result, 0);
        return truncate(result);    // Truncate to the nearest 128 bits (SHA1 outputs 160 bits)
    }

    public static boolean verify(final byte[] data, final Key key, final byte[] signature) {
        final byte[] generated = generate(data, key);
        return Arrays.equals(generated, signature);
    }

    private static byte[] truncate(byte[] array) {
        return Arrays.copyOf(array, array.length - array.length % 16);
    }

}
