package cipher.signature;

import com.google.common.io.BaseEncoding;

/**
 * <p>
 * </p>
 * Created on 6/18/2016
 *
 * @author Dan Kondratyuk
 */
public class Sign {

    public static String toBase64(final byte[] signature) {
        return BaseEncoding.base64().encode(signature);
    }

    public static byte[] fromBase64(final String signature) {
        return BaseEncoding.base64().decode(signature);
    }

}