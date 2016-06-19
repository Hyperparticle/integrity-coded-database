package mac;


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

    public static byte[] generateSignature(final byte[] data, final byte[] key) {
        cmac.init(new KeyParameter(key));
        cmac.update(data, 0, data.length);
        final byte[] result = new byte[aes.getBlockSize()];
        cmac.doFinal(result, 0);
        return result;
    }

    public static boolean verify(byte[] data, byte[] key, byte[] signature) {
        return Arrays.equals(generateSignature(data, key), signature);
    }

}
