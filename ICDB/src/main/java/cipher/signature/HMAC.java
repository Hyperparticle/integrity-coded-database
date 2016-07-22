package cipher.signature;

import cipher.Key;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.ShortenedDigest;
import org.bouncycastle.crypto.macs.HMac;

import java.util.Arrays;

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

    private static final int DATA_SIZE = 16; // Size in bytes

    private static final Digest digest = new ShortenedDigest(new SHA1Digest(), DATA_SIZE);
    private static final Mac hmac = new HMac(digest);

    public static byte[] generate(final byte[] data, final Key key) {
//        Mac hmac = getHMAC();

        hmac.init(key.getMacKey());
        hmac.update(data, 0, data.length);
        final byte[] result = new byte[DATA_SIZE];
        hmac.doFinal(result, 0);
        return result;
    }

    public static boolean verify(final byte[] data, final Key key, final byte[] signature) {
        final byte[] generated = generate(data, key);
        return Arrays.equals(generated, signature);
    }

    private static Mac getHMAC() {
        // TODO: use an object pool?
        Digest digest = new ShortenedDigest(new SHA1Digest(), DATA_SIZE);
        return new HMac(digest);
    }

}
