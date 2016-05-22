package main.args.option;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * <p>
 * </p>
 * Created on 5/21/2016
 *
 * @author Dan Kondratyuk
 */
public class CipherTypeConverter implements IStringConverter<CipherType> {

    @Override
    public CipherType convert(String value) {
        try {
            return CipherType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Value " + value + " is not valid. Available values are: RSA, AES, SHA");
        }
    }

}
