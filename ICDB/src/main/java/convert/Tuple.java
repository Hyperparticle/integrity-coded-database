package convert;

import com.google.common.base.Charsets;
import main.args.option.AlgorithmType;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * <p>
 * </p>
 * Created on 6/2/2016
 *
 * @author Dan Kondratyuk
 */
public class Tuple {

    private final String tuple;
    private final String delimiter;

    private final List<byte[]> signatures = new LinkedList<>();

    public Tuple(String tuple, String delimiter) {
        this.tuple = tuple;
        this.delimiter = delimiter;
    }

    public void convert(AlgorithmType algorithmType, byte[] key, boolean perField) {
        if (perField) {
            String[] fields = tuple.split(delimiter);
            for (String field : fields) {
                byte[] data = field.getBytes(Charsets.UTF_8);
                byte[] signature = algorithmType.generateSignature(data, key);
                signatures.add(signature);
            }
        } else {
            byte[] data = tuple.getBytes(Charsets.UTF_8);
            byte[] signature = algorithmType.generateSignature(data, key);
            signatures.add(signature);
        }
    }

}
