package cipher;

import com.google.common.hash.Hashing;
import com.sun.istack.internal.NotNull;
import sun.security.provider.SHA;

import java.nio.charset.StandardCharsets;
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

	private final String salt;

    // TODO: Auto-generate salt and store it with the message
	public SHACipher(String salt) {
        this.salt = salt;
	}

	@Override
    @NotNull
	public String encrypt(String message) {
        return Hashing.sha256()
                .hashString(message + salt, StandardCharsets.UTF_8)
                .toString();
	}

	@Override
	public boolean verify(String message, String encoded) {
		return encrypt(message).equals(encoded);
	}
}
