package cipher.deprecated;

/**
 * <p>
 *     A CodeCipher can encrypt a given String, or verify that an encrypted String matches its plaintext counterpart.
 * </p>
 * Created 5/8/2016
 * @author Dan Kondratyuk
 */
public interface CodeCipher {

    /**
     * Encodes the given message into ciphertext
     * @param message the message
     * @return A ciphertext String of the message
     */
    Message encrypt(String message);

    /**
     * Encodes the given message and compares it with its encoded counterpart to verify that they match
     * @param message the plaintext message
     * @param encoded the encoded message
     * @return whether the messages match
     */
    boolean verify(String message, String encoded);

}
