package mac;

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

    private static final Digest SHA1 = new SHA1Digest();
    private static final Digest SHA256 = new SHA256Digest();

    private static final Digest digest = SHA1;
    private static final HMac hmac = new HMac(digest);

    public static byte[] generateSignature(final byte[] data, final byte[] key) {
        hmac.init(new KeyParameter(key));
        hmac.update(data, 0, data.length);
        final byte[] result = new byte[digest.getDigestSize()];
        hmac.doFinal(result, 0);
        return truncate(result);    // Truncate to the nearest 128 bits (SHA1 outputs 160 bits)
    }

    public static boolean verify(byte[] data, byte[] key, byte[] signature) {
        return Arrays.equals(generateSignature(data, key), signature);
    }

    private static byte[] truncate(byte[] array) {
        return Arrays.copyOf(array, array.length - array.length % 16);
    }

    // JAVAX HMAC

//    public static final String SHA1_ALGORITHM    = "HmacSHA1";
//    public static final String SHA256_ALGORITHM  = "HmacSHA256";
//    public static final String DEFAULT_ALGORITHM = SHA1_ALGORITHM;
//
//    public static byte[] generateSignature(final byte[] data, final byte[] key)
//            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
//        return generateSignature(data, key, DEFAULT_ALGORITHM);
//    }
//
//    public static byte[] generateSignature(final byte[] data, final byte[] key, String algorithm)
//            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
//        final SecretKeySpec signingKey = new SecretKeySpec(key, algorithm);
//        final Mac mac = Mac.getInstance(algorithm);
//        mac.init(signingKey);
//        return mac.doFinal(data);
//    }

}
