package cipher;

import cipher.AlgorithmType;

/**
 * <p>
 *     A wrapper class containing a key and an algorithm to generate a signature from a message
 * </p>
 * Created on 6/29/2016
 *
 * @author Dan Kondratyuk
 */
public class CodeGen {

    // TODO: use Key class
    private final AlgorithmType algorithm;
    private final byte[] key;

    public CodeGen(AlgorithmType algorithm, byte[] key) {
        this.algorithm = algorithm;
        this.key = key;
    }

    public byte[] generateSignature(final byte[] data) {
        return algorithm.generateSignature(data, key);
    }

    public boolean verify(byte[] data, byte[] signature) {
        return algorithm.verify(data, key, signature);
    }

}
