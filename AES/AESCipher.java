package AES;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * An AES-based symmetric key encryption/decryption class. Can encrypt/decrypt 
 * a String message, and contains helper methods to generate a key, along with 
 * converting the key between its String and object form.
 * 
 * @author Daniel Kondratyuk
 *
 */
public class AESCipher {
	
	private static final String CIPHER_INSTANCE = "AES/CBC/PKCS5PADDING";
	private static final String KEYGEN_INSTANCE = "AES";
	private static final String DELIMITER = " ";
	private static final int KEY_SIZE = 128;
	
	private Cipher cipher;
	private KeyGenerator keyGenerator;
	
	private StringBuilder builder;
	private Base64.Encoder encoder = Base64.getEncoder();
	private Base64.Decoder decoder = Base64.getDecoder();
	
	/**
	 * Set up an AESCipher.
	 */
	public AESCipher() {
		try {
			cipher = Cipher.getInstance(CIPHER_INSTANCE);
			keyGenerator = KeyGenerator.getInstance(KEYGEN_INSTANCE);
			keyGenerator.init(KEY_SIZE);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Uses AES to encrypt the message with the specified key.
	 * 
	 * @param message the message to encrypt
	 * @param secretKey the key to encrypt with
	 * @return A delimited String containing an input vector (IV) 
	 * and the encrypted ciphertext.
	 */
	public String encrypt(String message, SecretKey secretKey) {
		try {
			byte[] plainTextByte = message.getBytes();
			
			// Encrypt the message, get ciphertext and iv
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] encryptedByte = cipher.doFinal(plainTextByte);
			byte[] iv = cipher.getIV();
			
			// Convert codes to string
			String encryptedText = encoder.encodeToString(encryptedByte);
			String ivText = encoder.encodeToString(iv);
			
			// Return the delimited string codes
			builder = new StringBuilder(ivText).append(DELIMITER).append(encryptedText);
			return builder.toString();
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		return null;
	}
	
	/**
	 * Uses AES to decrypt the message with the specified SecretKey.
	 * 
	 * @param encryptedText the ciphertext to decrypt
	 * @param secretKey the key to decrypt with
	 * @return the decrypted message
	 */
	public String decrypt(String encryptedText, SecretKey secretKey) {
		try {
			String[] codes = encryptedText.split(DELIMITER);
			
			// Extract the iv and ciphertext
			byte[] iv = decoder.decode(codes[0]);
			byte[] encryptedTextByte = decoder.decode(codes[1]);
			
			// Decrypt the message
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
			byte[] decryptedByte = cipher.doFinal(encryptedTextByte);
			
			// Return the decrypted message
			return new String(decryptedByte);
		} catch (InvalidKeyException | IllegalBlockSizeException | 
				BadPaddingException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		return null;
	}
	
	/**
	 * Uses AES to decrypt the message with the specified String key. 
	 * Use this method if you only need to use the key to decrypt once, 
	 * otherwise use keyToString() and call decrypt() with the 
	 * SecretKey object.
	 * 
	 * @param encryptedText
	 * @param secretKey
	 * @return
	 */
	public String decrypt(String encryptedText, String secretKey) {
		return decrypt(encryptedText, stringToKey(secretKey));
	}
	
	/**
	 * Securely generates a SecretKey
	 * @return a SecretKey
	 */
	public SecretKey generateKey() {
		return keyGenerator.generateKey();
	}
	
	/**
	 * Converts a SecretKey to its Base64 
	 * String representation
	 * @param secretKey the key to convert
	 * @return the String representation
	 */
	public String keyToString(SecretKey secretKey) {
		return encoder.encodeToString(secretKey.getEncoded());
	}
	
	/**
	 * Converts a Base64 SecretKey String to 
	 * a SecretKey object
	 * @param secretKey the String to convert
	 * @return a SecretKey
	 */
	public SecretKey stringToKey(String secretKey) {
		byte[] decodedKey = decoder.decode(secretKey);
		return new SecretKeySpec(decodedKey, 0, decodedKey.length, KEYGEN_INSTANCE);
	}
}