package cipher.signature;


import cipher.Key;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.util.Arrays;

/**
 * <p>
 * </p>
 * Created on 6/2/2016
 *
 * @author Dan Kondratyuk
 */
public class CMAC {

    private static BlockCipher aes = new AESFastEngine();
    private static Mac cmac = new CMac(aes);

    public static byte[] generate(final byte[] data, final Key key) {
        cmac.init(new KeyParameter(key.getMacKey()));
        cmac.update(data, 0, data.length);
        final byte[] result = new byte[aes.getBlockSize()];
        cmac.doFinal(result, 0);
        return result;
    }

    public static boolean verify(byte[] data, Key key, byte[] signature) {
        return Arrays.equals(generate(data, key), signature);
    }

}
