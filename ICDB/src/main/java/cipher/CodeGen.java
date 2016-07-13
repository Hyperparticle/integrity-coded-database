package cipher;

/**
 * <p>
 *     A wrapper class containing a key and an algorithm to generate a signature from a message
 * </p>
 * Created on 6/29/2016
 *
 * @author Dan Kondratyuk
 */
public class CodeGen {
    private final AlgorithmType algorithm;
    private final Key key;

    public CodeGen(AlgorithmType algorithm, Key key) {
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
