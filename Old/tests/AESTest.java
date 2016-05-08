package tests;

import AES.helper.AESCipher;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class AESTest {
	/**
	 * Tests the encryption / decryption methods
	 * @param args
	 * @throws NoSuchAlgorithmException 
	 * @throws NoSuchPaddingException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidKeyException 
	 * @throws Exception
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException {
		AESCipher aes = new AESCipher();
		
		SecretKey secretKey = aes.generateKey();
		
		String encodeKey = aes.keyToString(secretKey);
		System.out.println("Key: " + encodeKey);
		
		String plainText = "AES Symmetric Encryption Decryption";
		System.out.println("Plain Text Before Encryption: " + plainText);
		
		String encryptedText = aes.encrypt(plainText, secretKey);
		System.out.println("Encrypted Text After Encryption: " + encryptedText);

		SecretKey decodeKey = aes.stringToKey(encodeKey);
		String decryptedText = aes.decrypt(encryptedText, decodeKey);
		System.out.println("Decrypted Text After Decryption: " + decryptedText);
		
		System.out.println(plainText.equals(decryptedText) ? "Passed!" : "Failed!");
	}
}
