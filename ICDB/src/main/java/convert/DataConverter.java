package convert;

import java.util.List;

/**
 * <p>
 *     A DataConverter takes an input tuple and converts it into an ICDB tuple.
 * </p>
 * Created 5/8/2016
 *
 * @author Dan Kondratyuk
 */
public class DataConverter {

    private final String cipherType;
    private final String granularity;

    public DataConverter(String cipherType, String granularity) {
        this.cipherType = cipherType;
        this.granularity = granularity;
        // TODO: convert strings to enums?
    }

    public void parse(List<String> tuples) {
        // TODO: parse each tuple
    }

}
