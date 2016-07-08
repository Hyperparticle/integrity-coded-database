package cipher.deprecated;

import cipher.deprecated.CodeCipher;

/**
 * <p>
 * <p>
 * </p>
 * Created on 5/27/2016
 *
 * @author Dan Kondratyuk
 */
public class Message {

    private final byte[] message;
    private byte[] encoded;
    private byte[] salt;

    public Message(byte[] message, CodeCipher codeCipher) {
        this.message = message;
//        codeCipher.encrypt();
    }

    public byte[] getKey() {
        return salt;
    }

    public byte[] getEncoded() {
        return encoded;
    }

}
