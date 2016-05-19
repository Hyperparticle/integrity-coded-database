package main.args.validate;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * <p>
 * </p>
 * Created on 5/17/2016
 *
 * @author Dan Kondratyuk
 */
public class CipherTypeValidator implements IParameterValidator {

    private static String[] VALUES = { "RSA", "AES", "SHA" };

    public void validate(String name, String value) throws ParameterException {
        // TODO
    }

}
