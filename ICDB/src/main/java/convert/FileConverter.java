package convert;

import cipher.mac.Signature;
import cipher.mac.AlgorithmType;

import java.util.List;

/**
 * <p>
 * A FileConverter takes an input DB data file and generates a converted ICDB
 * data file. This class only supports MySQL for now.
 * </p>
 * Created 5/8/2016
 * 
 * @author Dan Kondratyuk
 */
public class FileConverter {

    /**
     * Given a String, this method generates codes (svc + serial) from it and adds them to
     * the end of the supplied list
     * @param collector the list to collect the codes
     */
    private static void convert(final List<String> collector, byte[] data, byte[] key, AlgorithmType algorithm) {
        // Generate the signature
        final byte[] signature = algorithm.generateSignature(data, key);
        final String signatureString = Signature.toBase64(signature);

        // TODO: add a serial
        final String serial = Signature.toBase64(new byte[] {0x33});

        // Write the line
        collector.add(signatureString);
        collector.add(serial);
    }

}
