package main.args.option;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * Converts the supplied String to its enum counterpart
 *
 * Created on 5/21/2016
 * @author Dan Kondratyuk
 */
public class GranularityConverter implements IStringConverter<Granularity> {

    @Override
    public Granularity convert(String value) {
        try {
            return Granularity.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Value " + value + " is not valid. Available values are: tuple, field");
        }
    }

}
