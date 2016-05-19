package cipher;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * <p>
 * An AESCipher uses AES as its backing cipher mechanism to encrypt and verify
 * messages.
 * </p>
 * Created 5/8/2016
 * 
 * @author Dan Kondratyuk
 * @see CodeCipher
 */
public class AESCipher implements CodeCipher {

	// private static final String CIPHER_INSTANCE = "AES/CBC/PKCS5PADDING";
	private static final String CIPHER_INSTANCE = "AES/ECB/PKCS5PADDING";
	private static final String KEYGEN_INSTANCE = "AES";
	private static final String ENCODING = "UTF-8";
	private static final String DELIMITER = " ";
	private static final int KEY_SIZE = 128;

	private Cipher cipher;
	private KeyGenerator key_generator;
	private SecretKey secretKey;

	private StringBuilder builder;
	private Base64.Encoder encoder = Base64.getEncoder();
	private Base64.Decoder decoder = Base64.getDecoder();

	/**
	 * Set up an AESCipher.
	 */
	public AESCipher() {
		try {
			cipher = Cipher.getInstance(CIPHER_INSTANCE);
			key_generator = KeyGenerator.getInstance(KEYGEN_INSTANCE);
			key_generator.init(KEY_SIZE);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Set up an AESCipher.
	 */
	public AESCipher(Path dataPath, String databaseName) {
		AESKeyGenerator aeskeygenerator = new AESKeyGenerator(dataPath, databaseName);
		secretKey = stringToKey(aeskeygenerator.getKey());
		try {
			cipher = Cipher.getInstance(CIPHER_INSTANCE);
			key_generator = KeyGenerator.getInstance(KEYGEN_INSTANCE);
			key_generator.init(KEY_SIZE);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Uses AES to encrypt the message with the specified key.
	 * 
	 * @param message
	 *            the message to encrypt
	 * 
	 * @return A delimited String containing an input vector (IV) and the
	 *         encrypted ciphertext.
	 */

	@Override
	public String encrypt(String message) {
		try {
			byte[] messageByte = message.getBytes(ENCODING);

			// Encrypt the message, get ciphertext and iv
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] encryptedByte = cipher.doFinal(messageByte);
			// byte[] iv = cipher.getIV();

			// Convert codes to string
			String encryptedText = encoder.encodeToString(encryptedByte);
			// String ivText = encoder.encodeToString(iv);

			// // Return the delimited string codes
			// builder = new
			// StringBuilder(ivText).append(DELIMITER).append(encryptedText);
			// return builder.toString();
			return encryptedText;
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
				| UnsupportedEncodingException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return null;
	}

	@Override
	public boolean verify(String message, String encoded) {
		if (encrypt(message).equals(encoded))
			return true;
		else
			return false;

	}

	/**
	 * Securely generates a SecretKey
	 * 
	 * @return a SecretKey
	 */
	public SecretKey generateKey() {
		return key_generator.generateKey();
	}

	/**
	 * Converts a SecretKey to its Base64 String representation
	 * 
	 * @param secretKey
	 *            the key to convert
	 * @return the String representation
	 */
	public String keyToString(SecretKey secretKey) {
		return encoder.encodeToString(secretKey.getEncoded());
	}

	/**
	 * Converts a Base64 SecretKey String to a SecretKey object
	 * 
	 * @param secretKey
	 *            the String to convert
	 * @return a SecretKey
	 */
	public SecretKey stringToKey(String secretKey) {
		byte[] decodedKey = decoder.decode(secretKey);
		return new SecretKeySpec(decodedKey, 0, decodedKey.length, KEYGEN_INSTANCE);
	}

}