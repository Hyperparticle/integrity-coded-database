package cipher.deprecated;

import cipher.deprecated.CodeCipher;
import cipher.deprecated.RSAKeyGenerator;

import java.math.BigInteger;
import java.nio.file.Path;

/**
 * <p>
 * An RSACipher uses RSA as its backing cipher mechanism to encrypt and verify
 * messages.
 * </p>
 * Created 5/8/2016
 * 
 * @author Dan Kondratyuk
 * @see CodeCipher
 */
public class RSACipher {
	private BigInteger privateKey;
	private BigInteger modulus;

	public RSACipher(Path dataPath, String databaseName) {
		RSAKeyGenerator keyGenerator = new RSAKeyGenerator(dataPath, databaseName);
		keyGenerator.getKeys();
		privateKey = RSAKeyGenerator.getPrivateKey();
		modulus = RSAKeyGenerator.getModulus();
	}

	public String encrypt(String message) {
		BigInteger encrypt = null;
		BigInteger Bigmessage = convertToInt(message);
		encrypt = Bigmessage.modPow(privateKey, modulus);
		return encrypt.toString(16);
	}

	public boolean verify(String message, String encoded) {
		if (encrypt(message).equals(encoded))
			return true;
		else
			return false;
	}

	/**
	 * Method to convert an input to integer
	 * 
	 * @param input
	 * @return
	 */
	public static BigInteger convertToInt(String input) {
		StringBuilder sBuilder = new StringBuilder();
		if (!input.equals("")) {
			for (char c : input.toCharArray()) {
				sBuilder.append((int) c);
			}
			return new BigInteger(sBuilder.toString());
		}
		return null;
	}

}
