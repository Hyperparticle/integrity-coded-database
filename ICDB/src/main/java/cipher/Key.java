package cipher;

import com.google.common.base.Charsets;

/**
 * <p>
 *     A key for generating signatures
 * </p>
 * Created on 6/29/2016
 *
 * @author Dan Kondratyuk
 */
public class Key {

    public final byte[] key;

    public Key(byte[] key) {
        this.key = key;
    }

    public Key(String key) {
        this.key = key.getBytes(Charsets.UTF_8);
    }

}
