package mac;

import com.google.common.io.BaseEncoding;

/**
 * <p>
 * </p>
 * Created on 6/18/2016
 *
 * @author Dan Kondratyuk
 */
public class Signature {

    public static String toBase64(byte[] signature) {
        return BaseEncoding.base64().encode(signature);
    }

    public static byte[] toBytes(String signature) {
        return BaseEncoding.base64().decode(signature);
    }

}
