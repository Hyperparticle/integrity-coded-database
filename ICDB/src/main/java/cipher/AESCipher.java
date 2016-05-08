package cipher;

/**
 * <p>
 *      An AESCipher uses AES as its backing cipher mechanism to encrypt and verify messages.
 * </p>
 * Created 5/8/2016
 * @author Dan Kondratyuk
 * @see CodeCipher
 */
public class AESCipher implements CodeCipher {

	@Override
	public String encode(String message) {
		return null;
	}

	@Override
	public boolean verify(String message, String encoded) {
		return false;
	}
}