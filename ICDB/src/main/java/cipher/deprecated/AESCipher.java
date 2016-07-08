package cipher.deprecated;

import com.google.common.base.Charsets;
import org.apache.commons.lang3.ArrayUtils;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
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
public class AESCipher {

    private static final String KEY_FACTORY = "PBKDF2WithHmacSHA1";
    private static final String CIPHER_INSTANCE = "AES/CBC/PKCS5PADDING";
    private static final String KEY_SPEC = "AES";
    private static final int ITERATIONS = 65536;
    private static final int KEY_SIZE = 256;

    private static final int SALT_BYTES = 32; // 32 B = 256 b
    private static final SecureRandom random = new SecureRandom();

    private static byte[] ivBytes;

    public static byte[] encrypt(final byte[] message, final byte[] key) throws GeneralSecurityException {
        // Get formatted key and salt
        final char[] keyChars = new String(key, Charsets.UTF_8).toCharArray();
        final byte[] salt = generateSalt();

        // Get the secret key
        final SecretKeySpec secret = getSecret(keyChars, salt);

        // Get the cipher
        final Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE);
        cipher.init(Cipher.ENCRYPT_MODE, secret);

        // Encrypt the message and get IV
        final byte[] encoded = cipher.doFinal(message);
        final byte[] ivBytes = cipher.getIV();

        // Join IV and salt with encoded message so that it can be verified
        return ArrayUtils.addAll(ArrayUtils.addAll(encoded, ivBytes), salt);
    }

//    public static byte[] decrypt(final byte[] encodedWithSalt, final byte[] key) throws GeneralSecurityException {
//        // Get formatted key, message, and salt
//        final char[] keyChars = new String(key, Charsets.UTF_8).toCharArray();
//        final byte[] salt = Arrays.copyOfRange(encodedWithSalt, HASH_BYTES, HASH_BYTES + SALT_BYTES);
//
//        // Get the secret key
//        final SecretKeySpec secret = getSecret();
//
//        // Decrypt the message
//        Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE);
//        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));
//
//        return cipher.doFinal(encoded);
//    }

    private static byte[] generateSalt() {
        final byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    private static SecretKeySpec getSecret(char[] keyChars, byte[] salt) throws GeneralSecurityException {
        final SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_FACTORY);
        final PBEKeySpec spec = new PBEKeySpec(keyChars, salt, ITERATIONS, KEY_SIZE);
        final SecretKey secretKey = factory.generateSecret(spec);
        return new SecretKeySpec(secretKey.getEncoded(), KEY_SPEC);
    }




//	private static final String KEYGEN_INSTANCE = "AES";
//	private static final String ENCODING = "UTF-8";
//
//
//	private Cipher cipher;
//	private KeyGenerator key_generator;
//	private SecretKey secretKey;
//
//	private StringBuilder builder;
//	private Base64.Encoder encoder = Base64.getEncoder();
//	private Base64.Decoder decoder = Base64.getDecoder();
//
//	/**
//	 * Set up an AESCipher.
//	 */
//	public AESCipher() {
//		try {
//			cipher = Cipher.getInstance(CIPHER_INSTANCE);
//			key_generator = KeyGenerator.getInstance(KEYGEN_INSTANCE);
//			key_generator.init(KEY_SIZE);
//		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
//			e.printStackTrace();
//			System.exit(1);
//		}
//	}
//
//	/**
//	 * Set up an AESCipher.
//	 */
//	public AESCipher(Path dataPath, String databaseName) {
//		AESKeyGenerator aeskeygenerator = new AESKeyGenerator(dataPath, databaseName);
//		secretKey = stringToKey(aeskeygenerator.getKey());
//		try {
//			cipher = Cipher.getInstance(CIPHER_INSTANCE);
//			key_generator = KeyGenerator.getInstance(KEYGEN_INSTANCE);
//			key_generator.init(KEY_SIZE);
//		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
//			e.printStackTrace();
//			System.exit(1);
//		}
//	}
//
//	/**
//	 * Uses AES to encrypt the message with the specified key.
//	 *
//	 * @param message
//	 *            the message to encrypt
//	 *
//	 * @return A delimited String containing an input vector (IV) and the
//	 *         encrypted ciphertext.
//	 */
//
//	public String encrypt(String message) {
//		try {
//			byte[] messageByte = message.getBytes(ENCODING);
//
//			// Encrypt the message, get ciphertext and iv
//			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
//			byte[] encryptedByte = cipher.doFinal(messageByte);
//			// byte[] iv = cipher.getIV();
//
//			// Convert codes to string
//			String encryptedText = encoder.encodeToString(encryptedByte);
//			// String ivText = encoder.encodeToString(iv);
//
//			// // Return the delimited string codes
//			// builder = new
//			// StringBuilder(ivText).append(DELIMITER).append(encryptedText);
//			// return builder.toBase64();
//			return encryptedText;
//		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
//				| UnsupportedEncodingException e) {
//			e.printStackTrace();
//			System.exit(1);
//		}
//
//		return null;
//	}
//
//	public boolean verify(String message, String encoded) {
//		if (encrypt(message).equals(encoded))
//			return true;
//		else
//			return false;
//
//	}
//
//	/**
//	 * Securely generates a SecretKey
//	 *
//	 * @return a SecretKey
//	 */
//	public SecretKey generateKey() {
//		return key_generator.generateKey();
//	}
//
//	/**
//	 * Converts a SecretKey to its Base64 String representation
//	 *
//	 * @param secretKey
//	 *            the key to convert
//	 * @return the String representation
//	 */
//	public String keyToString(SecretKey secretKey) {
//		return encoder.encodeToString(secretKey.getEncoded());
//	}
//
//	/**
//	 * Converts a Base64 SecretKey String to a SecretKey object
//	 *
//	 * @param secretKey
//	 *            the String to convert
//	 * @return a SecretKey
//	 */
//	public SecretKey stringToKey(String secretKey) {
//		byte[] decodedKey = decoder.decode(secretKey);
//		return new SecretKeySpec(decodedKey, 0, decodedKey.length, KEYGEN_INSTANCE);
//	}

}