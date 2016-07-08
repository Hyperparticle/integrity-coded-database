package cipher;

import java.security.SecureRandom;

/**
 * <p>
 *      A Secure Random Number Generator (RNG)
 * </p>
 * Created on 7/8/2016
 *
 * @author Dan Kondratyuk
 */
public class RNG {

    private static final SecureRandom random = new SecureRandom();

    public static int randomInt() {
        return random.nextInt(Integer.MAX_VALUE);
    }

}
