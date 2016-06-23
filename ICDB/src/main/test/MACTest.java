import com.google.common.base.Charsets;
import cipher.mac.CMAC;
import cipher.mac.HMAC;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>
 * </p>
 * Created on 6/2/2016
 *
 * @author Dan Kondratyuk
 */
public class MACTest {

    @Test
    public void HMACTest() {
        String data = "data";
        String key  = "key";

        byte[] dataBytes = data.getBytes(Charsets.UTF_8);
        byte[] keyBytes  = key.getBytes(Charsets.UTF_8);

        byte[] signature = HMAC.generateSignature(dataBytes, keyBytes);

        boolean verify = HMAC.verify(dataBytes, keyBytes, signature);

        Assert.assertTrue(verify);
    }

    @Test
    public void CMACTest() {
        String data = "data";

        byte[] dataBytes = data.getBytes(Charsets.UTF_8);
        byte[] keyBytes  = {
                0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7,
                0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7,
                0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7,
                0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7
        };

        byte[] signature = CMAC.generateSignature(dataBytes, keyBytes);

        boolean verify = CMAC.verify(dataBytes, keyBytes, signature);

        Assert.assertTrue(verify);
    }

}
