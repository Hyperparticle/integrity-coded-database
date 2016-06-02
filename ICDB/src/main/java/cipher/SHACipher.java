package cipher;

import com.google.common.hash.Hashing;
import org.apache.commons.lang3.ArrayUtils;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * <p>
 * An SHACipher uses SHA-256 as its backing cipher mechanism to encrypt and
 * verify messages.
 * </p>
 * Created 5/8/2016
 * 
 * @author Dan Kondratyuk
 * @see CodeCipher
 */
public class SHACipher {

    private static final int HASH_BYTES = 32; // 32 B = 256 b
    private static final int SALT_BYTES = 32; // 32 B = 256 b

    private static final SecureRandom random = new SecureRandom();

	public static byte[] encrypt(final byte[] message) {
        // Encrypt the message along with the salt
        return encrypt(message, generateSalt());
	}

    private static byte[] encrypt(final byte[] message, final byte[] salt) {
        // Join message with salt
        final byte[] saltedMessage = ArrayUtils.addAll(message, salt);

        // Get the hashed message
        final byte[] hashedMessage = Hashing.sha256()
                .hashBytes(saltedMessage)
                .asBytes();

        // Join the hashed message with the salt so that it can be verified
        return ArrayUtils.addAll(hashedMessage, salt);
    }


	public static boolean verify(final byte[] message, final byte[] encodedWithSalt) {
        // Extract the salt
        final byte[] salt = Arrays.copyOfRange(encodedWithSalt, HASH_BYTES, HASH_BYTES + SALT_BYTES);

        // Regenerate the hash and compare
		return Arrays.equals(encrypt(message, salt), encodedWithSalt);
    }

    private static byte[] generateSalt() {
        final byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return salt;
    }
}
