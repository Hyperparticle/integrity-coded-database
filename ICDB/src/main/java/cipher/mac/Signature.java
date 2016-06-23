package cipher.mac;

import com.google.common.io.BaseEncoding;

/**
 * <p>
 * </p>
 * Created on 6/18/2016
 *
 * @author Dan Kondratyuk
 */
public class Signature {

    public static String toBase64(final byte[] signature) {
        return BaseEncoding.base64().encode(signature);
    }

    public static byte[] toBytes(final String signature) {
        return BaseEncoding.base64().decode(signature);
    }

}
