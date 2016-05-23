package cipher;

import sun.security.provider.SHA;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
public class SHACipher implements CodeCipher {
	private byte[] salt = new byte[16];

	public SHACipher(Path dataPath, String databaseName) {
		SHAKeyGenerator shakeygenerator = new SHAKeyGenerator(dataPath, databaseName);
		salt = shakeygenerator.getKey().getBytes();
		System.out.println(salt.toString());
	}

	@Override
	public String encrypt(String message) {
		try {
			MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
			mDigest.update(salt);
			byte[] result = mDigest.digest(message.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < result.length; i++) {
				sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
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
}
